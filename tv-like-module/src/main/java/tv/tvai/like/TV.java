package tv.tvai.like;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class TV {

    private final Document doc;
    private final String dslHub;
    private final String path;

    public TV(String html, String url) throws MalformedURLException {
        this.doc = Jsoup.parse(html);
        this.path = new URL(url).getPath();
        this.dslHub = null;
    }

    public TV(String html, String url, String dslHub) throws MalformedURLException {
        this.doc = Jsoup.parse(html);
        this.path = new URL(url).getPath();
        this.dslHub = dslHub;
    }


    public List<Map<String, Object>> like() {

        TVLikeDSL tvLikeDSL = dslHub == null ? new TVLikeDSL() : new TVLikeDSL(dslHub);

        String dsl = tvLikeDSL.getDSL(doc);

        RuleParser parser = new RuleParser();
        parser.parse(dsl);
        List<RuleNode> rules = parser.getPathRule(path);
        Extractor extractor = new Extractor();
        return extractor.extract(doc, rules);

    }


}
