package cn.seu.cs.minicc.compiler.lex;

import cn.seu.cs.minicc.compiler.common.RangeIndex;
import cn.seu.cs.minicc.compiler.exception.LexException;
import lombok.Data;

import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static cn.seu.cs.minicc.compiler.constants.CommonConstants.LINE_SEPARATOR;
import static cn.seu.cs.minicc.compiler.constants.LexConstants.ACTION_SPLIT;
import static cn.seu.cs.minicc.compiler.constants.LexConstants.ALIAS_PART;
import static cn.seu.cs.minicc.compiler.utils.LexUtils.firstMatch;
import static cn.seu.cs.minicc.compiler.utils.LexUtils.getMatchedRanges;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/11/12
 */
@Data
public class LexParser {
    // .l文件内容
    private String filePath;
    private String rawContent;
    private List<String> splitContent;
    // .l文件的四部分
    private String copyPart; //直接复制部分
    private String regexAliasPart;  // 正则别名部分
    private String actionPart;  // 正则-动作部分
    private String cCodePart;  // C代码部分
    // 解析结果
    private Map<String, String> regexAliases;
    private Map<Regex, Action> regexActionMap;

    private final static String DEFAULT_LEX_FILE = "MiniC.l";

    public final static String EXCEPTION_PREFIX = "Lex Parser error";

    public LexParser(String path) throws Exception {
        this.filePath = path;
        URL url = this.getClass().getClassLoader().getResource(this.filePath);
        if (url == null) {
            throw new LexException(EXCEPTION_PREFIX);
        }
        URI uri = url.toURI();
        String context = Files.readString(Paths.get(uri));
        this.rawContent = context.replaceAll(LINE_SEPARATOR, "\n");
        this.regexAliases = new HashMap<>();
        this.regexActionMap = new HashMap<>();
        analysisContent();
        analysisAttributes();
    }

    /**
     * 分析文件内容
     */
    private void analysisContent() throws Exception {
        this.splitContent = Stream.of(rawContent.split("\n"))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
        int copyPartStart = -1, copyPartEnd = -1;
        List<Integer> twoPercent = new ArrayList<>(2);
        for (int i = 0; i < splitContent.size(); i++) {
            String content = splitContent.get(i);
            switch (content) {
                case "%{" -> {
                    if (copyPartStart != -1) {
                        throw new LexException(EXCEPTION_PREFIX);
                    }
                    copyPartStart = i;
                }
                case "%}" -> {
                    if (copyPartEnd != -1) {
                        throw new LexException(EXCEPTION_PREFIX);
                    }
                    copyPartEnd = i;
                }
                case "%%" -> {
                    if (twoPercent.size() >= 2) {
                        throw new LexException(EXCEPTION_PREFIX);
                    }
                    twoPercent.add(i);
                }
            }
        }
        this.cCodePart = String.join("\n", splitContent.subList(twoPercent.get(1) + 1, splitContent.size()));
        this.copyPart = String.join("\n", splitContent.subList(copyPartStart + 1, copyPartEnd));
        this.actionPart = String.join("\n", splitContent.subList(twoPercent.get(0) + 1, twoPercent.get(1)));
        this.regexAliasPart = String.join("\n", splitContent.subList(0, copyPartStart));
    }

    /**
     * 解析语义动作
     */
    private void analysisAttributes() throws Exception {
        // 分析正则别名部分
        for (String value : regexAliasPart.split("\n")) {
            String v = value.trim();
            if (!v.isEmpty()) {
                RangeIndex range = firstMatch(ALIAS_PART, v);
                if (range == null) {
                    throw new LexException(EXCEPTION_PREFIX);
                }
                String alias = v.substring(0, range.getEnd()).trim();
                String regex = v.substring(range.getEnd()).trim();
                regexAliases.put(alias, regex);
            }
        }
        // 分析规则动作部分，并作别名展开
        for (String value : actionPart.split("\n")) {
            String v = value.trim();
            if (v.isEmpty()) {
                continue;
            }
            List<RangeIndex> actionRanges = getMatchedRanges(ACTION_SPLIT, v);
            if (actionRanges.size() != 1) {
                throw new LexException(EXCEPTION_PREFIX);
            }
            RangeIndex range = actionRanges.get(0);
            String regex = v.substring(0, range.getStart()).trim();
            String action = v.substring(range.getEnd() + 1).trim();
            // System.out.println(regex + ":" + action);
            //TODO 解析正则表达式

            //TODO 解析动作
        }
    }
}
