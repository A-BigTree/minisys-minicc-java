package cn.seu.cs.minicc.compiler.ir.compont;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/12/21
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Quad {
    private String op;
    private String arg1;
    private String arg2;
    private String res;

    public String toString() {
        return "\t(" + op + ", " + arg1 + ", " + arg2 + ", " + res + ")";
    }
}
