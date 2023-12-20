package cn.seu.cs.minicc.compiler.yacc.grammar;

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
}
