package cn.seu.cs.minicc.compiler.asm;

import cn.seu.cs.minicc.compiler.asm.compont.AddressDescriptor;
import cn.seu.cs.minicc.compiler.asm.compont.RegisterDescriptor;
import cn.seu.cs.minicc.compiler.asm.compont.StackFrameInfo;
import cn.seu.cs.minicc.compiler.exception.ASMException;
import cn.seu.cs.minicc.compiler.ir.IRParse;
import cn.seu.cs.minicc.compiler.ir.compont.*;
import lombok.Data;

import java.util.*;
import java.util.stream.Collectors;

import static cn.seu.cs.minicc.compiler.constants.Constants.INIT_IR_OPS;
import static cn.seu.cs.minicc.compiler.constants.Constants.USEFUL_REGS;
import static cn.seu.cs.minicc.compiler.ir.compont.QuadOpType.CALL_FUNC;
import static cn.seu.cs.minicc.compiler.ir.compont.QuadOpType.SET_LABEL;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/12/23
 */
@Data
public class ASMParse {
    public static final String VAR_ASM_FORMAT = "%s: %s %s";
    public static final String CODE_ASM_FORMAT = "%s %s, %s";
    public static final String CODE3_ASM_FORMAT = "%s %s, %s, %s";
    public static final String LABEL_ASM_FORMAT = "%s %s";

    private IRParse ir;
    private List<String> asm;
    private List<String> GPRs;
    private Map<String, RegisterDescriptor> registerDescriptors;
    private Map<String, AddressDescriptor> addressDescriptors;
    private Map<String, StackFrameInfo> stackFrameInfos;

    private IRFunc currFunc;
    private StackFrameInfo currFrameInfo;


    public ASMParse(IRParse ir) {
        this.ir = ir;
        this.asm = new ArrayList<>();
        this.GPRs = new ArrayList<>(USEFUL_REGS);
        this.registerDescriptors = new LinkedHashMap<>();
        this.addressDescriptors = new LinkedHashMap<>();
        this.stackFrameInfos = new LinkedHashMap<>();

        currFunc = null;
        currFrameInfo = null;

        getFrameInfoFromFunPool();
        // 初始化寄存器
        for (String reg : GPRs) {
            registerDescriptors.put(reg, new RegisterDescriptor(true, new HashSet<>()));
        }
        asm.add(".data");
        initializeGlobalVars();
        asm.add(".text");
        generateASM();
        peepholeOptimize();
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
                asm.add(VAR_ASM_FORMAT.formatted(globalVar.getName(), globalVar.getType().getMiniCType(), "0x0"));
            } else {
                IRArray array = (IRArray) globalVar;
                String[] values = new String[array.getLen()];
                Arrays.fill(values, "0x0");
                asm.add(VAR_ASM_FORMAT.formatted(array.getName(), array.getType().getMiniCType(), String.join(", ", values)));
            }
        }
    }

    // 根据中间代码生成MIPS汇编代码
    private void generateASM() {
        for (int blockIndex = 0; blockIndex < ir.getBasicBlocks().size(); blockIndex++) {
            BasicBlock block = ir.getBasicBlocks().get(blockIndex);
            for (int irIndex = 0; irIndex < block.getContent().size(); irIndex++) {
                Quad quad = block.getContent().get(irIndex);
                if (quad == null) break;
                String op = quad.getOp(),
                        arg1 = quad.getArg1(),
                        arg2 = quad.getArg2(),
                        res = quad.getRes();
                // 二元表达式
                boolean binaryOp = (!arg1.isEmpty() && !arg2.isEmpty());
                // 一元表达式
                boolean unaryOp = (arg1.isEmpty() ^ arg2.isEmpty());
                if (CALL_FUNC.equals(op)) {
                    dealWithFunc(op, arg1, arg2, res, blockIndex, irIndex);
                } else if (binaryOp) {
                    dealWithBinaryOp(op, arg1, arg2, res, blockIndex, irIndex);
                } else if (unaryOp) {
                    dealWithUnaryOp(op, arg1, arg2, res, blockIndex, irIndex);
                } else {
                    dealWithOtherOp(op, res);
                }
                if (!op.equals("set_label") && !op.equals("j") && !op.equals("j_false") && irIndex == block.getContent().size() - 1) {
                    deallocateBlockMemory();
                }
            }
        }
    }

    /**
     * 处理函数调用
     */
    private void dealWithFunc(String op, String arg1, String arg2, String res, int blockIndex, int irIndex) {
        IRFunc func = ir.getFuncPool().stream()
                .filter(inner -> inner.getName().equals(arg1))
                .findFirst()
                .orElse(null);
        if (func == null) {
            throw new ASMException("Function %s not found", res);
        }
        if (func.getName().equals("main")) {
            throw new ASMException("Cannot call main function");
        }
        String[] args = arg2.split("&");
        if (!arg2.trim().isEmpty()) {
            for (int i = 0; i < func.getParamList().size(); i++) {
                String actualArg = args[i];
                AddressDescriptor ad = addressDescriptors.getOrDefault(actualArg, null);
                if (ad == null || ad.getCurrentAddress() == null || ad.getCurrentAddress().isEmpty()) {
                    throw new ASMException("Actual argument does not have current address");
                }
                String regLoc = "",
                        memLoc = "";
                for (String addr : ad.getCurrentAddress()) {
                    if (addr.startsWith("$")) {
                        regLoc = addr;
                        break;
                    } else {
                        memLoc = addr;
                    }
                }

                if (!regLoc.isEmpty()) {
                    if (i < 4) {
                        asm.add(CODE_ASM_FORMAT.formatted("move", "$a" + i, regLoc));
                    } else {
                        asm.add(CODE_ASM_FORMAT.formatted("sw", regLoc, String.format("%d($sp)", i * 4)));
                    }
                } else {
                    if (i < 4) {
                        asm.add(CODE_ASM_FORMAT.formatted("lw", "$a" + i, memLoc));
                        asm.add("nop");
                        asm.add("nop");
                    } else {
                        asm.add(CODE_ASM_FORMAT.formatted("lw", "$v1", memLoc));
                        asm.add("nop");
                        asm.add("nop");
                        asm.add(CODE_ASM_FORMAT.formatted("sw", "$v1", String.format("%d($sp)", i * 4)));
                    }
                }
            }
        }

        for (Map.Entry<String, AddressDescriptor> entry : addressDescriptors.entrySet()) {
            AddressDescriptor ad = entry.getValue();
            String key = entry.getKey();
            String boundMemAddress = ad.getBoundMemAddress();
            Set<String> currentAddress = ad.getCurrentAddress();
            if (boundMemAddress != null && !currentAddress.contains(boundMemAddress)) {
                if (!currentAddress.isEmpty()) {
                    for (String addr : currentAddress) {
                        if (addr.startsWith("$t")) {
                            storeVar(key, addr);
                            break;
                        }
                    }
                } else {
                    throw new ASMException("Attempted to store a ghost variable: %s", key);
                }
            }
        }

        asm.add(LABEL_ASM_FORMAT.formatted("jal", arg1));
        asm.add("nop");
        // 清除临时寄存器
        for (Map.Entry<String, AddressDescriptor> entry : addressDescriptors.entrySet()) {
            for (String addr : entry.getValue().getCurrentAddress()) {
                if (addr.startsWith("$t")) {
                    RegisterDescriptor registerDescriptor = registerDescriptors.getOrDefault(addr, null);
                    if (registerDescriptor != null) {
                        registerDescriptor.getVariables().remove(entry.getKey());
                    }
                }
            }
            entry.getValue().getCurrentAddress().removeIf(addr -> addr.startsWith("$t"));
        }

        if (!res.isEmpty()) {
            List<String> regs = getRegs(op, arg1, arg2, res, blockIndex, irIndex);
            asm.add(CODE_ASM_FORMAT.formatted("move", regs.get(0), "$v0"));
            manageResDescriptors(regs.get(0), res);
        }
    }

    /**
     * 处理一元表达式
     */
    private void dealWithUnaryOp(String op, String arg1, String arg2, String res, int blockIndex, int irIndex) {
        List<String> regs;
        String regX, regY;
        switch (op) {
            case "out_asm":
                if (arg1.isEmpty()) {
                    throw new ASMException("out_asm string cannot be empty");
                }
                asm.add(arg1.substring(1));
                break;
            case "j_false":
                regs = getRegs(op, arg1, arg2, res, blockIndex, irIndex);
                deallocateBlockMemory();
                asm.add(CODE3_ASM_FORMAT.formatted("beq", regs.get(0), "$zero", res));
                asm.add("nop");
                break;
            case "=const":
                regs = getRegs(op, arg1, arg2, res, blockIndex, irIndex);
                long immediateNum;
                if (arg1.startsWith("0x")) {
                    immediateNum = Long.parseLong(arg1.substring(2), 16);
                } else {
                    immediateNum = Long.parseLong(arg1);
                }
                if (immediateNum <= 32767 && immediateNum >= -32768) {
                    asm.add(CODE3_ASM_FORMAT.formatted("addiu", regs.get(0), "$zero", arg1));
                } else {
                    asm.add(CODE_ASM_FORMAT.formatted("lui", regs.get(0), String.valueOf(immediateNum >> 16)));
                    asm.add(CODE3_ASM_FORMAT.formatted("ori", regs.get(0), regs.get(0), String.valueOf(immediateNum & 0x0000ffff)));
                }
                manageResDescriptors(regs.get(0), res);
                break;
            case "=var":
                regs = getRegs(op, arg1, arg2, res, blockIndex, irIndex);
                registerDescriptors.get(regs.get(0)).getVariables().add(res);
                if (addressDescriptors.containsKey(res)) {
                    addressDescriptors.get(res).getCurrentAddress().clear();
                    addressDescriptors.get(res).getCurrentAddress().add(regs.get(0));
                } else {
                    addressDescriptors.put(res, new AddressDescriptor(null, new HashSet<>(Set.of(regs.get(0)))));
                }
                break;
            case "return_expr":
                AddressDescriptor ad = addressDescriptors.getOrDefault(arg1, null);
                if (ad == null || ad.getCurrentAddress() == null || ad.getCurrentAddress().isEmpty()) {
                    throw new ASMException("Return value does not have current address");
                }
                String regLoc = "",
                        memLoc = "";
                for (String addr : ad.getCurrentAddress()) {
                    if (addr.startsWith("$")) {
                        regLoc = addr;
                        break;
                    } else {
                        memLoc = addr;
                    }
                }
                if (!regLoc.isEmpty()) {
                    asm.add(CODE_ASM_FORMAT.formatted("move", "$v0", regLoc));
                } else {
                    asm.add(CODE_ASM_FORMAT.formatted("lw", "$v0", memLoc));
                    asm.add("nop");
                    asm.add("nop");
                }

                deallocateBlockMemory();

                if (currFrameInfo == null) {
                    throw new ASMException("Current frame info is null");
                }
                for (int i = 0; i < currFrameInfo.getNumGPRs2Save(); i++) {
                    asm.add(CODE_ASM_FORMAT.formatted("lw", "$s" + i,
                            String.format("%d($sp)", (currFrameInfo.getWordSize() - currFrameInfo.getNumGPRs2Save() + i) * 4)));
                    asm.add("nop");
                    asm.add("nop");
                }
                if (!currFrameInfo.isLeaf()) {
                    asm.add(CODE_ASM_FORMAT.formatted("lw", "$ra", String.format("%d($sp)", (currFrameInfo.getWordSize() - 1) * 4)));
                    asm.add("nop");
                    asm.add("nop");
                }
                asm.add(CODE3_ASM_FORMAT.formatted("addiu", "$sp", "$sp", String.valueOf(currFrameInfo.getWordSize() * 4)));
                asm.add("jr $ra");
                asm.add("nop");
                break;
            case "NOT_OP":
            case "MINUS":
            case "PLUS":
            case "BITINV_OP":
                regs = getRegs(op, arg1, arg2, res, blockIndex, irIndex);
                regY = regs.get(0);
                regX = regs.get(1);
                if (!registerDescriptors.containsKey(regY) || !registerDescriptors.get(regY).getVariables().contains(arg1)) {
                    loadVar(arg1, regY);
                }
                switch (op) {
                    case "NOT_OP":
                        asm.add(CODE3_ASM_FORMAT.formatted("xor", regX, "$zero", regY));
                        break;
                    case "MINUS":
                        asm.add(CODE3_ASM_FORMAT.formatted("sub", regX, "$zero", regY));
                        break;
                    case "PLUS":
                        asm.add(CODE_ASM_FORMAT.formatted("move", regX, regY));
                        break;
                    case "BITINV_OP":
                        asm.add(CODE3_ASM_FORMAT.formatted("nor", regX, regY, regY));
                        break;
                    default:
                        break;
                }
                manageResDescriptors(regX, res);
                break;
            case "DOLLAR":
                regs = getRegs(op, arg1, arg2, res, blockIndex, irIndex);
                regY = regs.get(0);
                regX = regs.get(1);
                asm.add(CODE_ASM_FORMAT.formatted("lw", regX, String.format("0(%s)", regY)));
                asm.add("nop");
                asm.add("nop");
                manageResDescriptors(regX, res);
                break;
            default:
                break;
        }
    }

    /**
     * 处理二元表达式
     */
    private void dealWithBinaryOp(String op, String arg1, String arg2, String res, int blockIndex, int irIndex) {
        List<String> regs;
        String regX, regY, regZ;
        String baseAddr;
        switch (op) {
            case "=[]":
                regs = getRegs(op, arg1, arg2, res, blockIndex, irIndex);
                regY = regs.get(0);
                regZ = regs.get(1);
                asm.add(CODE_ASM_FORMAT.formatted("move", "$v1", regY));
                asm.add(CODE3_ASM_FORMAT.formatted("sll", "$v1", "$v1", "2"));
                baseAddr = addressDescriptors.getOrDefault(res, new AddressDescriptor()).getBoundMemAddress();
                if (baseAddr == null) {
                    throw new ASMException("Variable %s not found its bound address", res);
                }
                asm.add(CODE_ASM_FORMAT.formatted("sw", regZ, String.format("%s($v1)", baseAddr)));
                break;
            case "[]":
                regs = getRegs(op, arg1, arg2, res, blockIndex, irIndex);
                regZ = regs.get(0);
                regX = regs.get(1);
                asm.add(CODE_ASM_FORMAT.formatted("move", "$v1", regZ));
                asm.add(CODE3_ASM_FORMAT.formatted("sll", "$v1", "$v1", "2"));
                baseAddr = addressDescriptors.getOrDefault(arg1, new AddressDescriptor()).getBoundMemAddress();
                if (baseAddr == null) {
                    throw new ASMException("Variable %s not found its bound address", arg1);
                }
                asm.add(CODE_ASM_FORMAT.formatted("lw", regX, String.format("%s($v1)", baseAddr)));
                asm.add("nop");
                asm.add("nop");
                manageResDescriptors(regX, res);
                break;
            case "=$":
                regs = getRegs(op, arg1, arg2, res, blockIndex, irIndex);
                regY = regs.get(0);
                regZ = regs.get(1);
                asm.add(CODE_ASM_FORMAT.formatted("sw", regZ, String.format("0(%s)", regY)));
                break;
            // X = Y op Z
            case "OR_OP":
            case "AND_OP":
            case "EQ_OP":
            case "NE_OP":
            case "LT_OP":
            case "GT_OP":
            case "LE_OP":
            case "GE_OP":
            case "PLUS":
            case "MINUS":
            case "MULTIPLY":
            case "SLASH":
            case "PERCENT":
            case "BITOR_OP":
            case "BITXOR_OP":
            case "BITAND_OP":
            case "LEFT_OP":
            case "RIGHT_OP":
                regs = getRegs(op, arg1, arg2, res, blockIndex, irIndex);
                regX = regs.get(2);
                regY = regs.get(0);
                regZ = regs.get(1);
                switch (op) {
                    case "BITOR_OP":
                    case "OR_OP":
                        asm.add(CODE3_ASM_FORMAT.formatted("or", regX, regY, regZ));
                        break;
                    case "BITAND_OP":
                    case "AND_OP":
                        asm.add(CODE3_ASM_FORMAT.formatted("and", regX, regY, regZ));
                        break;
                    case "EQ_OP":
                        asm.add(CODE3_ASM_FORMAT.formatted("sub", regX, regY, regZ));
                        asm.add(CODE3_ASM_FORMAT.formatted("sltu", regX, "$zero", regX));
                        asm.add(CODE3_ASM_FORMAT.formatted("xori", regX, regX, 1));
                        break;
                    case "NE_OP", "MINUS":
                        asm.add(CODE3_ASM_FORMAT.formatted("sub", regX, regY, regZ));
                        break;
                    case "LT_OP":
                        asm.add(CODE3_ASM_FORMAT.formatted("slt", regX, regY, regZ));
                        break;
                    case "GT_OP":
                        asm.add(CODE3_ASM_FORMAT.formatted("slt", regX, regZ, regY));
                        break;
                    case "LE_OP":
                        asm.add(CODE3_ASM_FORMAT.formatted("slt", regX, regZ, regY));
                        asm.add(CODE3_ASM_FORMAT.formatted("xori", regX, regX, 1));
                        break;
                    case "GE_OP":
                        asm.add(CODE3_ASM_FORMAT.formatted("slt", regX, regY, regZ));
                        asm.add(CODE3_ASM_FORMAT.formatted("xori", regX, regX, 1));
                        break;
                    case "PLUS":
                        asm.add(CODE3_ASM_FORMAT.formatted("add", regX, regY, regZ));
                        break;
                    case "MULTIPLY":
                        asm.add(CODE_ASM_FORMAT.formatted("mult", regY, regZ));
                        asm.add(LABEL_ASM_FORMAT.formatted("mflo", regX));
                        break;
                    case "SLASH":
                        asm.add(CODE_ASM_FORMAT.formatted("div", regY, regZ));
                        asm.add(LABEL_ASM_FORMAT.formatted("mflo", regX));
                        break;
                    case "PERCENT":
                        asm.add(CODE_ASM_FORMAT.formatted("div", regY, regZ));
                        asm.add(LABEL_ASM_FORMAT.formatted("mfhi", regX));
                        break;
                    case "BITXOR_OP":
                        asm.add(CODE3_ASM_FORMAT.formatted("xor", regX, regY, regZ));
                        break;
                    case "LEFT_OP":
                        asm.add(CODE3_ASM_FORMAT.formatted("sllv", regX, regY, regZ));
                        break;
                    case "RIGHT_OP":
                        asm.add(CODE3_ASM_FORMAT.formatted("srlv", regX, regY, regZ));
                        break;
                }
                manageResDescriptors(regX, res);
                break;
            default:
                break;
        }
    }

    /**
     * 处理其他操作
     */
    private void dealWithOtherOp(String op, String res) {
        switch (op) {
            case "set_label":
                String[] labels = res.split("_");
                String labelType = labels[labels.length - 1];
                if (labelType.equals("entry")) {
                    currFunc = ir.getFuncPool().stream()
                            .filter(inner -> inner.getEntryLabel().equals(res))
                            .findFirst()
                            .orElse(null);
                    if (currFunc == null) {
                        throw new ASMException("Function %s not found", res);
                    }
                    currFrameInfo = stackFrameInfos.getOrDefault(currFunc.getName(), null);
                    if (currFrameInfo == null) {
                        throw new ASMException("Function %s not found its stack frame info", res);
                    }
                    asm.add(
                            currFunc.getName() +
                                    ":" +
                                    "\t\t # vars = " + currFrameInfo.getLocalData() +
                                    ", regs to save($s#) = " + currFrameInfo.getNumGPRs2Save() +
                                    ", outgoing args = " + currFrameInfo.getOutgoingSlots() +
                                    ", " + (currFrameInfo.getNumReturnAdd() > 0 ? "" : "do not ") +
                                    "need to save return address"
                    );
                    asm.add(CODE3_ASM_FORMAT.formatted("addiu", "$sp", "$sp", String.format("-%d", currFrameInfo.getWordSize() * 4)));
                    if (!currFrameInfo.isLeaf()) {
                        asm.add(CODE_ASM_FORMAT.formatted("sw", "$ra", String.format("%d($sp)", (currFrameInfo.getWordSize() - 1) * 4)));
                    }
                    for (int i = 0; i < currFrameInfo.getNumGPRs2Save(); i++) {
                        asm.add(CODE_ASM_FORMAT.formatted("sw", "$s" + i,
                                String.format("%d($sp)", (currFrameInfo.getWordSize() - currFrameInfo.getNumGPRs2Save() + i) * 4)));
                    }
                    allocateProcMemory(currFunc);
                } else if (labelType.equals("exit")) {
                    deallocateProcMemory();
                } else {
                    asm.add(res + ":");
                }
                break;
            case "j":
                deallocateBlockMemory();
                asm.add(LABEL_ASM_FORMAT.formatted("j", res));
                asm.add("nop");
                break;
            case "return_void":
                deallocateBlockMemory();
                if (currFrameInfo == null) {
                    throw new ASMException("Current frame info is null");
                }
                for (int i = 0; i < currFrameInfo.getNumGPRs2Save(); i++) {
                    asm.add(CODE_ASM_FORMAT.formatted("lw", "$s" + i,
                            String.format("%d($sp)", (currFrameInfo.getWordSize() - currFrameInfo.getNumGPRs2Save() + i) * 4)));
                    asm.add("nop");
                    asm.add("nop");
                }
                if (!currFrameInfo.isLeaf()) {
                    asm.add(CODE_ASM_FORMAT.formatted("lw", "$ra", String.format("%d($sp)", (currFrameInfo.getWordSize() - 1) * 4)));
                    asm.add("nop");
                    asm.add("nop");
                }
                asm.add(CODE3_ASM_FORMAT.formatted("addiu", "$sp", "$sp", String.valueOf(currFrameInfo.getWordSize() * 4)));
                asm.add("jr $ra");
                asm.add("nop");
                break;
            default:
                break;
        }
    }


    /**
     * 回写寄存器内容到内存
     */
    private void storeVar(String varId, String reg) {
        String varLoc = addressDescriptors.getOrDefault(varId, new AddressDescriptor()).getBoundMemAddress();
        if (varLoc == null) {
            throw new ASMException("Variable %s not found its bound address", varId);
        }
        asm.add(CODE_ASM_FORMAT.formatted("sw", reg, varLoc));
        addressDescriptors.get(varId).getCurrentAddress().add(varLoc);
    }

    /**
     * 从内存加载变量到寄存器
     */
    private void loadVar(String varId, String reg) {
        String varLoc = addressDescriptors.getOrDefault(varId, new AddressDescriptor()).getBoundMemAddress();
        if (varLoc == null) {
            throw new ASMException("Variable %s not found its bound address", varId);
        }
        asm.add(CODE_ASM_FORMAT.formatted("lw", reg, varLoc));
        asm.add("nop");
        asm.add("nop");
        registerDescriptors.get(reg).getVariables().clear();
        registerDescriptors.get(reg).getVariables().add(varId);
        addressDescriptors.get(varId).getCurrentAddress().add(reg);
    }

    /**
     * 为一条四元式获取每个变量可用的寄存器
     */
    private List<String> getRegs(String op, String arg1, String arg2, String res, int blockIndex, int irIndex) {
        List<String> regs = new ArrayList<>();
        // 二元表达式
        boolean binaryOp = (!arg1.isEmpty() && !arg2.isEmpty());
        // 一元表达式
        boolean unaryOp = (arg1.isEmpty() ^ arg2.isEmpty());

        if (INIT_IR_OPS.contains(op)) {
            String regX, regY, regZ;
            switch (op) {
                case "=$", "=[]":
                    regY = allocateReg(blockIndex, irIndex, arg1, null, null);
                    if (!registerDescriptors.containsKey(regY) || !registerDescriptors.get(regY).getVariables().contains(arg1)) {
                        loadVar(arg1, regY);
                    }
                    regZ = allocateReg(blockIndex, irIndex, arg2, null, null);
                    if (!registerDescriptors.containsKey(regZ) || !registerDescriptors.get(regZ).getVariables().contains(arg2)) {
                        loadVar(arg2, regZ);
                    }
                    regs = List.of(regY, regZ);
                    break;
                case "=const":
                case "call":
                    regX = allocateReg(blockIndex, irIndex, res, null, null);
                    regs = List.of(regX);
                    break;
                case "j_false":
                    regY = allocateReg(blockIndex, irIndex, arg1, null, null);
                    if (!registerDescriptors.containsKey(regY) || !registerDescriptors.get(regY).getVariables().contains(arg1)) {
                        loadVar(arg1, regY);
                    }
                    regs = List.of(regY);
                    break;
                case "=var":
                    regY = allocateReg(blockIndex, irIndex, arg1, null, null);
                    if (!registerDescriptors.containsKey(regY) || !registerDescriptors.get(regY).getVariables().contains(arg1)) {
                        loadVar(arg1, regY);
                    }
                    regs = List.of(regY, regY);
                    break;
                case "[]":
                    regZ = allocateReg(blockIndex, irIndex, arg2, null, null);
                    if (!registerDescriptors.containsKey(regZ) || !registerDescriptors.get(regZ).getVariables().contains(arg2)) {
                        loadVar(arg2, regZ);
                    }
                    regX = allocateReg(blockIndex, irIndex, res, null, null);
                    regs = List.of(regZ, regX);
                    break;
                default:
                    break;
            }
        } else if (binaryOp) {
            String regY = allocateReg(blockIndex, irIndex, arg1, arg2, res);
            if (!registerDescriptors.containsKey(regY) || !registerDescriptors.get(regY).getVariables().contains(arg1)) {
                loadVar(arg1, regY);
            }
            String regZ = allocateReg(blockIndex, irIndex, arg2, arg1, res);
            if (!registerDescriptors.containsKey(regZ) || !registerDescriptors.get(regZ).getVariables().contains(arg2)) {
                loadVar(arg2, regZ);
            }
            String regX;
            if (res.equals(arg1)) {
                regX = regY.trim();
            } else if (res.equals(arg2)) {
                regX = regZ;
            } else {
                regX = allocateReg(blockIndex, irIndex, res, null, null);
            }
            regs = List.of(regY, regZ, regX);
        } else if (unaryOp) {
            String regY = allocateReg(blockIndex, irIndex, arg1, null, res);
            if (!registerDescriptors.containsKey(regY) || !registerDescriptors.get(regY).getVariables().contains(arg1)) {
                loadVar(arg1, regY);
            }
            String regX = res.equals(arg1) ? regY : allocateReg(blockIndex, irIndex, res, null, null);
            regs = List.of(regY, regX);
        } else {
            throw new ASMException("Unknown op: %s", op);
        }
        return regs;
    }

    /**
     * 为变量分配寄存器
     */
    private String allocateReg(int blockIndex, int irIndex, String thisArg, String otherArg, String res) {
        Set<String> addrDesc = addressDescriptors.getOrDefault(thisArg, new AddressDescriptor()).getCurrentAddress();
        String finalReg = "";
        boolean alreadyInReg = false;
        if (addrDesc != null) {
            for (String addr : addrDesc) {
                if (addr.startsWith("$")) {
                    finalReg = addr;
                    alreadyInReg = true;
                    break;
                }
            }
        }
        if (!alreadyInReg) {
            String freeReg = "";
            for (Map.Entry<String, RegisterDescriptor> entry : registerDescriptors.entrySet()) {
                if (entry.getValue().getVariables().isEmpty() && entry.getValue().isUsable()) {
                    freeReg = entry.getKey();
                    break;
                }
            }
            if (!freeReg.isEmpty()) {
                finalReg = freeReg;
            } else {
                BasicBlock basicBlock = ir.getBasicBlocks().get(blockIndex);
                Map<String, Integer> scores = new HashMap<>();
                for (Map.Entry<String, RegisterDescriptor> entry : registerDescriptors.entrySet()) {
                    String scoreKey = entry.getKey();
                    int score = 0;
                    RegisterDescriptor value = entry.getValue();
                    if (!value.isUsable()) {
                        score = Integer.MAX_VALUE;
                        scores.put(scoreKey, score);
                        continue;
                    }
                    Set<String> currentVars = value.getVariables();
                    for (String var : currentVars) {
                        if (var.equals(res) && !var.equals(otherArg)) {
                            continue;
                        }
                        boolean reused = false;
                        int tmpIndex = irIndex;
                        boolean procedureEnd = false;
                        while (!procedureEnd) {
                            Quad quad = basicBlock.getContent().get(++tmpIndex);
                            if (quad.getArg1().equals(var) ||
                                    quad.getArg2().equals(var) ||
                                    quad.getRes().equals(var)) {
                                reused = true;
                                break;
                            }
                            if (SET_LABEL.equals(quad.getOp()) && quad.getRes().endsWith("_exit")) {
                                procedureEnd = true;
                            }
                        }
                        if (reused) {
                            String boundMem = addressDescriptors.getOrDefault(var, new AddressDescriptor()).getBoundMemAddress();
                            if (boundMem != null) {
                                Set<String> addrs = addressDescriptors.getOrDefault(var, new AddressDescriptor()).getCurrentAddress();
                                if (!(addrs != null && addrs.size() > 1)) {
                                    score += 1;
                                }

                            } else {
                                score = Integer.MAX_VALUE;
                            }
                        }
                    }
                    scores.put(scoreKey, score);
                }
                int minScore = Integer.MAX_VALUE;
                String minKey = "";
                for (Map.Entry<String, Integer> entry : scores.entrySet()) {
                    if (entry.getValue() < minScore) {
                        minScore = entry.getValue();
                        minKey = entry.getKey();
                    }
                }
                if (minScore == Integer.MAX_VALUE) {
                    throw new ASMException("No available register for variable %s", thisArg);
                }
                finalReg = minKey;
                if (minScore > 0) {
                    Set<String> vars = registerDescriptors.getOrDefault(finalReg, new RegisterDescriptor()).getVariables();
                    if (vars == null) {
                        throw new ASMException("Register %s has no variable", minKey);
                    }
                    for (String var : vars) {
                        AddressDescriptor tmpAdr = addressDescriptors.getOrDefault(var, new AddressDescriptor());
                        if (tmpAdr.getCurrentAddress() == null) {
                            throw new ASMException("Variable %s has no current address", var);
                        }
                        if (tmpAdr.getBoundMemAddress() == null) {
                            throw new ASMException("Variable %s has no bound memory address", var);
                        }
                        String boundMem = tmpAdr.getBoundMemAddress();
                        if (!tmpAdr.getCurrentAddress().contains(boundMem)) {
                            storeVar(var, finalReg);
                            registerDescriptors.get(finalReg).getVariables().remove(var);
                            addressDescriptors.get(var).getCurrentAddress().remove(finalReg);
                        }
                    }
                }
            }
        }
        return finalReg;
    }

    /**
     * 管理寄存器和内存地址描述符
     */
    private void manageResDescriptors(String regX, String res) {
        registerDescriptors.get(regX).getVariables().clear();
        registerDescriptors.get(regX).getVariables().add(res);
        if (addressDescriptors.containsKey(res)) {
            for (AddressDescriptor descriptor : addressDescriptors.values()) {
                descriptor.getCurrentAddress().remove(regX);
            }
            addressDescriptors.get(res).getCurrentAddress().clear();
            addressDescriptors.get(res).getCurrentAddress().add(regX);
        } else {
            addressDescriptors.put(res, new AddressDescriptor(null, new HashSet<>(Set.of(regX))));
        }
    }

    /**
     * 清除只属于该基本块的描述符，并在必要时写回寄存器中的变量
     */
    private void deallocateBlockMemory() {
        for (Map.Entry<String, AddressDescriptor> entry : addressDescriptors.entrySet()) {
            String boundMem = entry.getValue().getBoundMemAddress();
            Set<String> currentAddress = entry.getValue().getCurrentAddress();
            if (boundMem != null && !currentAddress.contains(boundMem)) {
                for (String addr : currentAddress) {
                    if (addr.startsWith("$")) {
                        storeVar(entry.getKey(), addr);
                        break;
                    }
                }
            }
        }
        for (Map.Entry<String, RegisterDescriptor> entry : registerDescriptors.entrySet()) {
            entry.getValue().getVariables().clear();
        }
        for (Map.Entry<String, AddressDescriptor> entry : addressDescriptors.entrySet()) {
            entry.getValue().getCurrentAddress().removeIf(addr -> addr.startsWith("$"));
        }
    }

    /**
     * 为函数分配内存
     */
    private void allocateProcMemory(IRFunc func) {
        StackFrameInfo frameInfo = stackFrameInfos.getOrDefault(func.getName(), null);
        if (frameInfo == null) {
            throw new ASMException("Function %s not found its stack frame info", func.getName());
        }
        for (int i = 0; i < func.getParamList().size(); i++) {
            String memLoc = String.format("%d($sp)", (i + frameInfo.getWordSize()) * 4);
            if (i < 4) {
                asm.add(CODE_ASM_FORMAT.formatted("sw", "$a" + i, memLoc));
            }
            addressDescriptors.put(func.getParamList().get(i).getId(),
                    new AddressDescriptor(memLoc, new HashSet<>(Set.of(memLoc))));
        }
        int remainingSlots = frameInfo.getLocalData();
        for (AbstractIRVal localVal : func.getLocalVars()) {
            if (localVal instanceof IRVar) {
                if (!func.getParamList().contains(localVal)) {
                    String memLoc = String.format("%d($sp)",
                            (frameInfo.getWordSize() - frameInfo.getNumGPRs2Save() - remainingSlots - (frameInfo.isLeaf() ? 0 : 1)) * 4);
                    remainingSlots--;
                    addressDescriptors.put(localVal.getId(), new AddressDescriptor(memLoc, new HashSet<>(Set.of(memLoc))));
                }
            } else {
                throw new ASMException("Array local variable not supported");
            }
        }

        int availableRSs = func.getName().equals("main") ? 8 : frameInfo.getNumGPRs2Save();
        for (int i = 0; i < 8; i++) {
            boolean usable = i < availableRSs;
            registerDescriptors.put("$s" + i, new RegisterDescriptor(usable, new HashSet<>()));
        }
        allocateGlobalMemory();
    }

    private void allocateGlobalMemory() {
        for (AbstractIRVal globalVar : ir.getGlobalVars()) {
            if (globalVar instanceof IRVar) {
                addressDescriptors.put(globalVar.getId(),
                        new AddressDescriptor(globalVar.getName(), new HashSet<>(Set.of(globalVar.getName()))));
            } else {
                addressDescriptors.put(globalVar.getId(),
                        new AddressDescriptor(globalVar.getName(), new HashSet<>(Set.of(globalVar.getName()))));
            }
        }
    }

    /**
     * 释放函数内存
     */
    private void deallocateProcMemory() {
        for (Map.Entry<String, AddressDescriptor> entry : addressDescriptors.entrySet()) {
            String boundMem = entry.getValue().getBoundMemAddress();
            Set<String> currentAddress = entry.getValue().getCurrentAddress();
            if (boundMem != null && !currentAddress.contains(boundMem)) {
                if (currentAddress.isEmpty()) {
                    throw new ASMException("Attempted to store a ghost variable: %s", entry.getKey());
                }
                for (String addr : currentAddress) {
                    if (addr.startsWith("$")) {
                        storeVar(entry.getKey(), addr);
                        break;
                    }
                }
            }
        }
        addressDescriptors.clear();
        for (Map.Entry<String, RegisterDescriptor> entry : registerDescriptors.entrySet()) {
            entry.getValue().getVariables().clear();
        }
    }

    /**
     * 窥孔优化
     */
    private void peepholeOptimize() {
        Stack<String> newAsm = new Stack<>();
        newAsm.push(asm.get(0));

        for (int i = 1; i < asm.size(); i++) {
            String curr = asm.get(i);
            String prev = asm.get(i - 1);
            String[] currEle = curr.split(", | ");
            String[] prevEle = prev.split(", | ");
            if (currEle[0].equals("move") && !(List.of("nop", "sw").contains(prevEle[0]))) {
                String currSrc = currEle[2];
                if (prevEle.length < 2) {
                    continue;
                }
                String preDest = prevEle[1];
                if (currSrc.equals(preDest)) {
                    String currDest = currEle[1];
                    String newLine = asm.get(i - 1).replace(preDest, currDest);
                    newAsm.pop();
                    String[] newEle = newLine.split(", | ");
                    if (newEle[0].equals("move") && newEle[1].equals(newEle[2])) {
                        continue;
                    }
                    newAsm.push(newLine);
                } else {
                    newAsm.push(curr);
                }
            } else {
                newAsm.push(curr);
            }
        }

        asm = newAsm.stream().toList();
    }

    public String toString() {
        return asm.stream()
                .map(line -> (!((line.startsWith(".")) || line.contains(":")) ? "\t" : "") + line)
                .collect(Collectors.joining("\n"));
    }
}
