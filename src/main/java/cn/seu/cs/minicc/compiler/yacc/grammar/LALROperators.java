package cn.seu.cs.minicc.compiler.yacc.grammar;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/12/20
 */
@Data
public class LALROperators {
    @JsonAlias("_symbolId")
    private Integer symbolId;

    @JsonAlias("_assoc")
    private String assoc;

    @JsonAlias("_precedence")
    private Integer precedence;
}
