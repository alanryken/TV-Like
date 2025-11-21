package tv.tvai.like;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.URL;
import java.util.*;

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
        try {
            Document doc = Jsoup.parse(html);
            TVLikeDSL tvLikeDSL = dslHub == null ? new TVLikeDSL() : new TVLikeDSL(dslHub);

            URL u = new URL(url);
            String path = u.getPath();
            String host = u.getHost();

            String dsl = tvLikeDSL.getDSL(doc, host);
            if (dsl == null) return new ArrayList<>();
            RuleParser parser = new RuleParser();
            parser.parse(dsl);
            List<RuleNode> rules = parser.getPathRule(path);
            Extractor extractor = new Extractor();
            return extractor.extract(doc, rules);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }


}
