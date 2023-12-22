package cn.seu.cs.minicc.compiler.ir;

import cn.seu.cs.minicc.compiler.lex.DFAParser;
import cn.seu.cs.minicc.compiler.lex.LexParser;
import cn.seu.cs.minicc.compiler.lex.Token;
import cn.seu.cs.minicc.compiler.lex.dfa.DFA;
import cn.seu.cs.minicc.compiler.yacc.ASTNode;
import cn.seu.cs.minicc.compiler.yacc.LALRParse;
import cn.seu.cs.minicc.compiler.yacc.YaccParse;
import cn.seu.cs.minicc.compiler.yacc.grammar.LALR;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.*;

@Slf4j
public class IRParseTest {

    @Test
    public void testIR() throws IOException {
        File file = new File("D:\\ABigTree\\Github\\minisys-minicc-java\\Example.c");
        String code = Files.readString(file.toPath());
        LexParser parser = new LexParser();
        try {
            DFA dfa = new DFAParser().fromFile("MiniC-Lex.json");
            List<Token> tokens = parser.lexSourceCode(code, dfa);
            LALRParse lalrParse = new LALRParse();
            LALR lalr = lalrParse.fromFile("MiniC-LALRParse.json");
            YaccParse yaccParse = new YaccParse();
            ASTNode node = yaccParse.parseTokensLALR(tokens, lalr);
            IRParse irParse = new IRParse(node);
            log.info("\n{}", irParse);
        } catch (Exception e) {
            log.error("\n", e);
        }
    }

}