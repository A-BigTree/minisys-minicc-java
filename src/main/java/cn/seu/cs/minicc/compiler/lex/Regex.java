package cn.seu.cs.minicc.compiler.lex;

import cn.seu.cs.minicc.compiler.common.RangeIndex;
import cn.seu.cs.minicc.compiler.exception.LexException;
import lombok.Data;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static cn.seu.cs.minicc.compiler.constants.CommonConstants.*;
import static cn.seu.cs.minicc.compiler.constants.LexConstants.*;
import static cn.seu.cs.minicc.compiler.utils.LexUtils.*;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/11/12
 */
@Data
public class Regex {
    private String raw;          // 原始表达式
    private String escapeExpanded;   // 转义展开表达式
    private String rangeExpanded;    // range展开表达式
    private List<String> dotAdded;       // 加点后表达式
    private String postFix;      // 后缀形式表达式

    private final static String EXCEPTION_PREFIX = "Regex syntax error";

    public Regex(String raw) throws LexException {
        this.raw = raw;
        initEscape();
        initRange();
        initAddDots();
        transToPostfix();
    }

    /**
     * 转义字符串展开
     */
    private void initEscape() throws LexException {
        List<RangeIndex> quoteRanges = getMatchedRanges(INSIDE_QUOTE_NOT_SLASH, raw);
        List<RangeIndex> rangeRanges = getMatchedRanges(RANGE_NOT_SLASH, raw);
        System.out.println(quoteRanges);
        escapeExpanded = raw;
        for (int i = escapeExpanded.length() - 1; i >= 0; i--) {
            if (inRanges(quoteRanges, i) || inRanges(rangeRanges, i)) continue;
            if (escapeExpanded.charAt(i) == '\\' && escapeExpanded.charAt(i + 1) != '\\') {
                int slashBefore = 0;
                for (int j = i; j >= 0; j--) {
                    if (escapeExpanded.charAt(j) == '\\') slashBefore++;
                    else break;
                }
                // 向前找反斜杠，\\\x可以，但\\\\x不行，即奇数个才是对x的转义
                if (slashBefore % 2 != 0) {
                    char escape = escapeExpanded.charAt(i + 1);
                    if (!SUPPORTED_ESCAPE.contains(String.valueOf(escape))) {
                        throw new LexException(EXCEPTION_PREFIX);
                    }
                    String expanded = switch (escape) {
                        case 'd' -> "[0-9]";
                        case 's' -> "[\" \"\\t\\r\\n]";
                        default -> "";
                    };
                    escapeExpanded = escapeExpanded.substring(0, i) +
                            expanded + escapeExpanded.substring(i + 2);
                }
            }
        }
    }

    /**
     * 展开正则里方框范围
     */
    private void initRange() throws LexException {
        List<RangeIndex> quoteRanges = getMatchedRanges(INSIDE_QUOTE_NOT_SLASH, escapeExpanded);
        List<RangeIndex> rangeRanges = getMatchedRanges(RANGE_NOT_SLASH, escapeExpanded);
        // 判断[]是否覆盖
        boolean[] axis = new boolean[escapeExpanded.length()];
        for (RangeIndex rangeIndex : rangeRanges) {
            for (int i = rangeIndex.getStart(); i <= rangeIndex.getEnd(); i++) {
                if (axis[i]) {
                    throw new LexException(EXCEPTION_PREFIX);
                }
                axis[i] = true;
            }
        }
        List<String> replacement = new ArrayList<>();
        for (RangeIndex range : rangeRanges) {
            boolean conjugate = false;
            int start = range.getStart();
            int end = range.getEnd();
            String content = escapeExpanded.substring(start + 1, end);
            boolean checkStart = inRanges(quoteRanges, start) || !inRanges(rangeRanges, start),
                    checkEnd = inRanges(quoteRanges, end) || !inRanges(rangeRanges, end);
            if (checkStart || checkEnd) {
                replacement.add("[" + content + "]");
                continue;
            }
            // 处理[^]
            if (content.charAt(0) == '^') {
                conjugate = true;
                content = content.substring(1);
            }
            // 处理范围对
            List<Character[]> waitForExpand = new ArrayList<>();
            Set<String> expands = new HashSet<>();
            content = replace(RANGE_PAIR, content, value -> {
                waitForExpand.add(new Character[]{value.charAt(0), value.charAt(2)});
                return "";
            });
            for (Character[] value : waitForExpand) {
                try {
                    expands.addAll(lineSpace(value[0], value[1]));
                } catch (LexException e) {
                    throw new LexException(EXCEPTION_PREFIX);
                }
            }
            // 处理剩余单独字符
            boolean foundEscape = true;
            while (foundEscape) {
                foundEscape = false;
                for (int i = 0; i < content.length() - 1; i++) {
                    if (content.charAt(i) == '\\') {
                        expands.add("\\" + content.charAt(i + 1));
                        content = content.substring(0, i) + content.substring(i + 2);
                        foundEscape = true;
                        break;
                    }
                }
            }
            if (content.contains("\" \"")) {
                content = content.replaceAll("\" \"", "");
                expands.add(" ");
            }
            expands.addAll(Arrays.asList(content.split("")));
            if (conjugate) {
                expands = getConjugateSet(expands);
            }
            replacement.add("(" +
                    expands.stream()
                            .filter(value -> !value.isEmpty())
                            .collect(Collectors.joining("|"))
                    + ")");
        }
        AtomicInteger index = new AtomicInteger();
        rangeExpanded = replace(RANGE_NOT_SLASH, escapeExpanded, value -> replacement.get(index.getAndIncrement()));
    }

    private static Set<String> getConjugateSet(Set<String> expands) {
        Set<String> conjugateSet = new HashSet<>();
        for (char ascii = ASCII_MIN; ascii <= ASCII_MAX; ascii++) {
            if (!expands.contains(String.valueOf(ascii)) && CONJUGATE_ESCAPE1.contains(String.valueOf(ascii))) {
                conjugateSet.add("\\" + ascii);
            } else {
                if (!expands.contains("\\" + ascii) && CONJUGATE_ESCAPE2.contains(String.valueOf(ascii))) {
                    conjugateSet.add("\\" + ascii);
                }
                if (!expands.contains(String.valueOf(ascii))) {
                    conjugateSet.add(String.valueOf(ascii));
                }
            }
        }
        return conjugateSet;
    }

    /**
     * 加点处理
     * 不用点号而是用数组表示连缀关系，彻底避免冲突。我们将这称为隐式加点
     */
    private void initAddDots() {
        List<String> res = new ArrayList<>();
        StringBuilder part = new StringBuilder();
        // 当前是否在括号内
        boolean inQuote = false;
        List<RangeIndex> quoteRanges = getMatchedRanges(INSIDE_QUOTE_NOT_SLASH, rangeExpanded);
        for (int i = 0; i < rangeExpanded.length(); i++) {
            Character cur = rangeExpanded.charAt(i),
                    next = i == rangeExpanded.length() - 1 ? null : rangeExpanded.charAt(i + 1);
            // 考虑引号
            int finalI = i;
            if (quoteRanges.stream().anyMatch(
                    range -> finalI == range.getEnd()) && inQuote) { // 引号结束
                inQuote = false;
                res.add("\"" + part + "\"");
                part = new StringBuilder();
            } else if (inRanges(quoteRanges, i)) { // 在引号中
                if (!inQuote) inQuote = true;
                else part.append(cur);
            } else { // 非引号
                part.append(cur);
                boolean notAddDot = isNotAddDot(i, cur, next);
                if (!notAddDot) {
                    res.add(part.toString());
                    part = new StringBuilder();
                }
            }
        }
        if (!part.isEmpty()) {
            res.add(part.toString());
        }
        dotAdded = res.stream()
                .filter(value -> !value.isEmpty() && !value.equals(" "))
                .toList();
    }

    private boolean isNotAddDot(int i, Character cur, Character next) {
        int slashBefore = 0;
        for (int j = i - 1; j >= 0; j--) {
            if (rangeExpanded.charAt(j) == '\\') slashBefore += 1;
            else break;
        }
        return (cur == '\\' && slashBefore % 2 == 0) || // 为转义字符
                i == rangeExpanded.length() - 1 || // 最后一个字符
                ("|(".contains(cur.toString()) && (i == 0 || slashBefore % 2 == 0)) || // 当前字符为非转义字符|(
                (next != null && "|)*+?]".contains(next.toString())); // 下一个字符为操作符或右括号
    }

    /**
     * 中缀正则表达式转换为后缀正则表达式
     */
    private void transToPostfix() {
        List<String> res = new ArrayList<>(),
                parts = new ArrayList<>();
        Stack<String> stack = new Stack<>();
        for (String raw : dotAdded) {
            if (raw.matches("\".+\"")) {  // 引号内容
                for (int j = 1; j < raw.length() - 1; j++) {
                    if ("?*+.()|[]\\".contains(String.valueOf(raw.charAt(j)))) {
                        parts.add("\\");
                        parts.add("" + raw.charAt(j));
                        parts.add("[dot]");
                    } else if (raw.charAt(j) == ' ') {
                        parts.add("[ws]");
                        parts.add("[dot]");
                    } else {
                        parts.add("" + raw.charAt(j));
                        parts.add("[dot]");
                    }
                }
            } else { // 非引号内容
                parts.addAll(Arrays.asList(raw.split("")));
                parts.add("[dot]");
            }
            parts.remove(parts.size() - 1); // 去掉最后一个[dot]
            List<String> partsNew = parts.stream()
                    .map(value -> value.trim().isEmpty() ? "[ws]" : value)
                    .toList();
            boolean waitingEscape = false;
            for (String part : partsNew) {
                part = part.trim();
                if (waitingEscape) {
                    res.add(String.valueOf(part.charAt(0)));
                    waitingEscape = false;
                    continue;
                }
                if (part.isEmpty()) {
                    continue;
                }
                if (part.charAt(0) == '|') {
                    while (!stack.isEmpty() && ".*".contains(stack.peek())) {
                        res.add(stack.pop());
                    }
                    stack.push("|");
                } else if (part.equals("[dot]")) {
                    while (!stack.isEmpty() && "[dot]".equals(stack.peek())) {
                        res.add(stack.pop());
                    }
                    stack.push("[dot]");
                } else if (part.charAt(0) == '*') {
                    res.add("*");
                } else if (part.charAt(0) == '+') {
                    res.add("+");
                } else if (part.charAt(0) == '?') {
                    res.add("?");
                } else if (part.charAt(0) == '(') {
                    stack.push("(");
                } else if (part.charAt(0) == ')') {
                    while (!stack.isEmpty() && !stack.peek().equals("(")) {
                        res.add(stack.pop());
                    }
                    stack.pop();
                } else if (part.charAt(0) == '\\') {
                    res.add("\\");
                    waitingEscape = true;
                } else {
                    res.add(part);
                }
            }
            while (!stack.isEmpty()) {
                res.add(stack.pop());
            }
            postFix = String.join(" ", res);
        }

    }
}
