package cn.seu.cs.minicc.compiler.ir.compont;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/12/21
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class IRArray extends AbstractIRVal{
    private Integer len;

    public IRArray(String s, String name, MiniCType type, List<Integer>scope, Integer len) {
        super(s, name, type, scope);
        this.len = len;
    }

    public String toString() {
        return "\t" + id + "(" + name + ", " + type + ", " +
                "array" + ", " +
                scope + ")\n";
    }
}
