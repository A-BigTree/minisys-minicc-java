package cn.seu.cs.minicc.compiler;

import cn.seu.cs.minicc.compiler.asm.ASMParse;
import cn.seu.cs.minicc.compiler.ir.IROptimizer;
import cn.seu.cs.minicc.compiler.ir.IRParse;
import cn.seu.cs.minicc.compiler.lex.DFAParser;
import cn.seu.cs.minicc.compiler.lex.LexParser;
import cn.seu.cs.minicc.compiler.lex.Token;
import cn.seu.cs.minicc.compiler.lex.dfa.DFA;
import cn.seu.cs.minicc.compiler.pre.PreCompilerParse;
import cn.seu.cs.minicc.compiler.util.Utils;
import cn.seu.cs.minicc.compiler.yacc.ASTNode;
import cn.seu.cs.minicc.compiler.yacc.LALRParse;
import cn.seu.cs.minicc.compiler.yacc.YaccParse;
import cn.seu.cs.minicc.compiler.yacc.grammar.LALR;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/12/24
 */
@Slf4j
public class Main {
    public static void main(String[] args) {
        if (args.length < 2) {
            log.error("Usage: java -jar minicc-java.jar <input_file> <output_path> [-i]");
            return;
        }
        String input = args[0], output = args[1];
        boolean withIR = Arrays.asList(args).contains("-i");
        try {
            // 计时
            long startTime = System.currentTimeMillis();

            log.info("-------------------------------");
            log.info("=====[minisys-minicc-java]=====");
            log.info("-------------------------------");

            String basePath = Utils.getBasePath(input);
            String fileName = Utils.getFileName(input);

            log.info("*** Basic Information ***");
            log.warn("\tSource: {}", input);
            log.warn("\tOutput: {}", output);

            log.info("*** Start frontend part... ***");

            // 读入C代码
            log.warn("Reading source file...");
            String rawCode = Utils.readCode(input);
            if (rawCode.isBlank()) {
                log.error("ERROR: Empty source file");
                return;
            }
            log.info("Reading source file done");

            // 预编译
            log.warn("Start pre compiling...");
            String preCode = PreCompilerParse.preCompile(rawCode, basePath);
            log.info("Pre compiling done");

            // System.out.println(preCode);

            // 词法分析
            log.warn("Loading DFA for lexing...");
            DFAParser dfaParser = new DFAParser();
            DFA dfa = dfaParser.fromFile("MiniC-Lex.json");
            log.info("Loading DFA done");
            log.warn("Start tokenization...");
            LexParser lexicalAnalyzer = new LexParser();
            List<Token> tokens = lexicalAnalyzer.lexSourceCode(preCode, dfa);
            log.info("Tokenization done. Received {} tokens", tokens.size());

            // 语法分析
            log.warn("Loading parsing table for parsing...");
            LALRParse lalrParse = new LALRParse();
            LALR lalr = lalrParse.fromFile("MiniC-LALRParse.json");
            log.info("Loading parsing table done");
            log.warn("Start parsing...");
            YaccParse yaccParse = new YaccParse();
            ASTNode astNode = yaccParse.parseTokensLALR(tokens, lalr);
            if (astNode == null) {
                log.error("ERROR: Parsing failed, ASTNode is null");
                return;
            }
            log.info("Parsing done");

            log.info("*** Start backend part... ***");

            // 中间代码生成
            log.warn("Generating Intermediate Representation...");
            IRParse irParse = new IRParse(astNode);
            log.info("Generating Intermediate Representation done");

            // 中间代码优化
            log.warn("Optimizing Intermediate Representation...");
            IROptimizer optimizer = new IROptimizer(irParse);
            log.info("IR Optimizing done. Made {} changes", optimizer.getLogs().size());
            // 生成汇编代码
            log.warn("Generating Assembly Code...");
            ASMParse asmParse = new ASMParse(optimizer.getIrParse());
            log.info("Generating Assembly Code done");

            // 输出
            log.warn("Start output works...");
            String asmCode = asmParse.toString();
            Utils.writeFile(output, fileName + ".asm", asmCode);
            log.info("Object code output successfully.");
            if (withIR) {
                String irCode = optimizer.getIrParse().toString();
                Utils.writeFile(output, fileName + ".ir", irCode);
                log.info("IR code output successfully.");
            }
            log.info("Output works done");

            // 计时
            long endTime = System.currentTimeMillis();
            log.info("*** Summary ***");
            log.info("Compilation ended successfully with in {}ms", endTime - startTime);

        } catch (Exception e) {
            e.printStackTrace();
            log.error("ERROR: {}", e.getMessage());
        }
    }
}
