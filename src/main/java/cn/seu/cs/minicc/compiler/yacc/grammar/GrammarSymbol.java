package cn.seu.cs.minicc.compiler.yacc.grammar;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/12/20
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GrammarSymbol {

    public static final GrammarSymbol END = new GrammarSymbol("sptoken", "SP_END");
    public static final GrammarSymbol EPSILON = new GrammarSymbol("sptoken", "SP_EPSILON");


    private String type;
    private String content;
}
