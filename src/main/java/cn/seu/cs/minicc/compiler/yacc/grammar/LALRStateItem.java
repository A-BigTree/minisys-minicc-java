package cn.seu.cs.minicc.compiler.yacc.grammar;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/12/20
 */
@Data
public class LALRStateItem {
    @JsonAlias("_lookahead")
    private Integer lookAhead;
    @JsonAlias("_producer")
    private Integer producer;
    @JsonAlias("_dotPosition")
    private Integer dotPosition;
    @JsonAlias("_rawProducer")
    private LALRProducer rawProducer;
}
