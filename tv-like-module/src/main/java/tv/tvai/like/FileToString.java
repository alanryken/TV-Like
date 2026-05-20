package tv.tvai.like;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class FileToString {
    public static String get(String fileName) throws IOException {
        try (InputStream inputStream = TV.class.getClassLoader().getResourceAsStream(fileName)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("类路径下未找到文件");
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                int charCode;
                while ((charCode = reader.read()) != -1) {
                    sb.append((char) charCode);
                }
                return sb.toString();
            }
        } catch (IOException e) {
            throw new RuntimeException("读取 HTML 文件失败：", e);
        }
    }
}
