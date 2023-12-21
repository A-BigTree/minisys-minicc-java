package cn.seu.cs.minicc.compiler.ir.compont;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.function.Predicate;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/12/21
 */
@Data
@AllArgsConstructor
public class PostChecker {
    private List<Predicate<Object>> checkers;
    private List<Object> params;
    private String hint;

    public PostChecker(List<Predicate<Object>> checkers, List<Object> params) {
        this.checkers = checkers;
        this.params = params;
    }

    public void addChecker(Predicate<Object> checker, Object param) {
        checkers.add(checker);
        params.add(param);
    }
}
