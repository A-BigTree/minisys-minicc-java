package cn.seu.cs.minicc.compiler.ir;

import cn.seu.cs.minicc.compiler.ir.compont.AbstractIRVal;
import cn.seu.cs.minicc.compiler.ir.compont.IRFunc;
import cn.seu.cs.minicc.compiler.ir.compont.Quad;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.*;

import static cn.seu.cs.minicc.compiler.ir.IRParse.VAR_PREFIX;
import static cn.seu.cs.minicc.compiler.ir.compont.QuadOpType.*;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/12/22
 */
@Data
public class IROptimizer {
    private IRParse irParse;
    private List<String> logs;

    @Data
    @AllArgsConstructor
    public static class NodeElement {
        String type;
        String name;
        Integer index;
    }

    public IROptimizer(IRParse irParse) {
        this.irParse = irParse;
        this.logs = new ArrayList<>();


        // 重新进行基本块划分
        this.irParse.toBasicBlocks();
    }

    // 删除变量池中的死变量
    public boolean deadVarEliminate() {
        List<String> usedVars = new ArrayList<>();
        for (Quad quad : irParse.getQuads()) {
            if (CALL_FUNC.equals(quad.getOp())) {
                if (!quad.getArg2().trim().isEmpty()) {
                    usedVars.addAll(new ArrayList<>(List.of(quad.getArg2().split("&"))));
                }
            } else {
                if (quad.getArg2().startsWith(VAR_PREFIX)) {
                    usedVars.add(quad.getArg2());
                }
            }
            if (quad.getArg1().startsWith(VAR_PREFIX)) {
                usedVars.add(quad.getArg1());
            }
            if (quad.getRes().startsWith(VAR_PREFIX)) {
                usedVars.add(quad.getRes());
            }
        }
        Set<String> usedVarSet = new HashSet<>(usedVars);
        List<AbstractIRVal> unUsedVars = irParse.getValPool().stream()
                .filter(v -> !usedVarSet.contains(v.getId()))
                .toList();
        if (!unUsedVars.isEmpty()) {
            logs.add("删除了变量池中的死变量：" + unUsedVars);
        }
        irParse.getValPool().removeIf(v -> usedVarSet.contains(v.getId()));
        return !unUsedVars.isEmpty();
    }

    // 删除从未使用的函数
    private boolean deadFuncEliminate() {
        List<String> usedFunctions = new ArrayList<>();
        usedFunctions.add("main");
        boolean flag = false;
        do {
            flag = false;
            for (String func : usedFunctions) {
                for (String target :
                        irParse.getFuncPool().stream()
                                .filter(f -> f.getName().equals(func))
                                .map(IRFunc::getChildFunctions)
                                .findAny()
                                .orElse(new ArrayList<>())
                ) {
                    if (!usedFunctions.contains(target)) {
                        usedFunctions.add(target);
                        flag = true;
                    }
                }
            }
        } while (flag);

        List<IRFunc> unUsedFunctions = irParse.getFuncPool().stream()
                .filter(f -> !usedFunctions.contains(f.getName()))
                .toList();
        List<int[]> ranges = new ArrayList<>();
        unUsedFunctions.forEach(func -> {
            int start = -1, end = -1;
            for (int i = 0; i < irParse.getQuads().size(); i++) {
                Quad temp = irParse.getQuads().get(i);
                if (SET_LABEL.equals(temp.getOp()) && temp.getRes().equals(func.getEntryLabel())) {
                    start = i;
                }
                if (SET_LABEL.equals(temp.getOp()) && temp.getRes().equals(func.getExitLabel())) {
                    end = i;
                }
            }
            if (start != -1 && end != -1) {
                ranges.add(new int[]{start, end});
            }
            this.logs.add("删除从未被调用的函数 " + func.getName());
        });
        // 函数池中删除
        irParse.getFuncPool().removeIf(func -> !usedFunctions.contains(func.getName()));
        // 删除四元式
        for (int[] range : ranges) {
            for (int i = range[0]; i <= range[1]; i++) {
                irParse.getQuads().set(i, null);
            }
        }
        irParse.getQuads().removeIf(Objects::isNull);
        return !ranges.isEmpty();
    }

    // 删除在赋值后从未使用的变量的赋值语句
    private boolean deadVarUserEliminate() {
        Map<String, List<Integer>> varUpdate = new HashMap<>();
        for (int i = 0; i < irParse.getQuads().size(); i++) {
            Quad quad = irParse.getQuads().get(i);
            if (quad.getRes().startsWith(VAR_PREFIX)) {
                List<Integer> value = varUpdate.getOrDefault(quad.getRes(), new ArrayList<>());
                value.add(i);
                varUpdate.put(quad.getRes(), value);
            }
        }

        List<Integer> quadsToRemove = new ArrayList<>();
        for (Map.Entry<String, List<Integer>> entry : varUpdate.entrySet()) {
            List<Integer> indices = entry.getValue();
            Integer finalIndex = indices.get(indices.size() - 1);
            String var = entry.getKey();
            boolean used = false;
            for (int i = finalIndex; i < irParse.getQuads().size(); i++) {
                Quad quad = irParse.getQuads().get(i);
                if (quad.getArg1().equals(var) ||
                        quad.getArg2().equals(var) ||
                        List.of(quad.getArg2().split("&")).contains(var) ||
                        quad.getRes().equals(var) ||
                        List.of("j", "j_false", "call", "return_void", "return_expr").contains(quad.getOp())) {
                    used = true;
                    break;
                }
            }
            if (!used) {
                this.logs.add("删除从未被使用的变量 " + var + ", 对应四元式索引" + indices);
                quadsToRemove.add(finalIndex);
            }
        }
        for (Integer index : quadsToRemove) {
            irParse.getQuads().set(index, null);
        }
        irParse.getQuads().removeIf(Objects::isNull);
        return !quadsToRemove.isEmpty();
    }

    // 常量传播与常量折叠
    private boolean constPropAndFold() {
        Map<Integer, Quad> eqVars = new HashMap<>();
        for (int i = 0; i < irParse.getQuads().size(); i++) {
            Quad quad = irParse.getQuads().get(i);
            if (INIT_VAL.equals(quad.getOp())) {
                eqVars.put(i, quad);
            }
        }

        boolean unfix = false;

        for (Map.Entry<Integer, Quad> entry : eqVars.entrySet()) {
            List<String> constStk = new ArrayList<>();
            List<NodeElement> nodeStk = new ArrayList<>();
            boolean optimize = true;
            Integer index = entry.getKey();
            Quad value = entry.getValue();
            nodeStk.add(new NodeElement("var", value.getArg1(), index));
            while (!nodeStk.isEmpty()) {
                NodeElement node = nodeStk.get(nodeStk.size() - 1);
                nodeStk.remove(nodeStk.size() - 1);
                if (node.type.equals("var")) {
                    for (int i = node.index - 1; i >= 0; i--) {
                        Quad tmp = irParse.getQuads().get(i);
                        if (SET_LABEL.equals(tmp.getOp())) {
                            optimize = false;
                            break;
                        } else if (tmp.getRes().equals(node.name)) {
                            if (INIT_CONST.equals(tmp.getOp())) {
                                constStk.add(tmp.getArg1());
                            } else if (OPTIMIZE_OP_LIST.contains(tmp.getOp())) {
                                int argNum = tmp.getArg2().trim().isEmpty() ? 1 : 2;
                                nodeStk.add(new NodeElement("op", tmp.getOp(), argNum));
                                nodeStk.add(new NodeElement("var", tmp.getArg1(), i));
                                if (argNum > 1) {
                                    nodeStk.add(new NodeElement("var", tmp.getArg2(), i));
                                }
                            } else {
                                optimize = false;
                            }
                            break;
                        }
                    }
                } else {
                    List<String> args = new ArrayList<>();
                    for (int i = 0; i < node.index; i++) {
                        args.add(constStk.get(constStk.size() - 1));
                        constStk.remove(constStk.size() - 1);
                    }
                    String name = node.name;
                    switch (name) {
                        case "OR_OP" -> constStk.add(
                                (isBoolean(args.get(0)) || isBoolean(args.get(1))) ? "1" : "0"
                        );
                        case "AND_OP" -> constStk.add(
                                (isBoolean(args.get(0)) && isBoolean(args.get(1))) ? "1" : "0"
                        );
                        case "EQ_OP" -> constStk.add(
                                (args.get(0).equals(args.get(1))) ? "1" : "0"
                        );
                        case "NE_OP" -> constStk.add(
                                (!args.get(0).equals(args.get(1))) ? "1" : "0"
                        );
                        case "GT_OP" -> constStk.add(
                                (Integer.parseInt(args.get(0)) > Integer.parseInt(args.get(1))) ? "1" : "0"
                        );
                        case "LT_OP" -> constStk.add(
                                (Integer.parseInt(args.get(0)) < Integer.parseInt(args.get(1))) ? "1" : "0"
                        );
                        case "GE_OP" -> constStk.add(
                                (Integer.parseInt(args.get(0)) >= Integer.parseInt(args.get(1))) ? "1" : "0"
                        );
                        case "LE_OP" -> constStk.add(
                                (Integer.parseInt(args.get(0)) <= Integer.parseInt(args.get(1))) ? "1" : "0"
                        );
                        case "PLUS" -> {
                            if (node.index == 2)
                                constStk.add(
                                        String.valueOf(Integer.parseInt(args.get(0)) + Integer.parseInt(args.get(1)))
                                );
                            else
                                constStk.add(
                                        String.valueOf(Integer.parseInt(args.get(0)))
                                );
                        }
                        case "MINUS" -> {
                            if (node.index == 2)
                                constStk.add(
                                        String.valueOf(Integer.parseInt(args.get(0)) - Integer.parseInt(args.get(1)))
                                );
                            else
                                constStk.add(
                                        String.valueOf(-Integer.parseInt(args.get(0)))
                                );
                        }
                        case "MULTIPLY" -> constStk.add(
                                String.valueOf(Integer.parseInt(args.get(0)) * Integer.parseInt(args.get(1)))
                        );
                        case "SLASH" -> constStk.add(
                                String.valueOf(Integer.parseInt(args.get(0)) / Integer.parseInt(args.get(1)))
                        );
                        case "PERCENT" -> constStk.add(
                                String.valueOf(Integer.parseInt(args.get(0)) % Integer.parseInt(args.get(1)))
                        );
                        case "BITAND_OP" -> constStk.add(
                                String.valueOf(Integer.parseInt(args.get(0)) & Integer.parseInt(args.get(1)))
                        );
                        case "BITOR_OP" -> constStk.add(
                                String.valueOf(Integer.parseInt(args.get(0)) | Integer.parseInt(args.get(1)))
                        );
                        case "LEFT_OP" -> constStk.add(
                                String.valueOf(Integer.parseInt(args.get(0)) << Integer.parseInt(args.get(1)))
                        );
                        case "RIGHT_OP" -> constStk.add(
                                String.valueOf(Integer.parseInt(args.get(0)) >> Integer.parseInt(args.get(1)))
                        );
                        case "NOT_OP" -> constStk.add(
                                isBoolean(args.get(0)) ? "0" : "1"
                        );
                        case "BITINV_OP" -> constStk.add(
                                String.valueOf(~Integer.parseInt(args.get(0)))
                        );
                    }
                }
                if (!optimize) break;
            }
            if (optimize) {
                Quad quad = new Quad(INIT_CONST.getOp(), constStk.get(0), "", value.getRes());
                unfix = true;
                irParse.getQuads().set(index, quad);
                this.logs.add("常量传播与常量折叠：将位于" + entry.getKey() + "的四元式 " + value + " 优化为 " + quad);
            }
        }
        return unfix;
    }

    public static boolean isBoolean(String arg) {
        return "1".equals(arg);
    }
}
