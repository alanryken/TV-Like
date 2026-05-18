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
        String html = inputStreamToString();
        String url = "https://www.libvio.lat/";
//        Document doc = Jsoup.parse(html, url);
//
//        TVLikeDSL tvLikeDSL = new TVLikeDSL();
//        String dsl = tvLikeDSL.getDSL(doc, new java.net.URL(url).getHost());
//
//        RuleParser parser = new RuleParser();
//        DslValidationResult validationResult = parser.validate(dsl);
//        if (!validationResult.isValid()) {
//            System.out.println("YAML DSL validation failed:");
//            for (String error : validationResult.getErrors()) {
//                System.out.println(" - " + error);
//            }
//            return;
//        }
        HtmlSummarizer.SummaryResult summaryResult = new HtmlSummarizer().summarize(html);
        Map<String, String> summaryJson = new LinkedHashMap<String, String>();
        summaryJson.put("url", url);
        summaryJson.put("tree", summaryResult.getCustomTree());
        String jsonString = new ObjectMapper().writeValueAsString(summaryJson);
        System.out.println(jsonString);

        List<SectionResult> like = new TV(html, url).like();
        ObjectMapper om = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        System.out.println("YAML DSL is valid. Extracted result:");
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
}
