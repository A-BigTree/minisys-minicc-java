package cn.seu.cs.minicc.compiler.yacc.grammar;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.util.List;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/12/20
 */
@Data
public class LALR {
    private String desc;
    private List<GrammarSymbol> symbols;
    private List<LALROperators> operators;
    private List<LALRProducer> producers;
    private Integer startSymbol;
    private LALRDfa dfa;

    @JsonAlias("ACTIONTable")
    private List<List<LALRAction>> actionTable;

    @JsonAlias("GOTOTable")
    private List<List<Integer>> gotoTable;

    @JsonAlias("ACTIONReverseLookup")
    private List<Integer> actionReverseLookup;

    @JsonAlias("GOTOReverseLookup")
    private List<Integer> gotoReverseLookup;

    private List<List<Integer>> first;
    private Integer epsilon;

    public String getLHS(LALRProducer producer) {
        return symbols.get(producer.getLhs()).getContent();
    }
}
