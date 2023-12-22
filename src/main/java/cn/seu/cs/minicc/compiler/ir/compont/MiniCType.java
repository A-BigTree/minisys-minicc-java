package cn.seu.cs.minicc.compiler.ir.compont;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/12/21
 */
@AllArgsConstructor
@Getter
public enum MiniCType {
    INT("int"),
    VOID("void"),
    STRING("string"),
    NONE("none")
    ;
    final String type;

    private static final Map<String, MiniCType> typeMap = Map.of(
            "int", INT,
            "void", VOID,
            "string", STRING,
            "none", NONE
    );

    public static MiniCType getByType(String type) {
        return typeMap.getOrDefault(type, NONE);
    }

    public boolean equals(String type) {
        return this.type.equals(type);
    }
}
