# TV-Like YAML Tutorial

## 1. Goal

The YAML DSL describes how TV-Like should extract stable content blocks from a web page.

Supported features in the current engine:

- path matching
- sections
- list items
- field types: `text`, `img`, `link`
- `attr`, `limit`, `transforms`
- `meta` pass-through values

## 2. Top-Level Structure

```yaml
version: 1
paths:
  - match: /list/**
    sections:
      - name: hero
        selector: .hero-card
        fields:
          text:
            selector: .title
```

You can also define `sections` directly at the top level when path splitting is not needed.

## 3. Paths

```yaml
version: 1
paths:
  - matches:
      - /movie/**
      - /tv/**
    sections: []
```

More specific path rules are matched before generic ones.

## 4. Sections

```yaml
- name: hero
  selector: .hero-card
  limit: 1
  meta:
    badge: featured
  fields:
    text:
      selector: .title
      transforms: [trim, upper]
```

## 5. Fields

```yaml
fields:
  text:
    selector: .title
    transforms: [trim]
  link:
    selector: a
    attr: href
    transforms: [abs-url]
  img:
    selector: img[data-src]
    attr: data-src
    transforms: [abs-url]
```

Supported transforms:

- `trim`
- `upper`
- `lower`
- `digits`
- `abs-url`

## 6. Items

```yaml
items:
  selector: li.card
  limit: 12
  meta:
    img-ratio: 2/3
  fields:
    text:
      selector: .title
      transforms: [trim]
    link:
      selector: a
      attr: href
      transforms: [abs-url]
```

## 7. Inline Rule Example

```html
<script type="application/yaml" name="tv-like">
version: 1
sections:
  - name: hero
    selector: .hero
    fields:
      text:
        selector: h1
</script>
```

## 8. Remote Hub Lookup

When no inline rule exists, TV-Like tries remote YAML files such as:

- `${host}.yaml`
- `${host}.yml`
- reversed domain paths like `com/example/www.yaml`

## 9. Validate Rules

You can validate a YAML DSL before extraction:

```java
RuleParser parser = new RuleParser();
DslValidationResult result = parser.validate(dsl);

if (!result.isValid()) {
    for (String error : result.getErrors()) {
        System.out.println(error);
    }
}
```

The current validator checks common issues such as:

- missing top-level `paths` or `sections`
- missing `name` or `selector` in a section
- missing `selector` in `items`
- empty `fields` or unsupported field names
- invalid `limit`
- unsupported transforms
