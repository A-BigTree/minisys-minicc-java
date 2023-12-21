package cn.seu.cs.minicc.compiler.exception;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/12/21
 */
public class IRException extends RuntimeException {
    public IRException(String message) {
        super(message);
    }

    public IRException(String format, Object... args) {
        super(String.format(format, args));
    }
}
