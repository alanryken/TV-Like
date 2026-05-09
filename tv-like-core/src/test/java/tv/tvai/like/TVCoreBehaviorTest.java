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
    public void shouldReuseRulesAcrossRepeatedNodesAndKeepDomOrder() {
        String html = "<html><body>"
                + "<div class='category'><h2>动作</h2></div>"
                + "<div class='category-content'><a href='/detail/1'>片名1</a></div>"
                + "<div class='category'><h2>喜剧</h2></div>"
                + "<div class='category-content'><a href='/detail/2'>片名2</a></div>"
                + "<div class='category-content'><a href='/detail/3'>片名3</a></div>"
                + "<div class='category-content'><a href='/detail/4'>片名4</a></div>"
                + "<div class='category'><h2>科幻</h2></div>"
                + "<div class='category'><h2>纪录片</h2></div>"
                + "<script type='application/yaml' name='tv-like'>"
                + "version: 1\n"
                + "sections:\n"
                + "  - name: category-content\n"
                + "    selector: .category-content\n"
                + "    fields:\n"
                + "      text:\n"
                + "        selector: a\n"
                + "        transforms: [trim]\n"
                + "      link:\n"
                + "        selector: a\n"
                + "        attr: href\n"
                + "        transforms: [abs-url]\n"
                + "  - name: category\n"
                + "    selector: .category\n"
                + "    fields:\n"
                + "      text:\n"
                + "        selector: h2\n"
                + "        transforms: [trim]\n"
                + "</script>"
                + "</body></html>";

        List<SectionResult> result = new TV(html, "https://example.com/index").like();

        Assert.assertEquals(8, result.size());
        Assert.assertEquals("category", result.get(0).getSection());
        Assert.assertEquals("动作", result.get(0).getText().getValue());
        Assert.assertEquals("category-content", result.get(1).getSection());
        Assert.assertEquals("片名1", result.get(1).getText().getValue());
        Assert.assertEquals("https://example.com/detail/1", result.get(1).getLink().getValue());
        Assert.assertEquals("category", result.get(2).getSection());
        Assert.assertEquals("喜剧", result.get(2).getText().getValue());
        Assert.assertEquals("category-content", result.get(3).getSection());
        Assert.assertEquals("片名2", result.get(3).getText().getValue());
        Assert.assertEquals("category-content", result.get(4).getSection());
        Assert.assertEquals("片名3", result.get(4).getText().getValue());
        Assert.assertEquals("category-content", result.get(5).getSection());
        Assert.assertEquals("片名4", result.get(5).getText().getValue());
        Assert.assertEquals("category", result.get(6).getSection());
        Assert.assertEquals("科幻", result.get(6).getText().getValue());
        Assert.assertEquals("category", result.get(7).getSection());
        Assert.assertEquals("纪录片", result.get(7).getText().getValue());
    }

    @Test
    public void shouldInlineItemOnlySectionWhenNestedSectionsNeedDomOrder() {
        String html = "<html><body>"
                + "<div class='container'>"
                + "<div class='nav'><a href='/movie'>电影</a></div>"
                + "<ul class='latest'><li><a href='/detail/1'>片名1</a></li></ul>"
                + "<div class='nav'><a href='/tv'>电视剧</a></div>"
                + "<ul class='latest'><li><a href='/detail/2'>片名2</a></li></ul>"
                + "</div>"
                + "<script type='application/yaml' name='tv-like'>"
                + "version: 1\n"
                + "sections:\n"
                + "  - name: latest\n"
                + "    selector: .container > ul.latest\n"
                + "    items:\n"
                + "      selector: li\n"
                + "      fields:\n"
                + "        text:\n"
                + "          selector: a\n"
                + "          transforms: [trim]\n"
                + "        link:\n"
                + "          selector: a\n"
                + "          attr: href\n"
                + "          transforms: [abs-url]\n"
                + "  - name: nav\n"
                + "    selector: .container\n"
                + "    items:\n"
                + "      selector: .nav\n"
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

        List<SectionResult> result = new TV(html, "https://example.com/index").like();

        Assert.assertEquals(4, result.size());
        Assert.assertEquals("nav", result.get(0).getSection());
        Assert.assertEquals("电影", result.get(0).getText().getValue());
        Assert.assertEquals("latest", result.get(1).getSection());
        Assert.assertEquals("片名1", result.get(1).getItems().getList().get(0).getText().getValue());
        Assert.assertEquals("nav", result.get(2).getSection());
        Assert.assertEquals("电视剧", result.get(2).getText().getValue());
        Assert.assertEquals("latest", result.get(3).getSection());
        Assert.assertEquals("片名2", result.get(3).getItems().getList().get(0).getText().getValue());
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
