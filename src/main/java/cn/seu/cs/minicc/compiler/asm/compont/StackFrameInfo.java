package cn.seu.cs.minicc.compiler.asm.compont;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/12/23
 */
@Data
@AllArgsConstructor
public class StackFrameInfo {
    private boolean isLeaf;
    private int wordSize;
    private int outgoingSlots;
    private int localData;
    private int numGPRs2Save;
    private int numReturnAdd;
}
