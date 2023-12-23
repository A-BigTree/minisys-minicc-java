package cn.seu.cs.minicc.compiler.asm.compont;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Set;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/12/23
 */
@Data
@AllArgsConstructor
public class RegisterDescriptor {
    private boolean usable;
    private Set<String> variables;
}
