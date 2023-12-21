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
public class IRFunc {
    private String name;
    private MiniCType retType;
    private String entryLabel;
    private String exitLabel;
    private Boolean hasReturn;
    private List<IRVar> paramList;
    private List<AbstractIRVal> localVars;
    private List<String> childFunctions;
    private List<Integer> scopePath;
}
