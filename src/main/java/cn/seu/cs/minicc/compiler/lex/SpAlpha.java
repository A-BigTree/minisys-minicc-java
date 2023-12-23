package cn.seu.cs.minicc.compiler.lex;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

import static cn.seu.cs.minicc.compiler.constants.Constants.EMPTY_STRING;

/**
 * 特殊字符枚举
 *
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/11/12
 */
@Getter
public enum SpAlpha {
    EPSILON(-1, "[ε]"), // ε
    ANY(-2, "[any]"),  // . (any character, except \n, not ε)
    OTHER(-3, "[other]"),  // other character not mentioned

    ;
    private final int index;
    private final String alpha;

    private final static Map<Integer, String> MAP = new HashMap<>();

    static {
        for (SpAlpha entity : SpAlpha.values()) {
            MAP.put(entity.index, entity.alpha);
        }
    }

    SpAlpha(int index, String alpha) {
        this.index = index;
        this.alpha = alpha;
    }

    public static String getSpAlpha(int index) {
        return MAP.getOrDefault(index, EMPTY_STRING);
    }
}
