package cn.seu.cs.minicc.compiler.lex;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/12/20
 */
@AllArgsConstructor
@Getter
public enum TokenName {
    UN_MATCH("_UNMATCH"),
    COMMENT("_COMMENT"),
    WHITESPACE("_WHITESPACE"),

    ;
    final String name;

    public boolean equals(String name) {
        return this.name.equals(name);
    }
}
