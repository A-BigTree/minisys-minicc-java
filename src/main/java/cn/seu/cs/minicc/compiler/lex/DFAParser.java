package cn.seu.cs.minicc.compiler.lex;

import cn.seu.cs.minicc.compiler.exception.LexException;
import cn.seu.cs.minicc.compiler.lex.dfa.Action;
import cn.seu.cs.minicc.compiler.lex.dfa.DFA;
import cn.seu.cs.minicc.compiler.lex.dfa.MapAction;
import cn.seu.cs.minicc.compiler.lex.dfa.State;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/12/20
 */
public class DFAParser {
    public DFA fromFile(String path) throws URISyntaxException, IOException, LexException {
        URL url = this.getClass().getClassLoader().getResource(path);
        if (url == null) {
            throw new LexException("Lex Parser error");
        }
        URI uri = url.toURI();
        String json = Files.readString(Paths.get(uri));
        ObjectMapper mapper = new ObjectMapper();
        DFA dfa = mapper.readValue(json, DFA.class);
        // 构建状态
        List<State> status = new ArrayList<>(dfa.getStateCount());
        for (int i = 0; i < dfa.getStateCount(); i++) {
            status.add(new State());
        }
        dfa.setStates(status);
        // 构建开始状态
        List<State> startStates = new ArrayList<>(dfa.getStartStatesIndex().size());
        for (Integer index : dfa.getStartStatesIndex()) {
            startStates.add(dfa.getStates().get(index));
        }
        dfa.setStartStates(startStates);
        // 构建接受状态
        List<State> acceptStates = new ArrayList<>(dfa.getAcceptStatesIndex().size());
        for (Integer index : dfa.getAcceptStatesIndex()) {
            acceptStates.add(dfa.getStates().get(index));
        }
        dfa.setAcceptStates(acceptStates);
        // 构建接受动作
        Map<Integer, Action> map = new HashMap<>(dfa.getAcceptActionList().size());
        for (MapAction mapAction : dfa.getAcceptActionList()) {
            map.put(dfa.getStates().get(mapAction.getAcceptStateIndex()).getUuid(), mapAction.getAction());
        }
        dfa.setAcceptActionMap(map);
        return dfa;
    }
}
