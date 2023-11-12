package cn.seu.cs.minicc.compiler.constants;

import java.util.regex.Pattern;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/11/12
 */
public class LexConstants {
    // lex支持的转义
    public final static String SUPPORTED_ESCAPE = "dstrn\\[]*?+()|";

    public final static String CONJUGATE_ESCAPE1 = "\\[]*?+()|";
    public final static String CONJUGATE_ESCAPE2 = "trn";

    // 非转义引用之间的内容
    public final static Pattern INSIDE_QUOTE_NOT_SLASH =
            Pattern.compile("(?=[^\\\\]|^)(\"[^\"]*[^\\\\]\")");
    // 非转义[]定义的range
    public final static Pattern RANGE_NOT_SLASH =
            Pattern.compile("(?=[^\\\\]|^)\\[(([^\\[\\]]+)[^\\\\])]");

    // 范围角标对
    public final static Pattern RANGE_PAIR =
            Pattern.compile("\\S-\\S");
    public final static Pattern ALIAS_PART = Pattern.compile("\\s+");

    public final static Pattern ACTION_SPLIT =
            Pattern.compile("((action)|(ACTION)):");
}
