package cn.seu.cs.minicc.compiler.lex;

import cn.seu.cs.minicc.compiler.lex.dfa.DFA;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.List;

@Slf4j
public class LexParserTest {

    @Test
    public void testLex() {
        String code = "int main() {\n" +
                "    int a = 1;\n" +
                "    int b = 2;\n" +
                "    int c = a + b;\n" +
                "    return c;\n" +
                "}";
        LexParser parser = new LexParser();
        try {
            DFA dfa = new DFAParser().fromFile("MiniC-Lex.json");
            List<Token> tokens = parser.lexSourceCode(code, dfa);
            for (Token token : tokens) {
                log.info("{}", token);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}