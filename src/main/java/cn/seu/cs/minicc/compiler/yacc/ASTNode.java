package cn.seu.cs.minicc.compiler.yacc;

import cn.seu.cs.minicc.compiler.exception.YaccException;
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

    public ASTNode getByIndex(int index) throws YaccException {
        if (children == null || index < 0 || index > children.size()) {
            throw new YaccException("i超出范围：%s out-of %s", index, children == null ? 0 : children.size());
        }
        return children.get(index - 1);
    }

    public boolean match(String rhs) {
        String[] seq = rhs.trim().split(" ");
        if (seq.length == children.size()) {
            for (int i = 0; i < seq.length; i++) {
                if (!children.get(i).getName().equals(seq[i])) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }
}
