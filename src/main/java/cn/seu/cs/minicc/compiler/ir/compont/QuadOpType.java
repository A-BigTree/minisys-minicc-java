package cn.seu.cs.minicc.compiler.ir.compont;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/12/21
 */
@AllArgsConstructor
@Getter
public enum QuadOpType {
    INIT_VAL("=var"),
    INIT_CONST("=const"),
    INIT_STR("=string"),
    INIT_ARRAY("=[]"),
    INIT_ADDR("=$"),

    SET_LABEL("set_label"),
    J_FALSE("j_false"),
    JUMP("j"),

    RETURN_VOID("return_void"),
    RETURN_EXPR("return_expr"),

    READ_ARRAY("[]"),

    CALL_FUNC("call"),

    ;
    final String op;

    public static final List<String> OPTIMIZE_OP_LIST = List.of(
            "=var",
            "OR_OP",
            "AND_OP",
            "EQ_OP",
            "NE_OP",
            "GT_OP",
            "LT_OP",
            "GE_OP",
            "LE_OP",
            "PLUS",
            "MINUS",
            "MULTIPLY",
            "SLASH",
            "PERCENT",
            "BITAND_OP",
            "BITOR_OP",
            "LEFT_OP",
            "RIGHT_OP",
            "NOT_OP",
            "MINUS",
            "PLUS",
            "BITINV_OP"
    );

    public boolean equals(String op) {
        return this.op.equals(op);
    }
}
