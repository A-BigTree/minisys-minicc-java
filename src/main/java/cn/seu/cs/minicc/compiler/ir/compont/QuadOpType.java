package cn.seu.cs.minicc.compiler.ir.compont;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/12/21
 */
@AllArgsConstructor
@Getter
public enum QuadOpType {
    SET_LABEL("set_label"),
    INIT_VAL("=var"),
    READ_ARRAY("[]"),
    CALL_FUNC("call"),
    INIT_CONST("=const"),
    INIT_STR("=string"),
    INIT_ARRAY("=[]"),
    INIT_ADDR("=$"),
    J_FALSE("j_false"),
    JUMP("j"),
    RETURN_VOID("return_void"),
    RETURN_EXPR("return_expr"),

    ;
    final String op;

    public boolean equals(String op) {
        return this.op.equals(op);
    }
}
