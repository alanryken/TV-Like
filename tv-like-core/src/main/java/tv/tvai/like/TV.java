package tv.tvai.like;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import tv.tvai.like.util.StringUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TV {

    private final String html;
    private final String dslHub;
    private final String url;

    public TV(String html, String url) {
        this.html = html;
        this.url = url;
        this.dslHub = null;
    }

    public TV(String html, String url, String dslHub) {
        this.html = html;
        this.url = url;
        this.dslHub = dslHub;
    }

    public List<Map<String, Object>> like() {
        List<Map<String, Object>> emptyResult = new ArrayList<>();
        if (StringUtils.isBlank(html) || StringUtils.isBlank(url)) {
            return emptyResult;
        }
        try {
            URL u = new URL(url);
            Document doc = Jsoup.parse(html, u.toExternalForm());
            TVLikeDSL tvLikeDSL = dslHub == null ? new TVLikeDSL() : new TVLikeDSL(dslHub);

            String path = u.getPath();
            String host = u.getHost();

            String dsl = tvLikeDSL.getDSL(doc, host);
            if (StringUtils.isBlank(dsl)) return emptyResult;
            RuleParser parser = new RuleParser();
            parser.parse(dsl);
            List<RuleNode> rules = parser.getPathRule(StringUtils.isBlank(path) ? "/" : path);
            if (rules.isEmpty()) {
                return emptyResult;
            }
            Extractor extractor = new Extractor();
            return extractor.extract(doc, rules);
        } catch (Exception e) {
            return emptyResult;
        }
    }
}
