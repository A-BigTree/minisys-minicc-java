package cn.seu.cs.minicc.compiler.yacc.grammar;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/12/20
 */
@AllArgsConstructor
@Getter
public enum LALRActionType {
    SHIFT("shift"),
    REDUCE("reduce"),
    ACCEPT("acc"),
    NONE("none"),
    NON_TERMINAL("nonterminal"),
    ;
    final String type;

    public boolean equals(String type) {
        return this.type.equals(type);
    }
}
