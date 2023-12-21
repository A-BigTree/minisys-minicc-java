package cn.seu.cs.minicc.compiler.yacc;

import cn.seu.cs.minicc.compiler.exception.YaccException;
import cn.seu.cs.minicc.compiler.lex.Token;
import cn.seu.cs.minicc.compiler.yacc.grammar.LALR;
import cn.seu.cs.minicc.compiler.yacc.grammar.LALRActionType;
import cn.seu.cs.minicc.compiler.yacc.grammar.LALRProducer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cn.seu.cs.minicc.compiler.lex.TokenName.*;
import static cn.seu.cs.minicc.compiler.yacc.grammar.GrammarSymbolType.*;
import static cn.seu.cs.minicc.compiler.yacc.grammar.GrammarSymbolType.NON_TERMINAL;
import static cn.seu.cs.minicc.compiler.yacc.grammar.LALRActionType.*;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/12/20
 */
public class YaccParse {
    public static final int WHITESPACE_SYMBOL_ID = -10;

    public static final Pattern stringPattern = Pattern.compile("'([^']+)'");
    public static final Pattern variablePattern = Pattern.compile("\\$\\d+");

    /**
     * 分析表单元格
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TableCell {
        private String action;
        Integer target;
    }

    /**
     * 符号栈元素
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SymbolStackElement {
        private String name;
        private String type;
        ASTNode node;
    }

    private final List<SymbolStackElement> symbolStack = new ArrayList<>();
    private int curRhsLen = 0;
    private SymbolStackElement curSymbol = null;
    private final List<Integer> stateStack = new ArrayList<>();
    private int lineNo = 0;
    private int currentTokenIndex = 0;

    /**
     * 语法分析
     *
     * @param tokens   词法分析结果
     * @param analyzer LALR分析器
     * @return 语法树根节点
     * @throws YaccException 语法错误
     */
    public ASTNode parseTokensLALR(List<Token> tokens, LALR analyzer) throws YaccException {
        // 预处理
        // 检查未匹配符号
        if (tokens.stream().anyMatch(token -> UN_MATCH.equals(token.getName()))) {
            throw new YaccException("Token中存在未匹配符号");
        }
        // 移除注释
        tokens.forEach(token -> {
            // 保护行号
            if (COMMENT.equals(token.getName()) && token.getLiteral().equals("\n")) {
                token.setName(WHITESPACE.name());
            }
        });
        tokens.removeIf(token -> COMMENT.equals(token.getName()));
        // 移除空白符
        tokens.removeIf(token -> WHITESPACE.equals(token.getName()) && !token.getLiteral().equals("\n"));
        // Token编号
        Map<String, Integer> tokenIds = getTokenIds(analyzer);
        // LALR分析表
        List<List<TableCell>> table = getTable(analyzer);

        stateStack.add(analyzer.getDfa().getStartStateId());
        Integer token = tokenIds.get(getCurrToken(tokens).getName());
        while (token != null && token > 0) {
            Integer nextToken = dealWithSymbol(token, table, tokens, analyzer);
            while (nextToken != null && !nextToken.equals(token)) {
                if (nextToken == -1) {
                    return symbolStack.get(0).getNode();
                }
                dealWithSymbol(nextToken, table, tokens, analyzer);
                nextToken = dealWithSymbol(token, table, tokens, analyzer);
            }
            token = tokenIds.get(getCurrToken(tokens).getName());
        }
        return null;
    }

    private Map<String, Integer> getTokenIds(LALR analyzer) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < analyzer.getSymbols().size(); i++) {
            if (SP_TOKEN.equals(analyzer.getSymbols().get(i).getType()) ||
                    TOKEN.equals(analyzer.getSymbols().get(i).getType())) {
                map.put(analyzer.getSymbols().get(i).getContent(), i);
            }
        }
        map.put(WHITESPACE.getName(), WHITESPACE_SYMBOL_ID);
        return map;
    }

    private List<List<TableCell>> getTable(LALR analyzer) {
        List<List<TableCell>> table = new ArrayList<>();
        for (int state = 0; state < analyzer.getDfa().getStates().size(); state++) {
            int nonCnt = 0, nonNonCnt = 0;
            List<TableCell> row = new ArrayList<>();
            for (int symbol = 0; symbol < analyzer.getSymbols().size(); symbol++) {
                String action = "";
                Integer target = 0;
                if (NON_TERMINAL.equals(analyzer.getSymbols().get(symbol).getType())) {
                    action = LALRActionType.NON_TERMINAL.getType();
                    target = analyzer.getGotoTable().get(state).get(nonCnt++);
                } else {
                    String actionType = analyzer.getActionTable().get(state).get(nonNonCnt).getType();
                    if (SHIFT.equals(actionType)) {
                        action = SHIFT.getType();
                        target = analyzer.getActionTable().get(state).get(nonNonCnt).getData();
                    } else if (REDUCE.equals(actionType)) {
                        action = REDUCE.getType();
                        target = analyzer.getActionTable().get(state).get(nonNonCnt).getData();
                    } else if (ACCEPT.equals(actionType)) {
                        action = ACCEPT.getType();
                    } else {
                        action = "default";
                    }
                    nonNonCnt++;
                }
                row.add(new TableCell(action, target));
            }
            table.add(row);
        }
        return table;
    }

    private SymbolStackElement getNode(int num) throws YaccException {
        if (num <= 0 || num > curRhsLen) {
            throw new YaccException("动作代码中存在错误的属性值引用:%s", num);
        }
        return symbolStack.get(symbolStack.size() + num - curRhsLen - 1);
    }

    private void setStackNode(String name, ASTNode node) {
        curSymbol = new SymbolStackElement(name, NON_TERMINAL.getType(), node);
    }

    private Integer dealWithSymbol(Integer symbol,
                                   List<List<TableCell>> table,
                                   List<Token> tokens,
                                   LALR analyzer) throws YaccException {
        if (symbol == WHITESPACE_SYMBOL_ID) {
            lineNo++;
            return WHITESPACE_SYMBOL_ID;
        }
        String action = table.get(stateStack.get(stateStack.size() - 1)).get(symbol).getAction();
        if (SHIFT.equals(action)) {
            Token preToken = tokens.get(currentTokenIndex - 1);
            symbolStack.add(
                    new SymbolStackElement(preToken.getName(), TOKEN.getType(),
                            new ASTNode(preToken.getName(), TOKEN.getType(),
                                    preToken.getLiteral(), new ArrayList<>()))
            );
            return null;
        } else if (LALRActionType.NON_TERMINAL.equals(action)) {
            stateStack.add(table.get(stateStack.get(stateStack.size() - 1)).get(symbol).getTarget());
            return symbol;
        } else if (REDUCE.equals(action)) {
            LALRProducer producer =
                    analyzer.getProducers().get(table.get(stateStack.get(stateStack.size() - 1)).get(symbol).getTarget());
            curRhsLen = producer.getRhs().size();
            curSymbol = symbolStack.get(symbolStack.size() - curRhsLen);
            String actionCode = producer.getAction();
            Matcher stringMatcher = stringPattern.matcher(actionCode);
            Matcher variableMatcher = variablePattern.matcher(actionCode);
            String name = "";
            if (stringMatcher.find()) {
                name = stringMatcher.group(1);
            }
            name = name.replaceAll("'", "");
            List<Integer> nums = new ArrayList<>();
            while (variableMatcher.find()) {
                nums.add(Integer.parseInt(variableMatcher.group().substring(1)));
            }
            SymbolStackElement node = newNode(name, nums);
            setStackNode(analyzer.getLHS(producer) + "_DOLLAR2", node.getNode());
            while (curRhsLen-- > 0) {
                symbolStack.remove(symbolStack.size() - 1);
                stateStack.remove(stateStack.size() - 1);
            }
            symbolStack.add(curSymbol);
            return producer.getLhs();
        } else if (ACCEPT.equals(action)) {
            return -1;
        } else {
            throw new YaccException("语法分析表中存在未定义行为：在状态%s下收到%s时进行%s，推测行号为%s",
                    stateStack.get(stateStack.size() - 1),
                    analyzer.getSymbols().get(symbol).getContent(),
                    table.get(stateStack.get(stateStack.size()-1)).get(symbol).getAction(),
                    lineNo);
        }
    }

    private SymbolStackElement newNode(String name, List<Integer> nums) throws YaccException {
        ASTNode node = new ASTNode(name, NON_TERMINAL.getType(), name, new ArrayList<>());
        List<ASTNode> nodes = new ArrayList<>();
        if (nums != null && !nums.isEmpty()) {
            for (Integer num : nums) {
                nodes.add(getNode(num).getNode());
            }
        }
        nodes.forEach(node::pushNode);
        return new SymbolStackElement(name, NON_TERMINAL.getType(), node);
    }

    private Token getCurrToken(List<Token> tokens) {
        return tokens.get(currentTokenIndex++);
    }
}
