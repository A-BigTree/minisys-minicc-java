package cn.seu.cs.minicc.compiler.lex;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class LexParserTest {

    @Test
    public void testLoadFile() throws Exception {
        log.info(new LexParser("MiniC.l").toString());
    }
}