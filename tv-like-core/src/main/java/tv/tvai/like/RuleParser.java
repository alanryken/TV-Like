package tv.tvai.like;

import org.yaml.snakeyaml.Yaml;
import tv.tvai.like.enums.OptionKeyEnum;
import tv.tvai.like.util.AntPathMatcher;
import tv.tvai.like.util.PathMatcher;
import tv.tvai.like.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RuleParser {

    private static final Set<String> ALLOWED_FIELDS = new HashSet<String>(Arrays.asList("text", "img", "link"));
    private static final Set<String> ALLOWED_TRANSFORMS = new LinkedHashSet<String>(
            Arrays.asList("trim", "upper", "upper-case", "lower", "lower-case", "digits", "abs-url")
    );
    private static final String DEFAULT_PATH_PATTERN = "/**";

    private final PathMatcher pathMatcher = new AntPathMatcher();
    private final Yaml yaml = new Yaml();
    private Map<String, List<RuleNode>> pathRuleMap = new LinkedHashMap<String, List<RuleNode>>();

    public List<RuleNode> getPathRule(String path) {
        if (pathRuleMap == null || pathRuleMap.isEmpty()) {
            return new ArrayList<RuleNode>();
        }
        String normalizedPath = normalizePath(path);
        List<Map.Entry<String, List<RuleNode>>> matchedEntries = new ArrayList<Map.Entry<String, List<RuleNode>>>();
        for (Map.Entry<String, List<RuleNode>> entry : pathRuleMap.entrySet()) {
            if (pathMatcher.match(entry.getKey(), normalizedPath)) {
                matchedEntries.add(entry);
            }
        }
        if (matchedEntries.isEmpty()) {
            return new ArrayList<RuleNode>();
        }
        final Comparator<String> comparator = pathMatcher.getPatternComparator(normalizedPath);
        Collections.sort(matchedEntries, new Comparator<Map.Entry<String, List<RuleNode>>>() {
            @Override
            public int compare(Map.Entry<String, List<RuleNode>> left, Map.Entry<String, List<RuleNode>> right) {
                return comparator.compare(left.getKey(), right.getKey());
            }
        });
        return matchedEntries.get(0).getValue();
    }

    public void parse(String dsl) {
        pathRuleMap = new LinkedHashMap<String, List<RuleNode>>();
        DslValidationResult validationResult = validate(dsl);
        if (!validationResult.isValid()) {
            return;
        }
        String normalizedDsl = normalizeDsl(dsl);
        Object raw = loadYaml(normalizedDsl);
        if (!(raw instanceof Map)) {
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> root = (Map<String, Object>) raw;
        List<Map<String, Object>> paths = asMapList(root.get("paths"));
        if (!paths.isEmpty()) {
            for (Map<String, Object> pathConfig : paths) {
                List<RuleNode> sections = parseSections(pathConfig.get("sections"));
                if (sections.isEmpty()) {
                    continue;
                }
                for (String pattern : resolvePathPatterns(pathConfig)) {
                    pathRuleMap.put(pattern, sections);
                }
            }
        }

        if (pathRuleMap.isEmpty()) {
            List<RuleNode> sections = parseSections(root.get("sections"));
            if (!sections.isEmpty()) {
                pathRuleMap.put(DEFAULT_PATH_PATTERN, sections);
            }
        }
    }

    public DslValidationResult validate(String dsl) {
        DslValidationResult result = new DslValidationResult();
        String normalizedDsl = normalizeDsl(dsl);
        if (StringUtils.isBlank(normalizedDsl)) {
            result.addError("DSL content is empty");
            return result;
        }
        Object raw = loadYaml(normalizedDsl);
        if (!(raw instanceof Map)) {
            result.addError("DSL root must be a YAML object");
            return result;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> root = (Map<String, Object>) raw;
        validateRoot(root, result);
        return result;
    }

    private Object loadYaml(String content) {
        try {
            return yaml.load(content);
        } catch (Exception e) {
            return null;
        }
    }

    private List<RuleNode> parseSections(Object rawSections) {
        List<Map<String, Object>> sectionConfigs = asMapList(rawSections);
        List<RuleNode> sections = new ArrayList<RuleNode>();
        for (Map<String, Object> sectionConfig : sectionConfigs) {
            RuleNode section = parseSection(sectionConfig);
            if (section != null) {
                sections.add(section);
            }
        }
        return sections;
    }

    private RuleNode parseSection(Map<String, Object> config) {
        if (config == null) {
            return null;
        }
        String name = asString(config.get("name"));
        String selector = asString(config.get("selector"));
        if (StringUtils.isBlank(name) || StringUtils.isBlank(selector)) {
            return null;
        }

        RuleNode node = new RuleNode(name.trim(), selector.trim());
        node.setSectionOptions(buildOptions(config.get("meta"), config.get("limit"), null, null));
        parseFields(config.get("fields"), node);
        RuleNode itemTemplate = parseItems(config.get("items"));
        if (itemTemplate != null) {
            node.setItemTemplate(itemTemplate);
        }
        return hasUsableContent(node) ? node : null;
    }

    private boolean hasUsableContent(RuleNode node) {
        return node != null
                && ((!node.getFieldSelectors().isEmpty())
                || node.getItemTemplate() != null);
    }

    private RuleNode parseItems(Object rawItems) {
        if (!(rawItems instanceof Map)) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> itemsConfig = (Map<String, Object>) rawItems;
        String selector = asString(itemsConfig.get("selector"));
        if (StringUtils.isBlank(selector)) {
            return null;
        }
        RuleNode itemTemplate = new RuleNode("items", selector.trim());
        itemTemplate.setSectionOptions(buildOptions(itemsConfig.get("meta"), itemsConfig.get("limit"), null, null));
        parseFields(itemsConfig.get("fields"), itemTemplate);
        return hasUsableContent(itemTemplate) ? itemTemplate : null;
    }

    private void parseFields(Object rawFields, RuleNode node) {
        if (!(rawFields instanceof Map) || node == null) {
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> fields = (Map<String, Object>) rawFields;
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            String fieldName = entry.getKey() == null ? "" : entry.getKey().trim().toLowerCase();
            if (!ALLOWED_FIELDS.contains(fieldName)) {
                continue;
            }
            FieldSpec fieldSpec = parseFieldSpec(entry.getValue());
            if (fieldSpec == null || StringUtils.isBlank(fieldSpec.selector)) {
                continue;
            }
            if (!node.getFieldSelectors().containsKey(fieldName)) {
                node.getFieldSelectors().put(fieldName, fieldSpec.selector);
                node.getFieldOptions().put(fieldName, fieldSpec.options);
            }
        }
    }

    private FieldSpec parseFieldSpec(Object rawFieldConfig) {
        if (rawFieldConfig instanceof String) {
            String selector = trimToNull((String) rawFieldConfig);
            if (selector == null) {
                return null;
            }
            return new FieldSpec(selector, new RuleNode.Options());
        }
        if (!(rawFieldConfig instanceof Map)) {
            return null;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> fieldConfig = (Map<String, Object>) rawFieldConfig;
        String selector = trimToNull(asString(fieldConfig.get("selector")));
        if (selector == null) {
            return null;
        }
        RuleNode.Options options = buildOptions(fieldConfig.get("meta"), null, fieldConfig.get("attr"), fieldConfig.get("transforms"));
        if (fieldConfig.containsKey("transform") && !options.getValues().containsKey(OptionKeyEnum.TRANSFORM.getKey())) {
            mergeTransformOption(options, fieldConfig.get("transform"));
        }
        return new FieldSpec(selector, options);
    }

    private RuleNode.Options buildOptions(Object rawMeta, Object rawLimit, Object rawAttr, Object rawTransforms) {
        RuleNode.Options options = new RuleNode.Options();
        if (rawMeta instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> meta = (Map<String, Object>) rawMeta;
            for (Map.Entry<String, Object> entry : meta.entrySet()) {
                String key = trimToNull(entry.getKey());
                if (key != null && entry.getValue() != null) {
                    options.putIfAbsent(key, entry.getValue());
                }
            }
        }
        if (rawLimit != null) {
            String limitValue = trimToNull(asString(rawLimit));
            if (limitValue != null) {
                options.putIfAbsent(OptionKeyEnum.LIMIT.getKey(), limitValue);
            }
        }
        if (rawAttr != null) {
            String attrValue = trimToNull(asString(rawAttr));
            if (attrValue != null) {
                options.putIfAbsent(OptionKeyEnum.ATTR.getKey(), attrValue);
            }
        }
        mergeTransformOption(options, rawTransforms);
        return options;
    }

    private void mergeTransformOption(RuleNode.Options options, Object rawTransforms) {
        String transformValue = joinTransforms(rawTransforms);
        if (StringUtils.isNotBlank(transformValue)) {
            options.putIfAbsent(OptionKeyEnum.TRANSFORM.getKey(), transformValue);
        }
    }

    private List<String> resolvePathPatterns(Map<String, Object> pathConfig) {
        List<String> patterns = new ArrayList<String>();
        if (pathConfig == null) {
            patterns.add(DEFAULT_PATH_PATTERN);
            return patterns;
        }
        String singleMatch = trimToNull(asString(pathConfig.get("match")));
        if (singleMatch != null) {
            patterns.add(singleMatch);
        }

        Object rawMatches = pathConfig.get("matches");
        if (rawMatches instanceof List) {
            List<?> values = (List<?>) rawMatches;
            for (Object value : values) {
                String pattern = trimToNull(asString(value));
                if (pattern != null) {
                    patterns.add(pattern);
                }
            }
        }

        if (patterns.isEmpty()) {
            patterns.add(DEFAULT_PATH_PATTERN);
        }
        return patterns;
    }

    private List<Map<String, Object>> asMapList(Object rawValue) {
        if (!(rawValue instanceof List)) {
            return new ArrayList<Map<String, Object>>();
        }
        List<?> rawList = (List<?>) rawValue;
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Object item : rawList) {
            if (item instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> mapItem = (Map<String, Object>) item;
                result.add(mapItem);
            }
        }
        return result;
    }

    private void validateRoot(Map<String, Object> root, DslValidationResult result) {
        boolean hasPaths = root.get("paths") instanceof List;
        boolean hasSections = root.get("sections") instanceof List;
        if (!hasPaths && !hasSections) {
            result.addError("DSL must contain either top-level 'paths' or top-level 'sections'");
            return;
        }
        if (hasPaths) {
            List<Map<String, Object>> paths = asMapList(root.get("paths"));
            if (paths.isEmpty()) {
                result.addError("'paths' must contain at least one path rule");
            }
            for (int i = 0; i < paths.size(); i++) {
                validatePath(paths.get(i), i, result);
            }
        }
        if (hasSections) {
            validateSections(root.get("sections"), "sections", result);
        }
    }

    private void validatePath(Map<String, Object> pathConfig, int index, DslValidationResult result) {
        if (pathConfig == null) {
            result.addError("paths[" + index + "] must be an object");
            return;
        }
        List<String> patterns = resolvePathPatterns(pathConfig);
        if (patterns.isEmpty()) {
            result.addError("paths[" + index + "] must define 'match' or 'matches'");
        }
        validateSections(pathConfig.get("sections"), "paths[" + index + "].sections", result);
    }

    private void validateSections(Object rawSections, String location, DslValidationResult result) {
        List<Map<String, Object>> sectionConfigs = asMapList(rawSections);
        if (sectionConfigs.isEmpty()) {
            result.addError(location + " must contain at least one section");
            return;
        }
        for (int i = 0; i < sectionConfigs.size(); i++) {
            validateSection(sectionConfigs.get(i), location + "[" + i + "]", result);
        }
    }

    private void validateSection(Map<String, Object> section, String location, DslValidationResult result) {
        if (section == null) {
            result.addError(location + " must be an object");
            return;
        }
        requireNonBlank(section.get("name"), location + ".name", result);
        requireNonBlank(section.get("selector"), location + ".selector", result);
        validateLimit(section.get("limit"), location + ".limit", result);
        boolean hasFields = section.get("fields") instanceof Map;
        boolean hasItems = section.get("items") instanceof Map;
        if (!hasFields && !hasItems) {
            result.addError(location + " must define 'fields' or 'items'");
        }
        if (hasFields) {
            validateFields(section.get("fields"), location + ".fields", result);
        }
        if (hasItems) {
            validateItems(section.get("items"), location + ".items", result);
        }
    }

    private void validateItems(Object rawItems, String location, DslValidationResult result) {
        if (!(rawItems instanceof Map)) {
            result.addError(location + " must be an object");
            return;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> items = (Map<String, Object>) rawItems;
        requireNonBlank(items.get("selector"), location + ".selector", result);
        validateLimit(items.get("limit"), location + ".limit", result);
        validateFields(items.get("fields"), location + ".fields", result);
    }

    private void validateFields(Object rawFields, String location, DslValidationResult result) {
        if (!(rawFields instanceof Map)) {
            result.addError(location + " must be an object");
            return;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> fields = (Map<String, Object>) rawFields;
        if (fields.isEmpty()) {
            result.addError(location + " must contain at least one field");
            return;
        }
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            String fieldName = entry.getKey() == null ? "" : entry.getKey().trim().toLowerCase();
            String fieldLocation = location + "." + fieldName;
            if (!ALLOWED_FIELDS.contains(fieldName)) {
                result.addError(fieldLocation + " is not supported. Allowed fields: text, img, link");
                continue;
            }
            validateFieldConfig(entry.getValue(), fieldLocation, result);
        }
    }

    private void validateFieldConfig(Object rawFieldConfig, String location, DslValidationResult result) {
        if (rawFieldConfig instanceof String) {
            if (StringUtils.isBlank((String) rawFieldConfig)) {
                result.addError(location + ".selector must not be blank");
            }
            return;
        }
        if (!(rawFieldConfig instanceof Map)) {
            result.addError(location + " must be a string selector or an object");
            return;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> fieldConfig = (Map<String, Object>) rawFieldConfig;
        requireNonBlank(fieldConfig.get("selector"), location + ".selector", result);
        if (fieldConfig.containsKey("attr")) {
            requireNonBlank(fieldConfig.get("attr"), location + ".attr", result);
        }
        validateTransforms(fieldConfig.get("transforms"), location + ".transforms", result);
        if (fieldConfig.containsKey("transform")) {
            validateTransforms(fieldConfig.get("transform"), location + ".transform", result);
        }
    }

    private void validateTransforms(Object rawTransforms, String location, DslValidationResult result) {
        if (rawTransforms == null) {
            return;
        }
        List<String> transforms = new ArrayList<String>();
        if (rawTransforms instanceof List) {
            for (Object value : (List<?>) rawTransforms) {
                String transform = trimToNull(asString(value));
                if (transform != null) {
                    transforms.add(transform);
                }
            }
        } else {
            String transformValue = trimToNull(asString(rawTransforms));
            if (transformValue != null) {
                String[] split = transformValue.split("\\|");
                for (String transform : split) {
                    String normalized = trimToNull(transform);
                    if (normalized != null) {
                        transforms.add(normalized);
                    }
                }
            }
        }
        if (transforms.isEmpty()) {
            result.addError(location + " must not be empty");
            return;
        }
        for (String transform : transforms) {
            if (!ALLOWED_TRANSFORMS.contains(transform)) {
                result.addError(location + " contains unsupported transform '" + transform + "'");
            }
        }
    }

    private void validateLimit(Object rawLimit, String location, DslValidationResult result) {
        if (rawLimit == null) {
            return;
        }
        String value = trimToNull(asString(rawLimit));
        if (value == null) {
            result.addError(location + " must not be blank");
            return;
        }
        try {
            if (Long.parseLong(value) < 0) {
                result.addError(location + " must be greater than or equal to 0");
            }
        } catch (NumberFormatException e) {
            result.addError(location + " must be an integer");
        }
    }

    private void requireNonBlank(Object value, String location, DslValidationResult result) {
        if (StringUtils.isBlank(asString(value))) {
            result.addError(location + " must not be blank");
        }
    }

    private String joinTransforms(Object rawTransforms) {
        if (rawTransforms instanceof List) {
            List<?> values = (List<?>) rawTransforms;
            List<String> transforms = new ArrayList<String>();
            for (Object value : values) {
                String transform = trimToNull(asString(value));
                if (transform != null) {
                    transforms.add(transform);
                }
            }
            return transforms.isEmpty() ? null : joinWithPipe(transforms);
        }
        return trimToNull(asString(rawTransforms));
    }

    private String joinWithPipe(List<String> transforms) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < transforms.size(); i++) {
            if (i > 0) {
                builder.append("|");
            }
            builder.append(transforms.get(i));
        }
        return builder.toString();
    }

    private String normalizePath(String path) {
        if (StringUtils.isBlank(path)) {
            return "/";
        }
        return path.trim();
    }

    private static String normalizeDsl(String dsl) {
        if (dsl == null) {
            return "";
        }
        return dsl.replace("\r\n", "\n")
                .replace("\r", "\n")
                .trim();
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        return value.toString();
    }

    private String trimToNull(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        return value.trim();
    }

    private static class FieldSpec {
        private final String selector;
        private final RuleNode.Options options;

        private FieldSpec(String selector, RuleNode.Options options) {
            this.selector = selector;
            this.options = options;
        }
    }
}
