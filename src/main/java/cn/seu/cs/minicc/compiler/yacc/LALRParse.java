package cn.seu.cs.minicc.compiler.yacc;

import cn.seu.cs.minicc.compiler.exception.YaccException;
import cn.seu.cs.minicc.compiler.util.Utils;
import cn.seu.cs.minicc.compiler.yacc.grammar.LALR;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/12/20
 */
public class LALRParse {
    public LALR fromFile(String path) throws YaccException {
        LALR lalrAnalyzer;
        try {
            String json = Utils.readJson(path);
            ObjectMapper mapper = new ObjectMapper();
            lalrAnalyzer = mapper.readValue(json, LALR.class);
        } catch (Exception e) {
            throw new YaccException("Yacc Parser error");
        }
        return lalrAnalyzer;
    }
}
