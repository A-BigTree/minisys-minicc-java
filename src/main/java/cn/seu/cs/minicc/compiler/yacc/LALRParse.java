package cn.seu.cs.minicc.compiler.yacc;

import cn.seu.cs.minicc.compiler.exception.LexException;
import cn.seu.cs.minicc.compiler.exception.YaccException;
import cn.seu.cs.minicc.compiler.yacc.grammar.LALR;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/12/20
 */
@Slf4j
public class LALRParse {
    public LALR fromFile(String path) throws YaccException {
        LALR lalrAnalyzer = null;
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
            e.printStackTrace();
            throw new YaccException("Yacc Parser error");
        }
        return lalrAnalyzer;
    }

    public static void main(String[] args) throws YaccException {
        LALRParse lalrParse = new LALRParse();
        lalrParse.fromFile("MiniC-LALRParse.json");
    }
}
