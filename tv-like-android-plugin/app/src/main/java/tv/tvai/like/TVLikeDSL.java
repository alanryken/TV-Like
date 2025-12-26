package tv.tvai.like;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import tv.tvai.like.util.StringUtils;


public class TVLikeDSL {

    private String tv_like_dsl_hub = "https://hub.tvai.tv/";

    public TVLikeDSL() {

    }

    public TVLikeDSL(String dslHub) {
        if (StringUtils.isNotBlank(dslHub)) {
            tv_like_dsl_hub = dslHub.endsWith("/") ? dslHub : dslHub + "/";
        }
    }

    public String getDSL(Document doc, String host) {
        String fromScript = extractRulesFromScript(doc);
        if (StringUtils.isNotBlank(fromScript)) {
            return fromScript;
        }
        return getHubDsl(host);
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


    private String getHubDsl(String host) {
        //行程多种请求地址
        List<String> list = new ArrayList<>();
        // 1 使用标准方式获取
        list.add(host);
        // 2 如果开头是www 则去掉
        if (host.startsWith("www.")) {
            list.add(host.substring("www.".length()));
        }
        // 3 domain to path
        String domainPath = domainToDslPath(host);
        list.add(domainPath);
        // 4 如果结尾是www 则去掉
        if (domainPath.endsWith("/www")) {
            list.add(domainPath.substring(0, domainPath.length() - "/www".length()));
        }
        // 获取
        for (String l : list) {
            String reqUrl = tv_like_dsl_hub + l + ".dsl";
            String s = get(reqUrl);
            if (StringUtils.isNotBlank(s)) {
                return s;
            }
        }
        return null;
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
                        new InputStreamReader(conn.getInputStream()))) {
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
                        new InputStreamReader(conn.getErrorStream()))) {
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
                byte[] input = jsonBody.getBytes();
                os.write(input, 0, input.length);
            }

            // 读取响应（复用上面的读取逻辑）
            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()))) {
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

    public static String domainToDslPath(String host) {
        if (host == null || host.isEmpty()) {
            return null;
        }

        // 移除端口：比如 "m.douban.com:8080"
        host = host.split(":")[0].trim();

        // 按 . 分割
        String[] parts = host.split("\\.");

        if (parts.length == 1) {
            // 例如 localhost
            return parts[0];
        }

        // 反转域名：m.douban.com -> com/douban/m
        StringBuilder sb = new StringBuilder();

        for (int i = parts.length - 1; i >= 0; i--) {
            sb.append(parts[i]);
            if (i != 0) sb.append("/");
        }

        return sb.toString();
    }

}
