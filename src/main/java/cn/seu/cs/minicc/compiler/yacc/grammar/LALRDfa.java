package cn.seu.cs.minicc.compiler.yacc.grammar;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.util.List;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/12/20
 */
@Data
public class LALRDfa {
    @JsonAlias("_startStateId")
    private Integer startStateId;
    @JsonAlias("_states")
    private List<LALRState> states;
    @JsonAlias("_adjList")
    private List<List<LALRAdj>> adjList;

}
