package cn.seu.cs.minicc.compiler.pre;

import cn.seu.cs.minicc.compiler.exception.PreException;
import cn.seu.cs.minicc.compiler.util.Utils;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/12/23
 */
public class PreCompilerParse {
    public static final Pattern INCLUDE_PATTERN = Pattern.compile("^#include\\s+\"(.*?)\"$");

    @Data
    @AllArgsConstructor
    public static class Record {
        private int line;
        private List<String> content;
        private String path;
    }

    /**
     * 预编译 处理include
     */
    public static String preCompile(String code, String basePath) {
        List<String> lines = new ArrayList<>(Arrays.stream(code.replaceAll("\r\n", "\n")
                        .split("\n"))
                .map(String::trim)
                .toList());
        List<Record> patches = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            var matcher = INCLUDE_PATTERN.matcher(line);
            if (matcher.find()) {
                String path = matcher.group(1);
                try {
                    String content = Utils.readCode(path, basePath);
                    patches.add(new Record(i, Arrays.asList(content.replaceAll("\r\n", "\n").split("\n")), path));
                } catch (Exception e) {
                    throw new PreException("Cannot find include file '%s'", path);
                }
            }
        }

        int bias = 0;
        for (Record patch : patches) {
            lines.remove(patch.getLine() + bias);
            lines.addAll(patch.getLine() + bias, patch.getContent());
            bias += patch.getContent().size() - 1;
        }

        return String.join("\n", lines);
    }
}
