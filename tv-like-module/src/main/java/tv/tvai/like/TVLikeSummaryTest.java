package tv.tvai.like;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TVLikeSummaryTest {

    public static void main(String[] args) throws IOException {
        String html = FileToString.get("sszzyy.com/index.php/vod/type/id/20.html");
        String url = "https://sszzyy.com/index.php/vod/type/id/20.html";
        HtmlSummarizer.SummaryResult summaryResult = new HtmlSummarizer().summarize(html);
        Map<String, String> summaryJson = new LinkedHashMap<String, String>();
        summaryJson.put("url", url);
        summaryJson.put("html", summaryResult.getFoldedHtml());
        String jsonString = new ObjectMapper().writeValueAsString(summaryJson);
        System.out.println(jsonString);
    }
}
