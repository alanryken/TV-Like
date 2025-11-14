package tv.tvai.like;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.InputStream;
import java.util.Map;

public class PageExtractor {
    public static void main(String[] args) throws Exception {
        // 模拟 HTML with meta
        String html = "<html><head>" +
                "<meta name=\"tv-like\" content=\"page-type: video-detail&#10;" +
                "section: info .info-container {&#10;" +
                "  text: .title [transform: trim]&#10;" +
                "  img: img.cover [attr: src|data-src]&#10;" +
                "  link: null&#10;" +
                "}&#10;" +
                "section: episodes .episode-list {&#10;" +
                "  text: null&#10;" +
                "  img: null&#10;" +
                "  link: null&#10;" +
                "  items: .ep-item {&#10;" +
                "    text: .name&#10;" +
                "    img: null&#10;" +
                "    link: a [limit: 2]&#10;" +
                "  }&#10;" +
                "}\"> " +
                "</head><body>" +
                "<div class=\"info-container\"><h1 class=\"title\">标题1</h1><img class=\"cover\" data-src=\"/cover1.jpg\"></div>" +
                "<div class=\"episode-list\">" +
                "<div class=\"ep-item\"><span class=\"name\">集1</span><a href=\"/ep1\">链接</a></div>" +
                "<div class=\"ep-item\"><span class=\"name\">集2</span><a href=\"/ep2\">链接</a></div>" +
                "</div>" +
                "</body></html>";

        try (InputStream inputStream = PageExtractor.class.getClassLoader().getResourceAsStream("mtyy1_com_index.html")) {

            Document doc = Jsoup.parse(inputStream, "UTF-8", "");
            Extractor extractor = new Extractor();
            String baseUrl = "https://example.com";
            Map<String, Object> jsonData = extractor.extractOrdered(doc, baseUrl);

            ObjectMapper mapper = new ObjectMapper();
            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonData));

        }
    }
}