package cn.seu.cs.minicc.compiler.lex;

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
public class Token {
    // Token名称
    private String name;
    // 字面值
    private String literal;
}
