package tv.tvai.like.chatGPT;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class TV {

    private Document doc;
    private String dsl;
    private String path;

    public TV(String html, String url) throws MalformedURLException {
        this.doc = Jsoup.parse(html);
        this.path = new URL(url).getPath();
    }

    public TV(String html, String url, String dsl) throws MalformedURLException {
        this.doc = Jsoup.parse(html);
        this.path = new URL(url).getPath();
        this.dsl = dsl;
    }

    public List<Map<String, Object>> like() {

        this.dsl = new DSL().getDSL(doc);
        RuleParser parser = new RuleParser();
        parser.parse(dsl);
        List<RuleNode> rules = parser.getPathRule(path);
        Extractor extractor = new Extractor();
        return extractor.extract(doc, rules);

    }


}
