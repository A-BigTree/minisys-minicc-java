package cn.seu.cs.minicc.compiler.yacc.grammar;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/12/20
 */
@AllArgsConstructor
@Getter
public enum GrammarSymbolType {
    ASCII("ascii"),
    TOKEN("token"),
    SP_TOKEN("sptoken"),
    NON_TERMINAL("nonterminal"),
    ;
    final String type;
}
