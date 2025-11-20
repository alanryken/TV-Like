package tv.tvai.like.chatGPT;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import tv.tvai.like.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;


public class DSL {

    private String tv_like_dsl_hub = "";

    public DSL() {

    }

    public DSL(String dslHub) {
        if (StringUtils.isNotBlank(dslHub)) {
            tv_like_dsl_hub = dslHub;
        }
    }

    public String getDSL(Document doc) {
        String fromScript = extractRulesFromScript(doc);
        if (StringUtils.isNotBlank(fromScript)) {
            return fromScript;
        }
        return get(tv_like_dsl_hub);
    }


    /**
     * 从 <script type="text/plain" name="tv-like"> 中提取 DSL 内容
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


    // GET 请求
    public static String get(String urlStr) {
        try {

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // 基本配置
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10_000);  // 10秒连接超时
            conn.setReadTimeout(30_000);     // 30秒读取超时
            conn.setRequestProperty("User-Agent", "Mozilla/5.0"); // 有些服务器会拒绝没有UA的请求
            conn.setRequestProperty("Accept", "application/json");

            // 读取响应
            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    return sb.toString();
                }
            } else {
                // 读取错误流
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    throw new IOException("HTTP error code: " + code + "\nResponse: " + sb);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    // POST JSON 请求（最常用场景）
    public static String postJson(String urlStr, String jsonBody) {
        try {

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(30_000);
            conn.setDoOutput(true);  // 关键：允许输出
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setRequestProperty("Accept", "application/json");

            // 写入请求体
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // 读取响应（复用上面的读取逻辑）
            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    return sb.toString();
                }
            } else {
                throw new IOException("HTTP " + code + " : " + conn.getResponseMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

}
