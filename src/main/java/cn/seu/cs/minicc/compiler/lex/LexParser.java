package cn.seu.cs.minicc.compiler.lex;

import cn.seu.cs.minicc.compiler.exception.LexException;
import cn.seu.cs.minicc.compiler.lex.dfa.DFA;
import cn.seu.cs.minicc.compiler.lex.dfa.Transform;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/12/20
 */
@Slf4j
public class LexParser {

    public List<Token> lexSourceCode(String sourceCode, DFA dfa) throws LexException {
        String code = sourceCode.replace("\r\n", "\n");

        Integer initState = dfa.getStartStatesIndex().get(0);
        int yyLineNo = 1;
        String yyText = "";
        char curChar = ' ';
        StringBuilder curBuf = new StringBuilder();
        int curState = initState;
        int curPrt = 0;
        int lastAcceptState = -1;
        int lastAcceptPrt = 0;

        List<Token> tokens = new ArrayList<>();

        List<int[]> transMatrix = getTransMatrix(dfa);

        List<Integer> acceptList = getAcceptList(dfa);

        while (true) {
            int rollBackLine = 0;
            if (curPrt == code.length()) {
                break;
            }
            // 当前状态未结束
            while (curState != -1) {
                curChar = code.charAt(curPrt);
                curBuf.append(curChar);
                curPrt++;
                if (curChar == '\n') {
                    yyLineNo++;
                    rollBackLine++;
                }
                curState = transMatrix.get(curState)[curChar];
                // 半路到达接受状态
                if (curState != -1 && acceptList.get(curState) != -1) {
                    lastAcceptState = curState;
                    lastAcceptPrt = curPrt - 1;
                    rollBackLine = 0;
                }
                if (curPrt >= code.length()) {
                    break;
                }
            }
            // 处理接收情况
            if (lastAcceptState != -1) {
                // 回退多余的失败匹配
                curPrt = lastAcceptPrt + 1;
                yyLineNo -= rollBackLine;
                yyText = curBuf.substring(0, curBuf.length() - 1);
                curState = 0;
                curBuf = new StringBuilder();
                tokens.add(new Token(getTokenName(dfa.getAcceptActionMap().get(dfa.getStates().get(lastAcceptState).getUuid()).getCode()), yyText));
                lastAcceptState = -1;
                lastAcceptPrt = 0;
            } else {
                throw new LexException("无法识别的字符，行号：%s，字符：%s", yyLineNo, curChar);
            }
        }

        tokens.add(new Token("SP_END", ""));

        return tokens;
    }

    // 生成状态转义矩阵
    private List<int[]> getTransMatrix(DFA dfa) {
        List<int[]> transMatrix = new ArrayList<>();
        for (int i = 0; i < dfa.getTransformAdjList().size(); i++) {
            int[] targets = new int[128];
            Arrays.fill(targets, -1);
            int other = -1;
            for (Transform transform : dfa.getTransformAdjList().get(i)) {
                if (transform.getAlpha() == SpAlpha.OTHER.getIndex() || transform.getAlpha() == SpAlpha.ANY.getIndex()) {
                    other = transform.getTarget();
                } else {
                    targets[dfa.getAlphabet().get(transform.getAlpha()).charAt(0)] = transform.getTarget();
                }
            }
            if (other != -1) {
                for (int target : targets) {
                    if (target == -1) {
                        target = other;
                    }
                }
            }
            transMatrix.add(targets);
        }
        return transMatrix;
    }

    // 生成接受状态列表
    private List<Integer> getAcceptList(DFA dfa) {
        List<Integer> acceptList = new ArrayList<>(dfa.getStateCount());
        for (int i = 0; i < dfa.getStates().size(); i++) {
            if (dfa.getAcceptStates().contains(dfa.getStates().get(i))) {
                acceptList.add(i);
            } else {
                acceptList.add(-1);
            }
        }
        return acceptList;
    }

    // 拆解动作代码
    private String getTokenName(String actionCode) {
        return actionCode.replace(" ", "")
                .replace("return", "")
                .replace(";", "")
                .replace("(", "")
                .replace(")", "");
    }
}
