package cn.seu.cs.minicc.compiler.ir.compont;

import lombok.AllArgsConstructor;
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
public class IRVar extends AbstractIRVal{
    private boolean hasInit;

    public IRVar(String s, String name, MiniCType type, List<Integer> scopePath, boolean b) {
        super(s, name, type, scopePath);
        this.hasInit = b;
    }

    public String toString() {
        return "\t" + id + "(" + name + ", " + type + ", " +
                "var" + ", " +
                scope + ")\n";
    }

    public String toFuncParam() {
        return id + "(" + name + ", " + type +")";
    }
}
