package tv.tvai.like;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TVLikeYamlTest {

    private static final String EXTRACT_URL = "http://119.29.52.83:8085/tv-like-hub/ai/html-rule/extract";

    public static void main(String[] args) throws IOException {
        String html = FileToString.get("libvio.lat.index.html");
        String url = "https://www.libvio.lat/";

        //简化 压缩 html
        HtmlSummarizer.SummaryResult summaryResult = new HtmlSummarizer().summarize(html);

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, String> requestBody = new LinkedHashMap<>();
        requestBody.put("url", url);
        requestBody.put("html", summaryResult.getFoldedHtml());
        //请求 sse
        String responseBody = TVLikeDSL.postJson(EXTRACT_URL, objectMapper.writeValueAsString(requestBody));
        if (responseBody == null || responseBody.trim().isEmpty()) {
            throw new IllegalStateException("提取 yaml 接口返回为空");
        }
        //获取yaml
        JsonNode responseJson = objectMapper.readTree(responseBody);
        JsonNode yamlNode = responseJson.get("yaml");
        String dsl = yamlNode == null ? "" : yamlNode.asText();
        if (dsl == null || dsl.trim().isEmpty()) {
            throw new IllegalStateException("提取 yaml 接口未返回有效 yaml");
        }

        // like
        List<SectionResult> like = new TV(html, url, null, dsl).like();
        ObjectMapper prettyPrinter = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        System.out.println(prettyPrinter.writeValueAsString(like));
    }
}
