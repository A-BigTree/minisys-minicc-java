package cn.seu.cs.minicc.compiler.yacc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/12/20
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ASTNode {
    private String name;
    private String type;
    private String literal;
    private List<ASTNode> children;

    public void pushNode(ASTNode node) {
        if (children == null) {
            children = new ArrayList<>();
        }
        children.add(node);
    }
}
