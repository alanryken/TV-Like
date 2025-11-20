package tv.tvai.like.chatGPT;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.InputStream;
import java.util.*;

public class PageExtractor {

    public static void main(String[] args) throws Exception {

        // 1️⃣ 读取本地 HTML 文件
        InputStream inputStream = PageExtractor.class.getClassLoader()
                .getResourceAsStream("mtyy1_com_index.html");
        if (inputStream == null) {
            System.err.println("找不到 HTML 文件！");
            return;
        }
        String path = "/vodtype/index.html";

        // === 2️⃣ 解析 HTML ===
        Document doc = Jsoup.parse(inputStream, "UTF-8", "");

        // === 3️⃣ 提取 DSL script 内容 ===
        String dsl = extractRulesFromScript(doc);
        if (dsl.isEmpty()) {
            System.err.println("没有找到 TVLike DSL script！");
            return;
        }

        // === 4️⃣ 解析 DSL → RuleNode 列表 ===
        RuleParser parser = new RuleParser();
        parser.parse(dsl);

        List<RuleNode> rules = parser.getPathRule(path);

        // === 5️⃣ 用 Extractor 解析 HTML，按 DOM 顺序输出 ===
        Extractor extractor = new Extractor();
        List<Map<String, Object>> sections = extractor.extract(doc, rules);

        // === 6️⃣ 输出结果 ===
        System.out.println("解析结果 JSON 样式输出：");

        ObjectMapper om = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        System.out.println(om.writeValueAsString(sections));

    }


    /**
     * 从 <script type="text/plain" name="tv-like"> 中提取 DSL 内容
     * JDK8 兼容
     */
    private static String extractRulesFromScript(Document doc) {
        Element script = doc.selectFirst("script[type=text/plain][name=tv-like]");
        if (script == null) return "";

        return script.html()
                .replace("&#10;", "\n")
                .replace("&#13;", "\r\n")
                .replace("\r\n", "\n")
                .replaceAll("/\\*.*?\\*/", ""); //去掉注释行
    }
}
