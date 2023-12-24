package cn.seu.cs.minicc.compiler.pre;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/12/23
 */
public class PreCompilerParse {
    public static final Pattern INCLUDE_PATTERN = Pattern.compile("^#include\\s+\"(.*?)\"$");

    public static String preCompile(String code, String basePath) {
        List<String> lines = Arrays.stream(code.replaceAll("\r\n", "\n")
                .split("\n"))
                .map(String::trim)
                .toList();

        return String.join("\n", lines);
    }
}
