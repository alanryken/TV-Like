package tv.tvai.like;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class TVCoreBehaviorTest {

    @Test
    public void shouldPreferMostSpecificPathRuleAndApplyYamlTransforms() {
        String html = "<html><body>"
                + "<div class='card'><a class='title' href='/detail/1'>  hello world  </a><img data-src='/img/1.jpg'/></div>"
                + "<div class='card'><a class='title' href='/detail/2'>second</a><img data-src='/img/2.jpg'/></div>"
                + "<script type='application/yaml' name='tv-like'>"
                + "version: 1\n"
                + "paths:\n"
                + "  - match: /foo/**\n"
                + "    sections:\n"
                + "      - name: generic\n"
                + "        selector: .card\n"
                + "        meta:\n"
                + "          badge: generic\n"
                + "        fields:\n"
                + "          text:\n"
                + "            selector: .title\n"
                + "            transforms: [trim]\n"
                + "  - match: /foo/bar/**\n"
                + "    sections:\n"
                + "      - name: hero\n"
                + "        selector: .card\n"
                + "        limit: 1\n"
                + "        meta:\n"
                + "          badge: featured\n"
                + "        fields:\n"
                + "          text:\n"
                + "            selector: .title\n"
                + "            transforms: [trim, upper]\n"
                + "          link:\n"
                + "            selector: .title\n"
                + "            attr: href\n"
                + "            transforms: [abs-url]\n"
                + "          img:\n"
                + "            selector: img\n"
                + "            attr: data-src\n"
                + "            transforms: [abs-url]\n"
                + "</script>"
                + "</body></html>";

        List<Map<String, Object>> result = new TV(html, "https://example.com/foo/bar/index.html").like();

        Assert.assertEquals(1, result.size());
        Assert.assertEquals("hero", result.get(0).get("section"));
        Assert.assertEquals("featured", result.get(0).get("badge"));

        Map<String, Object> text = castMap(result.get(0).get("text"));
        Map<String, Object> link = castMap(result.get(0).get("link"));
        Map<String, Object> img = castMap(result.get(0).get("img"));

        Assert.assertEquals("HELLO WORLD", text.get("value"));
        Assert.assertEquals("https://example.com/detail/1", link.get("value"));
        Assert.assertEquals("https://example.com/img/1.jpg", img.get("value"));
    }

    @Test
    public void shouldKeepItemMetaAndIgnoreInvalidSelectorsWithYamlDsl() {
        String html = "<html><body>"
                + "<ul class='list'>"
                + "<li><a href='/a1'> one </a></li>"
                + "<li><a href='/a2'> two </a></li>"
                + "<li><a href='/a3'> three </a></li>"
                + "</ul>"
                + "<script type='application/yaml' name='tv-like'>"
                + "version: 1\n"
                + "sections:\n"
                + "  - name: list\n"
                + "    selector: .list\n"
                + "    fields:\n"
                + "      text:\n"
                + "        selector: \"[\"\n"
                + "    items:\n"
                + "      selector: li\n"
                + "      limit: 2\n"
                + "      meta:\n"
                + "        img-ratio: 16/9\n"
                + "      fields:\n"
                + "        text:\n"
                + "          selector: a\n"
                + "          transforms: [trim]\n"
                + "        link:\n"
                + "          selector: a\n"
                + "          attr: href\n"
                + "          transforms: [abs-url]\n"
                + "</script>"
                + "</body></html>";

        List<Map<String, Object>> result = new TV(html, "https://example.com/list").like();

        Assert.assertEquals(1, result.size());
        Map<String, Object> items = castMap(result.get(0).get("items"));
        List<Map<String, Object>> values = castList(items.get("value"));

        Assert.assertEquals(2, values.size());
        Assert.assertEquals("16/9", items.get("img-ratio"));
        Assert.assertEquals("one", castMap(values.get(0).get("text")).get("value"));
        Assert.assertEquals("https://example.com/a1", castMap(values.get(0).get("link")).get("value"));
        Assert.assertEquals("two", castMap(values.get(1).get("text")).get("value"));
    }

    @Test
    public void shouldSupportYamlSelectorStringsWithAttributeSelectors() {
        String html = "<html><body>"
                + "<div class='hero'><a href='/play/1'> Play Now </a><img data-src='/cover.jpg' /></div>"
                + "<script type='application/yaml' id='tv-like-rules'>"
                + "version: 1\n"
                + "sections:\n"
                + "  - name: hero\n"
                + "    selector: .hero\n"
                + "    fields:\n"
                + "      text:\n"
                + "        selector: a[href]\n"
                + "        transforms: [trim]\n"
                + "      link:\n"
                + "        selector: a[href]\n"
                + "        attr: href\n"
                + "        transforms: [abs-url]\n"
                + "      img:\n"
                + "        selector: img[data-src]\n"
                + "        attr: data-src\n"
                + "        transforms: [abs-url]\n"
                + "</script>"
                + "</body></html>";

        List<Map<String, Object>> result = new TV(html, "https://example.com/detail").like();

        Assert.assertEquals(1, result.size());
        Assert.assertEquals("Play Now", castMap(result.get(0).get("text")).get("value"));
        Assert.assertEquals("https://example.com/play/1", castMap(result.get(0).get("link")).get("value"));
        Assert.assertEquals("https://example.com/cover.jpg", castMap(result.get(0).get("img")).get("value"));
    }

    @Test
    public void shouldValidateYamlDslAndReportReadableErrors() {
        String dsl = ""
                + "version: 1\n"
                + "paths:\n"
                + "  - match: /detail/**\n"
                + "    sections:\n"
                + "      - name: hero\n"
                + "        limit: nope\n"
                + "        fields:\n"
                + "          text:\n"
                + "            transforms: [trim, unknown-transform]\n";

        RuleParser parser = new RuleParser();
        DslValidationResult validationResult = parser.validate(dsl);

        Assert.assertFalse(validationResult.isValid());
        Assert.assertTrue(containsMessage(validationResult.getErrors(), "sections[0].selector"));
        Assert.assertTrue(containsMessage(validationResult.getErrors(), "limit"));
        Assert.assertTrue(containsMessage(validationResult.getErrors(), "unknown-transform"));
    }

    private static boolean containsMessage(List<String> messages, String keyword) {
        if (messages == null || keyword == null) {
            return false;
        }
        for (String message : messages) {
            if (message != null && message.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castList(Object value) {
        return (List<Map<String, Object>>) value;
    }
}
