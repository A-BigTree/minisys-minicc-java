package cn.seu.cs.minicc.compiler.constants;

import java.util.List;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/11/12
 */
public class Constants {
    public final static String EMPTY_STRING = "";
    public static final long IO_MAX_ADDR = 0xffffffffL;

    public static final List<String> USEFUL_REGS = List.of(
            "$t0", "$t1", "$t2", "$t3", "$t4", "$t5", "$t6", "$t7", "$t8", "$t9",  // 子程序可以破坏其中的值
            "$s0", "$s1", "$s2", "$s3", "$s4", "$s5", "$s6", "$s7"    // 子程序必须保持前后的值
    );

    public static final List<String> ALL_REGS = List.of(
            "$zero", "$at",
            "$v0", "$v1",
            "$a0", "$a1", "$a2", "$a3",
            "$t0", "$t1", "$t2", "$t3", "$t4", "$t5", "$t6", "$t7", "$t8", "$t9",
            "$s0", "$s1", "$s2", "$s3", "$s4", "$s5", "$s6", "$s7",
            "$k0", "$k1",
            "$gp", "$sp", "$fp",
            "$ra"
    );

    public static final int WORD_LENGTH_BIT = 32;
    public static final int WORD_LENGTH_BYTE = 4;

    public static final int RAM_SIZE = 65536;

    public static final int ROM_SIZE = 65536;
}
