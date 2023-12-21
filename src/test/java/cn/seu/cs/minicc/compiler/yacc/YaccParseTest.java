package cn.seu.cs.minicc.compiler.yacc;

import cn.seu.cs.minicc.compiler.lex.DFAParser;
import cn.seu.cs.minicc.compiler.lex.LexParser;
import cn.seu.cs.minicc.compiler.lex.Token;
import cn.seu.cs.minicc.compiler.lex.dfa.DFA;
import cn.seu.cs.minicc.compiler.yacc.grammar.LALR;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

@Slf4j
public class YaccParseTest {
    @Test
    public void testYacc() {
        String code = """ 
                int main(void) {
                  int a;
                  int b;
                  a = 10;
                  b = 20;
                  func(a, c);
                  return 0;
                }""";
        LexParser parser = new LexParser();
        try {
            DFA dfa = new DFAParser().fromFile("MiniC-Lex.json");
            List<Token> tokens = parser.lexSourceCode(code, dfa);
            LALRParse lalrParse = new LALRParse();
            LALR lalr = lalrParse.fromFile("MiniC-LALRParse.json");
            YaccParse yaccParse = new YaccParse();
            ASTNode node = yaccParse.parseTokensLALR(tokens, lalr);
            System.out.println(node);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}