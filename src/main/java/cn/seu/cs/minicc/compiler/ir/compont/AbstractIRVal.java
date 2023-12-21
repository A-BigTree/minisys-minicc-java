package cn.seu.cs.minicc.compiler.ir.compont;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/12/21
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public abstract class AbstractIRVal {
    private String id;
    private String name;
    private MiniCType type;
    private List<Integer> scope;
}
