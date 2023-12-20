package cn.seu.cs.minicc.compiler.lex.dfa;

import lombok.Data;

/**
 * 自动机状态转换
 *
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/11/12
 */
@Data
public class Transform {
    private int alpha;
    private int target;
}
