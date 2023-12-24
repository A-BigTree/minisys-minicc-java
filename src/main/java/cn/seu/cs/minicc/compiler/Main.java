package cn.seu.cs.minicc.compiler;

import cn.seu.cs.minicc.compiler.util.Utils;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/12/24
 */
@Slf4j
public class Main {
    public static void main(String[] args) {
        if (args.length != 2) {
            log.error("Usage: java -jar minicc-java.jar <input file> <output file>");
            return;
        }
        String input = args[0], output = args[1];
        log.info("Input file: {}", input);
        log.info("Output file: {}", output);
        try {
            String code = Utils.readCode(input);
            String basePath = Utils.getBasePath(input);
            log.info("Base path: {}", basePath);
            log.info("Code: {}", code);
        } catch (Exception e) {
            log.error("Error: {}", e.getMessage());
        }
    }
}
