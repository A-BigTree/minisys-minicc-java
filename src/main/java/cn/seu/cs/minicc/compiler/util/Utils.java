package cn.seu.cs.minicc.compiler.util;

import cn.seu.cs.minicc.compiler.exception.PreException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/12/24
 */
public class Utils {
    public static String readCode(String fileName) {
        File file = new File(fileName);
        try {
            return Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e1) {
            throw new PreException("Cannot find file %s", fileName);
        } catch (OutOfMemoryError e2) {
            throw new PreException("File %s is too large", fileName);
        }
    }

    public static String getBasePath(String fileName) {
        File file = new File(fileName);
        return file.getParent();
    }
}
