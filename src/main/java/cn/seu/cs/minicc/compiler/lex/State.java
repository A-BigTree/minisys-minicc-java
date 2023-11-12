package cn.seu.cs.minicc.compiler.lex;

import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 自动机状态
 *
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/11/12
 */
@Data
public class State {
    private int uuid;

    private static AtomicInteger PRIMARY_KEY = new AtomicInteger(1);

    public State(int uuid) {
        this.uuid = uuid;
    }

    public State() {
        this(PRIMARY_KEY.getAndIncrement());
    }

    public int hashCode() {
        return Integer.hashCode(uuid);
    }

    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o instanceof State s) {
            return this.uuid == s.getUuid();
        }
        return false;
    }

    public static boolean equals(State s1, State s2) {
        if (s1 == null || s2 == null) {
            return false;
        }
        return s1.getUuid() == s2.getUuid();
    }
}
