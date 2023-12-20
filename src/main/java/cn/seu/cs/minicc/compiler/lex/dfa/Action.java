package cn.seu.cs.minicc.compiler.lex.dfa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 表示一段动作代码及其出现次序
 *
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/11/12
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Action {
    private int order;
    private String code;
}
