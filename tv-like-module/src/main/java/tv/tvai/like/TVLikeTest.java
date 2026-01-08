package tv.tvai.like;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class TVLikeTest {

    public static void main(String[] args) throws IOException {

        String html = inputStreamToString();
        String path = "https://www.dadaqu.cc/vodtype/index.html";

        String hub = "https://raw.githubusercontent.com/tv-like/dsl/refs/heads/main";

        List<Map<String, Object>> like = new TV(html, path, hub).like();

        ObjectMapper om = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        System.out.println(om.writeValueAsString(like));

    }

    public static String inputStreamToString() throws IOException {
        // 1️⃣ 读取本地 HTML 文件
        // 1. 获取类路径下的 InputStream（自动关闭资源）
        try (InputStream inputStream = TV.class.getClassLoader().getResourceAsStream("mtyy1_com_index4.html")) {
            if (inputStream == null) {
                throw new IllegalArgumentException("类路径下未找到文件：");
            }

            // 2. 关键：用 BufferedReader 按字符读取（而非按行），保留所有原始字符（包括 \r\n 或 \n）
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

                StringBuilder sb = new StringBuilder();
                int charCode; // 存储单个字符的 ASCII 码
                // 循环读取每个字符，直到结束（-1 表示流末尾）
                while ((charCode = reader.read()) != -1) {
                    sb.append((char) charCode); // 转为字符并追加
                }
                return sb.toString();
            }

        } catch (IOException e) {
            throw new RuntimeException("读取 HTML 文件失败：", e);
        }
    }
}
