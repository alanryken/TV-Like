package tv.tvai.like;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TVLikeTest {

    public static void main(String[] args) throws IOException {
//        String html = FileToString.get("sszzyy.com.html");
//        String url = "https://sszzyy.com/";
//        String dsl =  FileToString.get("sszzyy.com.yaml");

        String url = "https://sszzyy.com/index.php/vod/type/id/20.html";
        String html = FileToString.get("sszzyy.com/index.php/vod/type/id/20.html");
        String dsl = FileToString.get("sszzyy.com/index.php/vod/type/id/20.yaml");

        List<SectionResult> like = new TV(html, url, null, dsl).like();
//         List<SectionResult> like = new TV(html, url).like();
         ObjectMapper om = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
         System.out.println(om.writeValueAsString(like));
    }

    public static String inputStreamToString() throws IOException {
        try (InputStream inputStream = TV.class.getClassLoader().getResourceAsStream("libvio.lat.index.html")) {
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

    public static String getDslString() throws IOException {
        try (InputStream inputStream = TV.class.getClassLoader().getResourceAsStream("libvio.lat.index.yaml")) {
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
            throw new RuntimeException("读取 YAML 文件失败：", e);
        }
    }
}
