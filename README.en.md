# TV-Like Overview

## What TV-Like Does

TV-Like extracts the most important parts of a web page and converts them into a TV-friendly structured result.

It uses a readable YAML DSL to describe:

- which URL paths should match a rule
- which page sections should be extracted
- how to fetch `text`, `img`, and `link`
- which transforms should be applied to each field

## Why YAML DSL

The old custom DSL was compact, but it was hard to scale:

- the syntax could conflict with CSS selectors
- options were packed into one line and hard to read
- validation and tooling were difficult
- extending the grammar increased parser complexity quickly

The YAML DSL makes the rule model explicit and easier to maintain.

## Core Features

- Inline YAML extraction rules inside the page
- Remote YAML hub lookup
- Specific path rules win over generic path rules
- Section extraction and list extraction
- Field types: `text`, `img`, `link`
- Field options: `attr`, `transforms`
- Section and item `limit`
- Pass-through metadata with `meta`

## Minimal Example

```yaml
version: 1
sections:
  - name: hero
    selector: .hero-card
    fields:
      text:
        selector: .title
        transforms: [trim]
      link:
        selector: .title a
        attr: href
        transforms: [abs-url]
```

## Quick Start

```java
String html = "...";
String url = "https://example.com/list";

List<Map<String, Object>> result = new TV(html, url).like();
```

## More Docs

- Chinese overview: `README.zh.md`
- Chinese tutorial: `TUTORIAL.zh.md`
- English tutorial: `TUTORIAL.en.md`
- Project website copy: `index.html`
