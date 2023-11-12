package cn.seu.cs.minicc.compiler.lex;

import cn.seu.cs.minicc.compiler.exception.LexException;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class RegexTest {

    @Test
    public void test() throws LexException {
        String test = "[1-3abcx-z].*\"abc\\d\"";
        log.info(new Regex(test).toString());
    }
}