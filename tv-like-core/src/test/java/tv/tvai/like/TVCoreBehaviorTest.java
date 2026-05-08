package tv.tvai.like;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

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

        List<SectionResult> result = new TV(html, "https://example.com/foo/bar/index.html").like();

        Assert.assertEquals(1, result.size());
        Assert.assertEquals("hero", result.get(0).getSection());
        Assert.assertEquals("featured", result.get(0).getMeta().get("badge"));
        Assert.assertEquals("HELLO WORLD", result.get(0).getText().getValue());
        Assert.assertEquals("https://example.com/detail/1", result.get(0).getLink().getValue());
        Assert.assertEquals("https://example.com/img/1.jpg", result.get(0).getImg().getValue());
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

        List<SectionResult> result = new TV(html, "https://example.com/list").like();

        Assert.assertEquals(1, result.size());
        ItemsResult items = result.get(0).getItems();

        Assert.assertEquals(2, items.getList().size());
        Assert.assertEquals("16/9", items.getMeta().get("img-ratio"));
        Assert.assertEquals("one", items.getList().get(0).getText().getValue());
        Assert.assertEquals("https://example.com/a1", items.getList().get(0).getLink().getValue());
        Assert.assertEquals("two", items.getList().get(1).getText().getValue());
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

        List<SectionResult> result = new TV(html, "https://example.com/detail").like();

        Assert.assertEquals(1, result.size());
        Assert.assertEquals("Play Now", result.get(0).getText().getValue());
        Assert.assertEquals("https://example.com/play/1", result.get(0).getLink().getValue());
        Assert.assertEquals("https://example.com/cover.jpg", result.get(0).getImg().getValue());
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
}
