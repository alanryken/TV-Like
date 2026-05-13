package tv.tvai.like;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import tv.tvai.like.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HtmlSummarizer {

    private static final Set<String> NOISE_TAGS = new LinkedHashSet<String>(Arrays.asList(
            "script", "style", "noscript", "template", "iframe", "svg", "canvas"
    ));
    private static final Set<String> KEPT_ATTRIBUTE_NAMES = new LinkedHashSet<String>(Arrays.asList(
            "id", "class", "href", "src", "data-src", "data-original", "title", "alt", "role"
    ));
    private static final String REPEAT_ATTRIBUTE = "data-tv-like-repeat";
    private static final String FOLDED_ATTRIBUTE = "data-tv-like-folded";
    private static final int MAX_TEXT_LENGTH = 80;
    private static final int FINGERPRINT_DEPTH = 2;

    public SummaryResult summarize(String html) {
        if (StringUtils.isBlank(html)) {
            return new SummaryResult("", "", "{}", "");
        }

        Document cleanedDocument = Jsoup.parse(html);
        cleanedDocument.outputSettings().prettyPrint(false);
        sanitize(cleanedDocument);
        String cleanedHtml = extractFragment(cleanedDocument);

        Document foldedDocument = cleanedDocument.clone();
        foldRepeatedBlocks(foldedDocument.body());
        String foldedHtml = extractFragment(foldedDocument);

        StructureNode rootNode = buildStructureNode(foldedDocument.body());
        String title = foldedDocument.title();
        String customJson = toCustomJson(title, rootNode);
        String customTree = toCustomTree(title, rootNode);
        return new SummaryResult(cleanedHtml, foldedHtml, customJson, customTree);
    }

    private void sanitize(Document document) {
        removeComments(document);
        removeNoiseElements(document);
        normalizeTextNodes(document);
        trimAttributes(document);
    }

    private void removeComments(Node node) {
        List<Node> children = new ArrayList<Node>(node.childNodes());
        for (Node child : children) {
            if (child instanceof Comment) {
                child.remove();
                continue;
            }
            removeComments(child);
        }
    }

    private void removeNoiseElements(Document document) {
        for (String tag : NOISE_TAGS) {
            for (Element element : new ArrayList<Element>(document.select(tag))) {
                element.remove();
            }
        }
        List<Element> elements = new ArrayList<Element>(document.getAllElements());
        for (Element element : elements) {
            if (element == document || element == document.body() || element == document.head()) {
                continue;
            }
            if (isHiddenElement(element)) {
                element.remove();
            }
        }
    }

    private boolean isHiddenElement(Element element) {
        if (element == null) {
            return false;
        }
        if (element.hasAttr("hidden")) {
            return true;
        }
        String style = normalizeText(element.attr("style"));
        return style.contains("display:none") || style.contains("visibility:hidden");
    }

    private void normalizeTextNodes(Node node) {
        List<Node> children = new ArrayList<Node>(node.childNodes());
        for (Node child : children) {
            if (child instanceof TextNode) {
                TextNode textNode = (TextNode) child;
                String normalized = normalizeText(textNode.getWholeText());
                if (StringUtils.isBlank(normalized)) {
                    textNode.remove();
                } else {
                    textNode.text(normalized);
                }
                continue;
            }
            normalizeTextNodes(child);
        }
    }

    private void trimAttributes(Document document) {
        List<Element> elements = new ArrayList<Element>(document.getAllElements());
        for (Element element : elements) {
            List<Attribute> attributes = new ArrayList<Attribute>(element.attributes().asList());
            for (Attribute attribute : attributes) {
                if (!shouldKeepAttribute(attribute.getKey())) {
                    element.removeAttr(attribute.getKey());
                }
            }
            normalizeClassAttribute(element);
        }
    }

    private boolean shouldKeepAttribute(String key) {
        if (StringUtils.isBlank(key)) {
            return false;
        }
        String normalizedKey = key.trim().toLowerCase();
        return KEPT_ATTRIBUTE_NAMES.contains(normalizedKey)
                || normalizedKey.startsWith("aria-")
                || normalizedKey.equals(REPEAT_ATTRIBUTE)
                || normalizedKey.equals(FOLDED_ATTRIBUTE);
    }

    private void normalizeClassAttribute(Element element) {
        if (element == null || !element.hasAttr("class")) {
            return;
        }
        Set<String> classes = new LinkedHashSet<String>();
        for (String className : element.classNames()) {
            String normalized = className == null ? "" : className.trim();
            if (!normalized.isEmpty()) {
                classes.add(normalized);
            }
        }
        if (classes.isEmpty()) {
            element.removeAttr("class");
            return;
        }
        StringBuilder builder = new StringBuilder();
        int index = 0;
        for (String className : classes) {
            if (index > 0) {
                builder.append(" ");
            }
            builder.append(className);
            index++;
        }
        element.attr("class", builder.toString());
    }

    private void foldRepeatedBlocks(Element parent) {
        if (parent == null) {
            return;
        }
        List<Element> originalChildren = new ArrayList<Element>(parent.children());
        for (Element child : originalChildren) {
            foldRepeatedBlocks(child);
        }

        int index = 0;
        while (index < parent.childrenSize()) {
            List<Element> children = new ArrayList<Element>(parent.children());
            if (index >= children.size()) {
                break;
            }
            Element current = children.get(index);
            String fingerprint = buildFingerprint(current, FINGERPRINT_DEPTH);
            int end = index + 1;
            while (end < children.size()) {
                Element candidate = children.get(end);
                if (!fingerprint.equals(buildFingerprint(candidate, FINGERPRINT_DEPTH))) {
                    break;
                }
                end++;
            }
            int repeatCount = end - index;
            if (repeatCount > 1) {
                current.attr(REPEAT_ATTRIBUTE, String.valueOf(repeatCount));
                current.attr(FOLDED_ATTRIBUTE, "true");
                for (int i = index + 1; i < end; i++) {
                    children.get(i).remove();
                }
            }
            index++;
        }
    }

    private String buildFingerprint(Element element, int depth) {
        StringBuilder builder = new StringBuilder();
        builder.append(element.tagName());
        appendClassFingerprint(element, builder);
        appendAttributeFingerprint(element, builder);
        builder.append("|text=").append(textShape(element.ownText()));
        if (depth <= 0) {
            builder.append("|children=").append(element.childrenSize());
            return builder.toString();
        }
        for (Element child : element.children()) {
            builder.append('[').append(buildFingerprint(child, depth - 1)).append(']');
        }
        return builder.toString();
    }

    private void appendClassFingerprint(Element element, StringBuilder builder) {
        builder.append("|class=");
        if (element.classNames().isEmpty()) {
            builder.append("-");
            return;
        }
        int index = 0;
        for (String className : element.classNames()) {
            if (index > 0) {
                builder.append('.');
            }
            builder.append(className);
            index++;
        }
    }

    private void appendAttributeFingerprint(Element element, StringBuilder builder) {
        builder.append("|attrs=");
        List<String> attributeKeys = new ArrayList<String>();
        for (Attribute attribute : element.attributes()) {
            String key = attribute.getKey();
            if ("class".equals(key) || "id".equals(key) || REPEAT_ATTRIBUTE.equals(key) || FOLDED_ATTRIBUTE.equals(key)) {
                continue;
            }
            if (shouldKeepAttribute(key)) {
                attributeKeys.add(key);
            }
        }
        if (attributeKeys.isEmpty()) {
            builder.append("-");
            return;
        }
        for (int i = 0; i < attributeKeys.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(attributeKeys.get(i));
        }
    }

    private String textShape(String text) {
        String normalized = normalizeText(text);
        if (normalized.isEmpty()) {
            return "none";
        }
        return normalized.length() <= 12 ? "short" : "long";
    }

    private StructureNode buildStructureNode(Element element) {
        if (element == null) {
            return null;
        }
        StructureNode node = new StructureNode();
        node.tag = element.tagName();
        if (element.hasAttr("id")) {
            node.id = element.attr("id");
        }
        node.classes.addAll(element.classNames());
        for (Attribute attribute : element.attributes()) {
            String key = attribute.getKey();
            if ("id".equals(key) || "class".equals(key) || REPEAT_ATTRIBUTE.equals(key) || FOLDED_ATTRIBUTE.equals(key)) {
                continue;
            }
            if (shouldKeepAttribute(key) && StringUtils.isNotBlank(attribute.getValue())) {
                node.attributes.put(key, attribute.getValue());
            }
        }
        if (element.hasAttr(REPEAT_ATTRIBUTE)) {
            try {
                node.repeat = Integer.parseInt(element.attr(REPEAT_ATTRIBUTE));
            } catch (NumberFormatException e) {
                node.repeat = 1;
            }
        }
        String ownText = truncate(normalizeText(element.ownText()), MAX_TEXT_LENGTH);
        if (StringUtils.isNotBlank(ownText)) {
            node.text = ownText;
        }
        for (Element child : element.children()) {
            StructureNode childNode = buildStructureNode(child);
            if (childNode != null) {
                node.children.add(childNode);
            }
        }
        return node;
    }

    private String toCustomJson(String title, StructureNode rootNode) {
        Map<String, Object> root = new LinkedHashMap<String, Object>();
        root.put("title", title == null ? "" : title);
        root.put("root", rootNode == null ? null : rootNode.toMap());
        StringBuilder builder = new StringBuilder();
        appendJsonValue(builder, root);
        return builder.toString();
    }

    private void appendJsonValue(StringBuilder builder, Object value) {
        if (value == null) {
            builder.append("null");
            return;
        }
        if (value instanceof String) {
            builder.append('"').append(escapeJson((String) value)).append('"');
            return;
        }
        if (value instanceof Number || value instanceof Boolean) {
            builder.append(String.valueOf(value));
            return;
        }
        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            builder.append('{');
            int index = 0;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (index > 0) {
                    builder.append(',');
                }
                builder.append('"').append(escapeJson(entry.getKey())).append('"').append(':');
                appendJsonValue(builder, entry.getValue());
                index++;
            }
            builder.append('}');
            return;
        }
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            builder.append('[');
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    builder.append(',');
                }
                appendJsonValue(builder, list.get(i));
            }
            builder.append(']');
            return;
        }
        builder.append('"').append(escapeJson(String.valueOf(value))).append('"');
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            switch (current) {
                case '\\':
                    builder.append("\\\\");
                    break;
                case '"':
                    builder.append("\\\"");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                default:
                    builder.append(current);
                    break;
            }
        }
        return builder.toString();
    }

    private String toCustomTree(String title, StructureNode rootNode) {
        StringBuilder builder = new StringBuilder();
        builder.append("document");
        if (StringUtils.isNotBlank(title)) {
            builder.append(" title=\"").append(title).append("\"");
        }
        builder.append("\n");
        appendTreeNode(builder, rootNode, 1);
        return builder.toString().trim();
    }

    private void appendTreeNode(StringBuilder builder, StructureNode node, int depth) {
        if (node == null) {
            return;
        }
        for (int i = 0; i < depth; i++) {
            builder.append("  ");
        }
        builder.append(node.tag);
        if (StringUtils.isNotBlank(node.id)) {
            builder.append("#").append(node.id);
        }
        for (String className : node.classes) {
            builder.append(".").append(className);
        }
        if (node.repeat > 1) {
            builder.append(" repeat=").append(node.repeat);
        }
        if (!node.attributes.isEmpty()) {
            builder.append(" attrs=").append(node.attributes.keySet());
        }
        if (StringUtils.isNotBlank(node.text)) {
            builder.append(" text=\"").append(node.text).append("\"");
        }
        builder.append("\n");
        for (StructureNode child : node.children) {
            appendTreeNode(builder, child, depth + 1);
        }
    }

    private String extractFragment(Document document) {
        if (document == null) {
            return "";
        }
        Element body = document.body();
        return body == null ? document.outerHtml() : body.html().trim();
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value == null ? "" : value;
        }
        return value.substring(0, maxLength) + "...";
    }

    public static final class SummaryResult {
        private final String cleanedHtml;
        private final String foldedHtml;
        private final String customJson;
        private final String customTree;

        public SummaryResult(String cleanedHtml, String foldedHtml, String customJson, String customTree) {
            this.cleanedHtml = cleanedHtml;
            this.foldedHtml = foldedHtml;
            this.customJson = customJson;
            this.customTree = customTree;
        }

        public String getCleanedHtml() {
            return cleanedHtml;
        }

        public String getFoldedHtml() {
            return foldedHtml;
        }

        public String getCustomJson() {
            return customJson;
        }

        public String getCustomTree() {
            return customTree;
        }
    }

    private static final class StructureNode {
        private String tag;
        private String id;
        private String text;
        private int repeat = 1;
        private final List<String> classes = new ArrayList<String>();
        private final Map<String, String> attributes = new LinkedHashMap<String, String>();
        private final List<StructureNode> children = new ArrayList<StructureNode>();

        private Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            map.put("tag", tag);
            if (StringUtils.isNotBlank(id)) {
                map.put("id", id);
            }
            if (!classes.isEmpty()) {
                map.put("classes", new ArrayList<String>(classes));
            }
            if (!attributes.isEmpty()) {
                map.put("attrs", new LinkedHashMap<String, String>(attributes));
            }
            if (repeat > 1) {
                map.put("repeat", repeat);
            }
            if (StringUtils.isNotBlank(text)) {
                map.put("text", text);
            }
            List<Map<String, Object>> childMaps = new ArrayList<Map<String, Object>>();
            for (StructureNode child : children) {
                childMaps.add(child.toMap());
            }
            map.put("children", childMaps);
            return map;
        }
    }
}
