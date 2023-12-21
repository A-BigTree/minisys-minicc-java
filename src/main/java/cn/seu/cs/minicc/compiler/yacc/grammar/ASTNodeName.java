package cn.seu.cs.minicc.compiler.yacc.grammar;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/12/21
 */
@AllArgsConstructor
@Getter
public enum ASTNodeName {
    DECL_LIST("decl_list"),
    DECL("decl"),
    VAR_DECL("var_decl"),
    FUN_DECL("fun_decl"),
    PARAM_LIST("param_list"),
    PARAM("param"),
    LOCAL_DECLS("local_decls"),
    LOCAL_DECL("local_decl"),
    STMT_LIST("stmt_list"),
    STMT("stmt"),
    EXPR_STMT("expr_stmt"),
    COMP_STMT("compound_stmt"),
    IF_STMT("if_stmt"),
    WHILE_STMT("while_stmt"),
    RETURN_STMT("return_stmt"),
    CONTINUE_STMT("continue_stmt"),
    BREAK_STMT("break_stmt"),
    EXPR("expr"),
    ARGS("args"),

    ;
    final String name;

    public boolean equals(String name) {
        return this.name.equals(name);
    }
}
