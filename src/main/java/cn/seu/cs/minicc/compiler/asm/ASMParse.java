package cn.seu.cs.minicc.compiler.asm;

import cn.seu.cs.minicc.compiler.asm.compont.RegisterDescriptor;
import cn.seu.cs.minicc.compiler.asm.compont.StackFrameInfo;
import cn.seu.cs.minicc.compiler.ir.IRParse;
import cn.seu.cs.minicc.compiler.ir.compont.AbstractIRVal;
import cn.seu.cs.minicc.compiler.ir.compont.IRArray;
import cn.seu.cs.minicc.compiler.ir.compont.IRFunc;
import cn.seu.cs.minicc.compiler.ir.compont.IRVar;
import lombok.Data;

import java.util.*;

import static cn.seu.cs.minicc.compiler.constants.Constants.USEFUL_REGS;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/12/23
 */
@Data
public class ASMParse {
    private IRParse ir;
    private List<String> asm;
    private List<String> GPRs;
    private Map<String, RegisterDescriptor> registerDescriptors;
    private Map<String, String> addressDescriptors;
    private Map<String, StackFrameInfo> stackFrameInfos;

    public ASMParse(IRParse ir) {
        this.ir = ir;
        this.asm = new ArrayList<>();
        this.GPRs = new ArrayList<>(USEFUL_REGS);
        this.registerDescriptors = new HashMap<>();
        this.addressDescriptors = new HashMap<>();
        this.stackFrameInfos = new HashMap<>();

        getFrameInfoFromFunPool();
        // 初始化寄存器
        for (String reg : GPRs) {
            registerDescriptors.put(reg, new RegisterDescriptor(true, new HashSet<>()));
        }
        asm.add(".data");
        initializeGlobalVars();
        asm.add(".text");


    }

    // 根据函数池中的信息，获取该程序所需栈帧信息
    public void getFrameInfoFromFunPool() {
        for (IRFunc func : ir.getFuncPool()) {
            boolean isLeaf = func.getChildFunctions().isEmpty();
            int maxArgs = ir.getFuncPool().stream()
                    .filter(inner -> func.getChildFunctions().contains(inner.getName()))
                    .map(inner -> inner.getParamList().size())
                    .max(Integer::compareTo)
                    .orElse(0);
            int outgoingSlots = isLeaf ? 0 : Math.max(maxArgs, 4);
            int localData = 0;
            for (AbstractIRVal localVal : func.getLocalVars()) {
                if (localVal instanceof IRVar) {
                    if (!func.getParamList().contains(localVal)) {
                        localData++;
                    }
                } else {
                    localData += ((IRArray) localVal).getLen();
                }
            }
            int numGPRs2Save = func.getName().equals("main") ?
                    0 : (localData > 10 ? (localData > 18 ? 8 : localData - 8) : 0);
            int wordSize = (isLeaf ? 0 : 1) + localData + numGPRs2Save + outgoingSlots + numGPRs2Save;
            if (wordSize % 2 == 1) {
                wordSize++;
            }
            stackFrameInfos.put(func.getName(),
                    new StackFrameInfo(isLeaf, wordSize, outgoingSlots, localData, numGPRs2Save, isLeaf ? 0 : 1));
        }
    }

    // 初始化全局变量代码
    public void initializeGlobalVars() {
        List<AbstractIRVal> globalVars = ir.getGlobalVars();
        for (AbstractIRVal globalVar : globalVars) {
            if (globalVar instanceof IRVar) {
                asm.add(globalVar.getName() + ": .word 0");
            } else {
                asm.add(globalVar.getName() + ": .space " + ((IRArray) globalVar).getLen() * 4);
            }
        }
    }
}
