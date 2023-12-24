package cn.seu.cs.minicc.compiler.exception;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/12/24
 */
public class ASMException extends RuntimeException{
    public ASMException(String format, Object... args) {
        super(String.format(format, args));
    }
}
