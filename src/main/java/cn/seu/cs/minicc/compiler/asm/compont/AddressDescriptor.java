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
public class AddressDescriptor {
    private String boundMemAddress;
    private Set<String> currentAddress;
}
