package cn.seu.cs.minicc.compiler.util;

import cn.seu.cs.minicc.compiler.Main;
import cn.seu.cs.minicc.compiler.exception.PreException;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * @author Shuxin Wang <shuxinwang662@gmail.com>
 * Created on 2023/12/24
 */
public class Utils {
    public static String readCode(String fileName) {
        File file = new File(fileName);
        return readCode(file);
    }

    public static String readCode(String fileName, String basePath) {
        File file = new File(basePath, fileName);
        return readCode(file);
    }

    private static String readCode(File file) {
        try {
            return Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e1) {
            throw new PreException("Cannot find file %s", file.getName());
        } catch (OutOfMemoryError e2) {
            throw new PreException("File %s is too large", file.getName());
        }
    }

    public static String getBasePath(String fileName) {
        File file = new File(fileName);
        return file.getParent();
    }

    public static String getFileName(String fileName) {
        File file = new File(fileName);
        return file.getName().substring(0, file.getName().lastIndexOf("."));
    }

    public static void writeFile(String basePath, String fileName, String content) {
        File file = new File(basePath, fileName);
        try {
            Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new PreException("Cannot write file %s", file.getName());
        }
    }

    public static String readJson(String path) {
        try {

            InputStream inputStream = Main.class.getClassLoader().getResourceAsStream(path);

            if (inputStream == null) {
                throw new PreException("Cannot find file %s", path);
            }
            StringBuilder stringBuffer=new StringBuilder();//用于解析文件
            byte[] buff=new byte[1024];
            int btr=0;
            while ((btr = inputStream.read(buff)) != -1){
                stringBuffer.append(new String(buff,0, btr, StandardCharsets.UTF_8));
            }
            inputStream.close();
            return stringBuffer.toString();
        } catch (Exception e) {
            throw new PreException("Cannot read file %s", path);
        }
    }
}
