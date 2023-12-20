package cn.seu.cs.minicc.compiler.lex.dfa;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/12/20
 */
@Data
public class DFA {
    private String desc;

    private List<String> alphabet;

    private Integer stateCount;
    private List<State> states;

    private List<Integer> startStatesIndex;
    private List<State> startStates;
    private List<Integer> acceptStatesIndex;
    private List<State> acceptStates;

    private List<List<Transform>> transformAdjList;

    private List<MapAction> acceptActionList;
    private Map<Integer, Action> acceptActionMap;
}
