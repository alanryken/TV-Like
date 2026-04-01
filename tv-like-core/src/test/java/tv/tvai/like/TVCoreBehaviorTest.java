package tv.tvai.like;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class TVCoreBehaviorTest {

    @Test
    public void shouldSupportMultiPathTransformAndSectionLimit() {
        String html = "<html><body>"
                + "<div class='card'><a class='title' href='/detail/1'>  hello world  </a><img data-src='/img/1.jpg'/></div>"
                + "<div class='card'><a class='title' href='/detail/2'>second</a><img data-src='/img/2.jpg'/></div>"
                + "<script type='text/plain' name='tv-like'>"
                + "path: /foo/** || /bar/** {\n"
                + "section:hero .card {\n"
                + "text: .title [transform: trim|upper]\n"
                + "link: .title [attr: href] [transform: abs-url]\n"
                + "img: img [attr: data-src] [transform: abs-url]\n"
                + "} [limit: 1] [badge: \"featured\"]\n"
                + "}\n"
                + "</script>"
                + "</body></html>";

        List<Map<String, Object>> result = new TV(html, "https://example.com/foo/index.html").like();

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
    public void shouldKeepItemOptionsAndIgnoreInvalidSelectors() {
        String html = "<html><body>"
                + "<ul class='list'>"
                + "<li><a href='/a1'> one </a></li>"
                + "<li><a href='/a2'> two </a></li>"
                + "<li><a href='/a3'> three </a></li>"
                + "</ul>"
                + "<script type='text/plain' name='tv-like'>"
                + "section:list .list {\n"
                + "text: [\n"
                + "items: li {\n"
                + "text: a [transform: trim]\n"
                + "link: a [attr: href] [transform: abs-url]\n"
                + "} [limit: 2] [img-ratio: 16/9]\n"
                + "}\n"
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

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castList(Object value) {
        return (List<Map<String, Object>>) value;
    }
}
