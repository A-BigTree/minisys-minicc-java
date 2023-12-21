package cn.seu.cs.minicc.compiler.exception;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/11/12
 */
public class YaccException extends Exception{
    public YaccException(String message) {
        super(message);
    }

    public YaccException(String format, Object... args) {
        super(String.format(format, args));
    }
}
