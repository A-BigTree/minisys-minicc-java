package cn.seu.cs.minicc.compiler.yacc;

import cn.seu.cs.minicc.compiler.exception.YaccException;
import cn.seu.cs.minicc.compiler.yacc.grammar.LALR;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/12/20
 */
public class LALRParse {
    public LALR fromFile(String path) throws YaccException {
        LALR lalrAnalyzer;
        try {
            URL url = this.getClass().getClassLoader().getResource(path);
            if (url == null) {
                throw new YaccException("Yacc Parser error");
            }
            URI uri = url.toURI();
            String json = Files.readString(Paths.get(uri));
            ObjectMapper mapper = new ObjectMapper();
            lalrAnalyzer = mapper.readValue(json, LALR.class);
        } catch (Exception e) {
            throw new YaccException("Yacc Parser error");
        }
        return lalrAnalyzer;
    }
}
