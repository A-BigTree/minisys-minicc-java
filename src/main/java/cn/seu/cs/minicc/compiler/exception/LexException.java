package cn.seu.cs.minicc.compiler.exception;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/11/12
 */
public class LexException extends Exception{
    public LexException(String message) {
        super(message);
    }

    public LexException(String format, Object...args) {
        this(format.formatted(args));
    }

    public LexException() {

    }
}
