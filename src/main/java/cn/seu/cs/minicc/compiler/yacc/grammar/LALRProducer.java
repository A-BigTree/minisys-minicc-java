package cn.seu.cs.minicc.compiler.yacc.grammar;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.util.List;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/12/20
 */
@Data
public class LALRProducer {
    @JsonAlias("_lhs")
    private Integer lhs;

    @JsonAlias("_rhs")
    private List<Integer> rhs;

    @JsonAlias("_action")
    private String action;
}
