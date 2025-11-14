package tv.tvai.like;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.InputStream;
import java.util.Map;

public class PageExtractor {
    public static void main(String[] args) throws Exception {

        try (InputStream inputStream = PageExtractor.class.getClassLoader().getResourceAsStream("mtyy1_com_index.html")) {
            // === 2. 解析 HTML ===
            Document doc = Jsoup.parse(inputStream, "UTF-8", "");


            // === 3. 提取器：自动从 <script type="text/plain" name="tv-like"> 加载规则 ===
            Extractor extractor = new Extractor();
            extractor.parseRules(doc);  // 自动提取 script 规则

            // === 4. 提取结构化数据 ===
            Map<String, Object> jsonData = extractor.extractOrdered(doc);

            // === 5. 打印 JSON ===
            ObjectMapper mapper = new ObjectMapper();
            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonData));
        }
    }
}