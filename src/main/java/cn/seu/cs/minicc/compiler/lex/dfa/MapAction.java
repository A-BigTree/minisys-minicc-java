package cn.seu.cs.minicc.compiler.lex.dfa;

import lombok.Data;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/12/20
 */
@Data
public class MapAction {
    private int acceptStateIndex;
    private Action action;
}
