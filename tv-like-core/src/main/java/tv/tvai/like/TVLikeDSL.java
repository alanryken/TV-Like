package tv.tvai.like;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import tv.tvai.like.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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

    private static String extractRulesFromScript(Document doc) {
        Element script = doc.selectFirst("script[type=text/plain][name=tv-like]");
        if (script == null) return "";

        return script.html()
                .replace("&#10;", "\n")
                .replace("&#13;", "\r\n")
                .replace("\r\n", "\n")
                .replaceAll("/\\*.*?\\*/", "");
    }

    private String getHubDsl(String host) {
        List<String> list = new ArrayList<>();
        list.add(host);
        if (host.startsWith("www.")) {
            list.add(host.substring("www.".length()));
        }
        String domainPath = domainToDslPath(host);
        list.add(domainPath);
        if (domainPath.endsWith("/www")) {
            list.add(domainPath.substring(0, domainPath.length() - "/www".length()));
        }
        for (String l : list) {
            String reqUrl = tv_like_dsl_hub + l + ".dsl";
            String s = get(reqUrl);
            if (StringUtils.isNotBlank(s)) {
                return s;
            }
        }
        return null;
    }

    public static String get(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(30_000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setRequestProperty("Accept", "application/json");

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    return sb.toString();
                }
            } else {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream(), "UTF-8"))) {
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

    public static String postJson(String urlStr, String jsonBody) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(30_000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setRequestProperty("Accept", "application/json");

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.getBytes("UTF-8");
                os.write(input, 0, input.length);
            }

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
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

        host = host.split(":")[0].trim();
        String[] parts = host.split("\\.");

        if (parts.length == 1) {
            return parts[0];
        }

        StringBuilder sb = new StringBuilder();

        for (int i = parts.length - 1; i >= 0; i--) {
            sb.append(parts[i]);
            if (i != 0) sb.append("/");
        }

        return sb.toString();
    }
}
