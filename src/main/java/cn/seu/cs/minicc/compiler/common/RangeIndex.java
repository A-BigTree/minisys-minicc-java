package cn.seu.cs.minicc.compiler.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/11/12
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RangeIndex {
    private int start;
    private int end;

    public boolean inRange(int target) {
        return target >= start && target <= end;
    }
}
