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
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;

public class TVLikeDSL {

    private static final int CONNECT_TIMEOUT_MILLIS = 10_000;
    private static final int READ_TIMEOUT_MILLIS = 30_000;
    private static final String DEFAULT_USER_AGENT = "Mozilla/5.0";
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
        if (doc == null) return "";
        Element script = doc.selectFirst("script[type=text/plain][name=tv-like]");
        if (script == null) return "";

        return script.html()
                .replace("&#10;", "\n")
                .replace("&#13;", "\r\n")
                .replace("\r\n", "\n")
                .replaceAll("(?s)/\\*.*?\\*/", "")
                .trim();
    }

    private String getHubDsl(String host) {
        if (StringUtils.isBlank(host)) {
            return null;
        }
        String normalizedHost = normalizeHost(host);
        if (StringUtils.isBlank(normalizedHost)) {
            return null;
        }
        Set<String> candidates = new LinkedHashSet<String>();
        candidates.add(normalizedHost);
        if (normalizedHost.startsWith("www.")) {
            candidates.add(normalizedHost.substring("www.".length()));
        }
        String domainPath = domainToDslPath(normalizedHost);
        if (StringUtils.isNotBlank(domainPath)) {
            candidates.add(domainPath);
            if (domainPath.endsWith("/www")) {
                candidates.add(domainPath.substring(0, domainPath.length() - "/www".length()));
            }
        }
        for (String l : candidates) {
            String reqUrl = tv_like_dsl_hub + l + ".dsl";
            String s = get(reqUrl);
            if (StringUtils.isNotBlank(s)) {
                return s.trim();
            }
        }
        return null;
    }

    public static String get(String urlStr) {
        if (StringUtils.isBlank(urlStr)) {
            return "";
        }
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
            conn.setReadTimeout(READ_TIMEOUT_MILLIS);
            conn.setRequestProperty("User-Agent", DEFAULT_USER_AGENT);
            conn.setRequestProperty("Accept", "application/json");

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                return readResponseBody(conn);
            }
        } catch (Exception e) {
            return "";
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return "";
    }

    public static String postJson(String urlStr, String jsonBody) {
        if (StringUtils.isBlank(urlStr)) {
            return "";
        }
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
            conn.setReadTimeout(READ_TIMEOUT_MILLIS);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("User-Agent", DEFAULT_USER_AGENT);
            conn.setRequestProperty("Accept", "application/json");

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = (jsonBody == null ? "" : jsonBody).getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                return readResponseBody(conn);
            }
        } catch (Exception e) {
            return "";
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return "";
    }

    public static String domainToDslPath(String host) {
        if (StringUtils.isBlank(host)) {
            return null;
        }

        host = normalizeHost(host);
        if (StringUtils.isBlank(host)) {
            return null;
        }
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

    private static String normalizeHost(String host) {
        if (StringUtils.isBlank(host)) {
            return "";
        }
        String normalizedHost = host.split(":")[0].trim().toLowerCase();
        while (normalizedHost.endsWith(".")) {
            normalizedHost = normalizedHost.substring(0, normalizedHost.length() - 1);
        }
        return normalizedHost;
    }

    private static String readResponseBody(HttpURLConnection conn) throws IOException {
        if (conn == null) {
            return "";
        }
        InputStreamReader reader = null;
        if (conn.getResponseCode() >= 200 && conn.getResponseCode() < 300) {
            reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8);
        } else if (conn.getErrorStream() != null) {
            reader = new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8);
        }
        if (reader == null) {
            return "";
        }
        try (BufferedReader br = new BufferedReader(reader)) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        }
    }
}
