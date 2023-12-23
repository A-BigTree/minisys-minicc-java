package cn.seu.cs.minicc.compiler.ir;

import cn.seu.cs.minicc.compiler.exception.IRException;
import cn.seu.cs.minicc.compiler.exception.YaccException;
import cn.seu.cs.minicc.compiler.ir.compont.*;
import cn.seu.cs.minicc.compiler.yacc.ASTNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

import static cn.seu.cs.minicc.compiler.ir.compont.MiniCType.*;
import static cn.seu.cs.minicc.compiler.ir.compont.QuadOpType.*;
import static cn.seu.cs.minicc.compiler.yacc.grammar.ASTNodeName.*;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/12/21
 */
@Data
public class IRParse {
    public static final List<Integer> GLOBAL_SCOPE = new ArrayList<>(List.of(0));
    public static final String LABEL_PREFIX = "_label_";
    public static final String VAR_PREFIX = "_var_";


    /**
     * loop栈内元素
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LoopEntry {
        private String loopLabel;
        private String breakLabel;
    }

    /**
     * 作用域函数调用
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ScopeFunc {
        private List<Integer> scopePath;
        private String funcName;
    }

    /**
     * 函数上下文
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FuncContext {
        private String funcName;
        private String entryLabel;
        private String exitLabel;
    }

    private final List<IRFunc> funcPool;
    private final List<Quad> quads;
    private final List<BasicBlock> basicBlocks;
    private final List<AbstractIRVal> valPool;
    private Integer varCount;
    private Integer labelCount;
    private Integer scopeCount;
    private List<Integer> scopePath;
    private List<LoopEntry> loopStack;
    private List<PostChecker> postChecks;
    private List<ScopeFunc> callInScope;

    public IRParse(ASTNode node) {
        this.varCount = 0;
        this.labelCount = 0;
        this.scopeCount = 0;
        this.funcPool = new ArrayList<>();
        this.quads = new ArrayList<>();
        this.basicBlocks = new ArrayList<>();
        this.valPool = new ArrayList<>();
        this.loopStack = new ArrayList<>();
        this.postChecks = new ArrayList<>();
        this.callInScope = new ArrayList<>();
        this.scopePath = GLOBAL_SCOPE;
        // 分析语法树
        start(node);
        // 添加内置函数
        scopePath.add(++scopeCount);
        funcPool.add(new IRFunc(
                "__asm",
                VOID,
                newLabel("__asm_entry"),
                newLabel("__asm_exit"),
                true,
                new ArrayList<>(List.of(new IRVar(newVarId(), "asm", STRING, new ArrayList<>(scopePath), true))),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(scopePath)
        ));
        scopePath.remove(scopePath.size() - 1);
        // 后置检查
        postProcess1();
        postCheck();
        postProcess2();
        // 基本块划分
        toBasicBlocks();
    }

    private void postProcess1() {
        // 补充函数信息
        for (IRFunc func : funcPool) {
            func.getLocalVars().addAll(
                    valPool.stream()
                            .filter(v -> inScope(func.getScopePath(), v.getScope()))
                            .toList()
            );
            func.getChildFunctions().addAll(
                    new HashSet<String>(callInScope.stream()
                            .filter(call -> inScope(func.getScopePath(), call.scopePath))
                            .map(ScopeFunc::getFuncName)
                            .toList())
            );
        }
    }

    private void postCheck() {
        for (PostChecker checker : postChecks) {
            for (int i = 0; i < checker.getCheckers().size(); i++) {
                if (!checker.getCheckers().get(i).test(checker.getParams().get(i))) {
                    throw new IRException(checker.getHint());
                }
            }
        }
        if (funcPool.stream().noneMatch(func -> func.getName().equals("main"))) {
            throw new IRException("未定义main函数");
        }
        for (IRFunc func : funcPool) {
            if (!func.getHasReturn() && !func.getChildFunctions().contains("__asm")) {
                throw new IRException("函数 %s 无返回语句", func.getName());
            }
        }
    }

    private void postProcess2() {
        for (int i = 0; i < quads.size(); i++) {
            Quad quad = quads.get(i);
            if (quad.getOp().equals("call") && quad.getArg1().equals("__asm")) {
                if (i < 1) {
                    throw new IRException("asm函数调用不在函数内");
                }
                Quad prev = quads.get(i - 1);
                if (prev.getArg2().split("&").length != 1) {
                    throw new IRException("asm函数调用参数不为1");
                }
                if (!prev.getOp().equals("=string")) {
                    throw new IRException("asm函数调用参数不为字符串");
                }
                quads.set(i, new Quad("out_asm", prev.getArg1(), "", ""));
                quads.set(i - 1, null);
            }
        }
        quads.removeIf(Objects::isNull);
    }

    public void toBasicBlocks() {
        List<Integer> leaders = new ArrayList<>();
        boolean nextFlag = false;
        for (int i = 0; i < quads.size(); i++) {
            if (i == 0) {
                leaders.add(i);
                continue;
            }
            if (SET_LABEL.equals(quads.get(i).getOp()) &&
                    quads.get(i).getRes().contains("entry")) {
                leaders.add(i);
                continue;
            }
            if (JUMP.equals(quads.get(i).getOp()) ||
                    J_FALSE.equals(quads.get(i).getOp())) {
                for (int j = 0; j < quads.size(); j++) {
                    if (SET_LABEL.equals(quads.get(j).getOp()) &&
                            quads.get(j).getRes().equals(quads.get(i).getRes())) {
                        leaders.add(j);
                        break;
                    }
                }
                nextFlag = true;
                continue;
            }
            if (nextFlag) {
                leaders.add(i);
                nextFlag = false;
            }
        }
        leaders.sort((Comparator.comparingInt(o -> o)));
        List<Integer> leaders2 = new ArrayList<>();
        for (Integer index : leaders) {
            if (!leaders2.contains(index)) {
                leaders2.add(index);
            }
        }
        if (leaders2.get(leaders2.size() - 1) != quads.size()) {
            leaders2.add(quads.size());
        }
        int id = 0;
        for (int i = 0; i < leaders2.size() - 1; i++) {
            basicBlocks.add(
                    new BasicBlock(id++,
                            quads.subList(leaders2.get(i),
                                    leaders2.get(i + 1)))
            );
        }

    }

    private String newVarId() {
        return VAR_PREFIX + varCount++;
    }

    private String newLabel(String desc) {
        return LABEL_PREFIX + labelCount++ + "_" + desc;
    }

    private static boolean sameScope(List<Integer> scope1, List<Integer> scope2) {
        if (scope1.size() != scope2.size()) {
            return false;
        }
        for (int i = 0; i < scope1.size(); i++) {
            if (!scope1.get(i).equals(scope2.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameVal(AbstractIRVal val1, AbstractIRVal val2) {
        return val1.getName().equals(val2.getName()) && sameScope(val1.getScope(), val2.getScope());
    }

    private static boolean inScope(List<Integer> scope1, List<Integer> scope2) {
        if (scope1.size() > scope2.size()) {
            return false;
        }
        for (int i = 0; i < scope1.size(); i++) {
            if (!scope1.get(i).equals(scope2.get(i))) {
                return false;
            }
        }
        return true;
    }

    private AbstractIRVal findVal(String name) throws IRException {
        List<List<Integer>> validScope = new ArrayList<>();
        for (int i = scopePath.size() - 1; i >= 0; i--) {
            List<Integer> scope = new ArrayList<>();
            for (int j = 0; j <= i; j++) {
                scope.add(scopePath.get(j));
            }
            validScope.add(scope);
        }
        for (List<Integer> scope : validScope) {
            for (AbstractIRVal val : valPool) {
                if (val.getName().equals(name) && sameScope(val.getScope(), scope)) {
                    return val;
                }
            }
        }
        throw new IRException("未找到变量：%s", name);
    }

    private void start(ASTNode node) throws IRException, YaccException {
        if (node == null) {
            throw new IRException("AST根节点为null");
        }
        parseDeclList(node.getByIndex(1));
    }

    private void parseDeclList(ASTNode node) throws YaccException, IRException {
        if (DECL_LIST.equals(node.getByIndex(1).getName())) {
            parseDeclList(node.getByIndex(1));
            parseDecl(node.getByIndex(2));
        }
        if (DECL.equals(node.getByIndex(1).getName())) {
            parseDecl(node.getByIndex(1));
        }
    }

    private void parseDecl(ASTNode node) throws YaccException, IRException {
        if (VAR_DECL.equals(node.getByIndex(1).getName())) {
            parseVarDecl(node.getByIndex(1));
        }
        if (FUN_DECL.equals(node.getByIndex(1).getName())) {
            parseFunDecl(node.getByIndex(1));
        }
    }

    private void parseVarDecl(ASTNode node) throws YaccException, IRException {
        if (node.match("type_spec IDENTIFIER")) {
            MiniCType type = parseTypeSpec(node.getByIndex(1));
            String name = node.getByIndex(2).getLiteral();
            if (type == VOID) {
                throw new IRException("void类型变量声明：%s", name);
            }
            scopePath = GLOBAL_SCOPE;
            if (valPool.stream()
                    .anyMatch(val -> sameScope(val.getScope(), GLOBAL_SCOPE) && val.getName().equals(name))) {
                throw new IRException("重复声明变量：%s", name);
            }
            valPool.add(new IRVar(newVarId(), name, type, new ArrayList<>(scopePath), false));
        }
        if (node.match("type_spec IDENTIFIER CONSTANT")) {
            MiniCType type = parseTypeSpec(node.getByIndex(1));
            String name = node.getByIndex(2).getLiteral();
            int len = Integer.parseInt(node.getByIndex(3).getLiteral());
            scopePath = GLOBAL_SCOPE;
            if (len <= 0) {
                throw new IRException("数组长度错误：%s", len);
            }
            valPool.add(new IRArray(newVarId(), name, type, scopePath, len));
        }
    }

    private void parseFunDecl(ASTNode node) throws YaccException, IRException {
        MiniCType retType = parseTypeSpec(node.getByIndex(1));
        String funcName = node.getByIndex(2).getLiteral();
        if (funcPool.stream().anyMatch(func -> func.getName().equals(funcName))) {
            throw new IRException("重复声明函数：%s", funcName);
        }
        String entryLabel = newLabel(funcName + "_entry");
        String exitLabel = newLabel(funcName + "_exit");
        // 进一层作用域
        scopePath.add(++scopeCount);
        funcPool.add(new IRFunc(funcName, retType, entryLabel, exitLabel, false,
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(scopePath)));
        quads.add(new Quad(SET_LABEL.getOp(), "", "", entryLabel));
        parseParams(node.getByIndex(3), funcName);
        if (node.getChildren().size() == 5) {
            parseLocalDecls(node.getByIndex(4));
            parseStmtList(node.getByIndex(5), new FuncContext(funcName, entryLabel, exitLabel));
        } else if (node.getChildren().size() == 4) {
            parseStmtList(node.getByIndex(4), new FuncContext(funcName, entryLabel, exitLabel));
        }
        // 退出作用域
        quads.add(new Quad(SET_LABEL.getOp(), "", "", exitLabel));
        scopePath.remove(scopePath.size() - 1);
    }

    private MiniCType parseTypeSpec(ASTNode node) {
        return getByType(node.getByIndex(1).getLiteral());
    }

    private void parseParams(ASTNode node, String funcName) throws YaccException, IRException {
        if (VOID.equals(node.getByIndex(1).getName())) {
            funcPool.stream()
                    .filter(func -> func.getName().equals(funcName))
                    .forEach(func -> func.setParamList(new ArrayList<>()));
        }
        if (PARAM_LIST.equals(node.getByIndex(1).getName())) {
            parseParamList(node.getByIndex(1), funcName);
        }
    }

    private void parseParamList(ASTNode node, String funcName) throws YaccException, IRException {
        if (PARAM_LIST.equals(node.getByIndex(1).getName())){
            parseParamList(node.getByIndex(1), funcName);
            parseParam(node.getByIndex(2), funcName);
        }
        if (PARAM.equals(node.getByIndex(1).getName())) {
            parseParam(node.getByIndex(1), funcName);
        }
    }

    private void parseParam(ASTNode node, String funcName) throws YaccException, IRException {
        MiniCType type = parseTypeSpec(node.getByIndex(1));
        String name = node.getByIndex(2).getLiteral();
        if (type == VOID) {
            throw new IRException("void类型变量声明：%s", name);
        }
        IRVar var = new IRVar(newVarId(), name, type, new ArrayList<>(scopePath), true);
        valPool.add(var);
        funcPool.stream()
                .filter(func -> func.getName().equals(funcName))
                .forEach(func -> func.getParamList().add(var));

    }

    private void parseLocalDecls(ASTNode node) throws YaccException, IRException {
        if (LOCAL_DECL.equals(node.getByIndex(1).getName())) {
            parseLocalDecl(node.getByIndex(1));
        }
        if (LOCAL_DECLS.equals(node.getByIndex(1).getName())) {
            parseLocalDecls(node.getByIndex(1));
            parseLocalDecl(node.getByIndex(2));
        }
    }

    private void parseLocalDecl(ASTNode node) throws IRException, YaccException {
        if (node.getChildren().size() == 2) {
            MiniCType type = parseTypeSpec(node.getByIndex(1));
            String name = node.getByIndex(2).getLiteral();
            if (type == VOID) {
                throw new IRException("void类型变量声明：%s", name);
            }
            IRVar val = new IRVar(newVarId(), name, type, new ArrayList<>(scopePath), false);
            if (valPool.stream()
                    .anyMatch(v -> sameVal(v, val))) {
                throw new IRException("重复声明局部变量：%s", name);
            }
            valPool.add(val);
        }
        if (node.getChildren().size() == 3) {
            throw new IRException("数组声明只能声明在全局域中，而%s不符合", node.getByIndex(2).getLiteral());
        }
    }

    private void parseStmtList(ASTNode node, FuncContext context) throws YaccException, IRException {
        if (STMT_LIST.equals(node.getByIndex(1).getName())) {
            parseStmtList(node.getByIndex(1), context);
            parseStmt(node.getByIndex(2), context);
        }
        if (STMT.equals(node.getByIndex(1).getName())) {
            parseStmt(node.getByIndex(1), context);
        }
    }

    private void parseStmt(ASTNode node, FuncContext context) throws YaccException, IRException {
        if (EXPR_STMT.equals(node.getByIndex(1).getName())) {
            parseExprStmt(node.getByIndex(1));
        }
        if (COMP_STMT.equals(node.getByIndex(1).getName())) {
            parseCompoundStmt(node.getByIndex(1), context);
        }
        if (IF_STMT.equals(node.getByIndex(1).getName())) {
            parseIfStmt(node.getByIndex(1), context);
        }
        if (WHILE_STMT.equals(node.getByIndex(1).getName())) {
            parseWhileStmt(node.getByIndex(1), context);
        }
        if (RETURN_STMT.equals(node.getByIndex(1).getName())) {
            parseReturnStmt(node.getByIndex(1), context);
        }
        if (BREAK_STMT.equals(node.getByIndex(1).getName())) {
            parseBreakStmt(node.getByIndex(1));
        }
        if (CONTINUE_STMT.equals(node.getByIndex(1).getName())) {
            parseContinueStmt(node.getByIndex(1));
        }
    }

    private void parseContinueStmt(ASTNode node) {
        if (loopStack.isEmpty()) {
            throw new IRException("continue语句不在循环中");
        }
        quads.add(new Quad(JUMP.getOp(), "", "", loopStack.get(loopStack.size() - 1).getLoopLabel()));
    }

    private void parseExprStmt(ASTNode node) {
        // 变量赋值
        if (node.match("IDENTIFIER ASSIGN expr")) {
            IRVar lhs = (IRVar) findVal(node.getByIndex(1).getLiteral());
            lhs.setHasInit(true);
            String rhs = parseExpr(node.getByIndex(3));
            quads.add(new Quad(INIT_VAL.getOp(), rhs, "", lhs.getId()));
        }
        // 数组赋值
        if (node.match("IDENTIFIER expr ASSIGN expr")) {
            IRArray arr = (IRArray) findVal(node.getByIndex(1).getLiteral());
            String index = parseExpr(node.getByIndex(2));
            String rhs = parseExpr(node.getByIndex(4));
            quads.add(new Quad(INIT_ARRAY.getOp(), index, rhs, arr.getId()));
        }
        // 访问地址
        if (node.match("DOLLAR expr ASSIGN expr")) {
            String addr = parseExpr(node.getByIndex(2));
            String rhs = parseExpr(node.getByIndex(4));
            quads.add(new Quad(INIT_ADDR.getOp(), addr, rhs, ""));
        }
        // 调函数
        if (node.match("IDENTIFIER args")) {
            List<String> args = parseArgs(node.getByIndex(2));
            String funcName = node.getByIndex(1).getLiteral();
            if (funcName.equals("main")) {
                throw new IRException("main函数不允许调用");
            }
            PostChecker checker1 = new PostChecker(new ArrayList<>(), new ArrayList<>());
            checker1.addChecker(
                    val -> funcPool.stream().anyMatch(func -> func.getName().equals(val)),
                    funcName
            );
            checker1.setHint("未声明就调用了函数 " + funcName);
            postChecks.add(checker1);
            quads.add(new Quad(CALL_FUNC.getOp(), funcName, String.join("&", args), ""));
            callInScope.add(new ScopeFunc(new ArrayList<>(scopePath), funcName));
        }
        // 调函数 无参
        if (node.match("IDENTIFIER LPAREN RPAREN")) {
            String funcName = node.getByIndex(1).getLiteral();
            if (funcName.equals("main")) {
                throw new IRException("main函数不允许调用");
            }
            PostChecker checker1 = new PostChecker(new ArrayList<>(), new ArrayList<>());
            checker1.addChecker(
                    val -> funcPool.stream().anyMatch(func -> func.getName().equals(val)),
                    funcName
            );
            checker1.setHint("未声明就调用了函数 " + funcName);
            postChecks.add(checker1);
            quads.add(new Quad(CALL_FUNC.getOp(), funcName, "", ""));
            callInScope.add(new ScopeFunc(new ArrayList<>(scopePath), funcName));
        }
    }

    private String parseExpr(ASTNode node) {
        if (node.match("LPAREN expr RPAREN")) {
            String op = parseExpr(node.getByIndex(2));
            String res = newVarId();
            quads.add(new Quad(INIT_VAL.getOp(), op, "", res));
            return res;
        }
        if (node.match("IDENTIFIER")) {
            IRVar var = (IRVar) findVal(node.getByIndex(1).getLiteral());
            if (!var.isHasInit()) {
                throw new IRException("变量未初始化：%s", var.getName());
            }
            return var.getId();
        }
        if (node.match("IDENTIFIER expr")) {
            String index = parseExpr(node.getByIndex(2));
            String name = node.getByIndex(1).getLiteral();
            String res = newVarId();
            quads.add(new Quad(READ_ARRAY.getOp(), findVal(name).getId(), index, res));
            return res;
        }
        // 调用函数 有参
        if (node.match("IDENTIFIER args")) {
            List<String> args = parseArgs(node.getByIndex(2));
            String funcName = node.getByIndex(1).getLiteral();
            if (funcName.equals("main")) {
                throw new IRException("main函数不允许调用");
            }
            PostChecker checker1 = new PostChecker(new ArrayList<>(), new ArrayList<>());
            checker1.addChecker(
                    val -> funcPool.stream().anyMatch(func -> func.getName().equals(val)),
                    funcName
            );
            checker1.setHint("未声明就调用了函数 " + funcName);
            postChecks.add(checker1);
            String res = newVarId();
            quads.add(new Quad(CALL_FUNC.getOp(), funcName, String.join("&", args), res));
            callInScope.add(new ScopeFunc(new ArrayList<>(scopePath), funcName));
            return res;
        }
        // 调用函数 无参
        if (node.match("IDENTIFIER LPAREN RPAREN")) {
            String funcName = node.getByIndex(1).getLiteral();
            if (funcName.equals("main")) {
                throw new IRException("main函数不允许调用");
            }
            PostChecker checker1 = new PostChecker(new ArrayList<>(), new ArrayList<>());
            checker1.addChecker(
                    val -> funcPool.stream().anyMatch(func -> func.getName().equals(val)),
                    funcName
            );
            checker1.setHint("未声明就调用了函数 " + funcName);
            postChecks.add(checker1);
            String res = newVarId();
            quads.add(new Quad(CALL_FUNC.getOp(), funcName, "", res));
            callInScope.add(new ScopeFunc(new ArrayList<>(scopePath), funcName));
            return res;
        }
        // 常量
        if (node.match("CONSTANT")) {
            String res = newVarId();
            quads.add(new Quad(INIT_CONST.getOp(), node.getByIndex(1).getLiteral(), "", res));
            return res;
        }
        // 字符串字面量
        if (node.match("STRING_LITERAL")) {
            String res = newVarId();
            quads.add(new Quad(INIT_STR.getOp(), node.getByIndex(1).getLiteral(), "", res));
            return res;
        }
        // 处理二元运算
        if (node.getChildren().size() == 3 && node.getByIndex(1).getName().equals("expr") && node.getByIndex(3).getName().equals("expr")) {
            String op1 = parseExpr(node.getByIndex(1));
            String op2 = parseExpr(node.getByIndex(3));
            String op = node.getByIndex(2).getName();
            String res = newVarId();
            quads.add(new Quad(op, op1, op2, res));
            return res;
        }
        // 处理一元运算
        if (node.getChildren().size() == 2) {
            String op1 = parseExpr(node.getByIndex(2));
            String op = node.getByIndex(1).getName();
            String res = newVarId();
            quads.add(new Quad(op, op1, "", res));
            return res;
        }
        throw new IRException("未知表达式类型：%s", node.getName());
    }

    private List<String> parseArgs(ASTNode node) {
        List<String> args = new ArrayList<>();
        if (EXPR.equals(node.getByIndex(1).getName())) {
            args.add(parseExpr(node.getByIndex(1)));
            return args;
        }
        if (ARGS.equals(node.getByIndex(1).getName())) {
            args.addAll(parseArgs(node.getByIndex(1)));
            args.add(parseExpr(node.getByIndex(2)));
            return args;
        }
        return args;
    }

    private void parseCompoundStmt(ASTNode node, FuncContext context) {
        scopePath.add(++scopeCount);
        if (node.getChildren().size() == 2) {
            parseLocalDecls(node.getByIndex(1));
            parseStmtList(node.getByIndex(1), context);
        } else if (node.getChildren().size() == 1) {
            parseStmtList(node.getByIndex(1), context);
        }
        scopePath.remove(scopePath.size() - 1);
    }

    private void parseIfStmt(ASTNode node, FuncContext context) {
        String expr = parseExpr(node.getByIndex(1));
        String trueLabel = newLabel("true");
        String falseLabel = newLabel("false");
        quads.add(new Quad(SET_LABEL.getOp(), "", "", trueLabel));
        quads.add(new Quad(J_FALSE.getOp(), expr, "", falseLabel));
        parseStmt(node.getByIndex(2), context);
        quads.add(new Quad(SET_LABEL.getOp(), "", "", falseLabel));
    }

    private void parseWhileStmt(ASTNode node, FuncContext context) {
        String loopLabel = newLabel("loop");
        String breakLabel = newLabel("break");
        loopStack.add(new LoopEntry(loopLabel, breakLabel));
        quads.add(new Quad(SET_LABEL.getOp(), "", "", loopLabel));
        String expr = parseExpr(node.getByIndex(1));
        quads.add(new Quad(J_FALSE.getOp(), expr, "", breakLabel));
        parseStmt(node.getByIndex(2), context);
        quads.add(new Quad(JUMP.getOp(), "", "", loopLabel));
        quads.add(new Quad(SET_LABEL.getOp(), "", "", breakLabel));
        loopStack.remove(loopStack.size() - 1);
    }

    private void parseReturnStmt(ASTNode node, FuncContext context) {
        funcPool.stream()
                .filter(func -> func.getName().equals(context.getFuncName()))
                .forEach(func -> func.setHasReturn(true));
        if (node.getChildren().isEmpty()) {
            PostChecker checker = new PostChecker(new ArrayList<>(), new ArrayList<>());
            checker.addChecker(
                    val -> funcPool.stream().anyMatch(func -> func.getName().equals(val) && func.getRetType() == VOID),
                    context.getFuncName()
                    );
            checker.setHint("函数 " + context.getFuncName() + " 无返回值");
            postChecks.add(checker);
            quads.add(new Quad(RETURN_VOID.getOp(), "", "", context.getExitLabel()));
        }
        if (node.getChildren().size() == 1) {
            PostChecker checker = new PostChecker(new ArrayList<>(), new ArrayList<>());
            checker.addChecker(
                    val -> funcPool.stream().anyMatch(func -> func.getName().equals(val) && func.getRetType() != VOID),
                    context.getFuncName()
            );
            checker.setHint("函数 " + context.getFuncName() + " 返回类型为void， 却有返回值");
            postChecks.add(checker);
            String expr = parseExpr(node.getByIndex(1));
            quads.add(new Quad(RETURN_EXPR.getOp(), expr, "", context.getExitLabel()));
        }
    }

    private void parseBreakStmt(ASTNode node) {
        if (loopStack.isEmpty()) {
            throw new IRException("break语句不在循环中");
        }
        quads.add(new Quad(JUMP.getOp(), "", "", loopStack.get(loopStack.size() - 1).getBreakLabel()));
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("函数池：\n");
        for (IRFunc func : funcPool) {
            sb.append(func);
            sb.append("\n");
        }

        sb.append("全局变量：\n");
        for (AbstractIRVal val : valPool) {
            if (sameScope(val.getScope(), GLOBAL_SCOPE)) {
                sb.append(val);
                sb.append("\n");
            }
        }

        sb.append("变量池：\n");
        for (AbstractIRVal val : valPool) {
            sb.append(val);
            sb.append("\n");
        }

        sb.append("四元式：\n");
        for (Quad quad : quads) {
            sb.append(quad);
            sb.append("\n");
        }
        return sb.toString();
    }

    public List<AbstractIRVal> getGlobalVars() {
        return valPool.stream()
                .filter(val -> sameScope(val.getScope(), GLOBAL_SCOPE))
                .toList();
    }

}
