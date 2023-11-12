package cn.seu.cs.minicc.compiler.utils;

import cn.seu.cs.minicc.compiler.common.RangeIndex;
import cn.seu.cs.minicc.compiler.exception.LexException;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/11/12
 */
@Slf4j
public class LexUtils {
    public static List<RangeIndex> getMatchedRanges(Pattern regex, String str) {
        Matcher matcher = regex.matcher(str);
        List<RangeIndex> result = new ArrayList<>();
        while (matcher.find()) {
            result.add(new RangeIndex(matcher.start(), matcher.end() - 1));
        }
        return result;
    }

    public static boolean inRanges(List<RangeIndex> ranges, int target) {
        return ranges.stream()
                .anyMatch(range -> range.inRange(target));
    }

    public static String replace(Pattern regex, String str,
                                 Function<String, String> action){
        Matcher matcher = regex.matcher(str);
        return matcher.replaceAll(replacer -> action.apply(replacer.group()));
    }

    public static List<String> lineSpace(Character left, Character right) throws LexException {
        if (right < left) {
            throw new LexException();
        }
        List<String> result = new ArrayList<>(right - left + 1);
        for (char c = left; c <= right; c++) {
            result.add(String.valueOf(c));
        }
        return result;
    }

    public static RangeIndex firstMatch(Pattern pattern, String str) {
        Matcher matcher = pattern.matcher(str);
        if (matcher.find()) {
            return new RangeIndex(matcher.start(), matcher.end());
        } else {
            return null;
        }
    }
}
