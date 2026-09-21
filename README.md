# Storybook Server integration for Enonic XP

This application integrates with [Storybook Server renderer](https://www.npmjs.com/package/@storybook/server) and can
render [Apache FreeMarker templates](https://github.com/ItemConsulting/lib-xp-freemarker/) or [Thymeleaf templates](https://github.com/enonic/lib-thymeleaf) with the `args` from Storybook.

![Build badge](https://github.com/ItemConsulting/xp-storybook/actions/workflows/main.yml/badge.svg)
[![](https://repo.itemtest.no/api/badge/latest/releases/no/item/xp-storybook)](https://repo.itemtest.no/#/releases/no/item/xp-storybook)
[![](https://img.shields.io/npm/types/%40itemconsulting%2Fxp-storybook-utils)](https://www.npmjs.com/package/@itemconsulting/xp-storybook-utils)

<img src="https://github.com/ItemConsulting/xp-storybook/raw/main/docs/icon.svg?sanitize=true" width="150">

> [!CAUTION]  
> This application should **never** be deployed in production! An attacker can use this application to render any 
> content on your domain. 
>
> The app will only render templates when Enonic XP runs in **development mode**.

## Versions

| Enonic XP | This library |
| --------- | ------------ |
| 8.x       | 2.x          |
| 7.x       | 1.x          |

### The rendering endpoint

The app exposes a [Universal API](https://developer.enonic.com/docs/code/stable/web/apis) named `preview`, mounted on 
the Web endpoint:

```
http://localhost:8080/api/no.item.storybook:preview/<path/to/template>
```

The path is resolved relative to `xpResourcesDirPath`, and only `.ftl`, `.ftlh`, `.ftlx` and
`.html` files can be rendered — anything else is rejected with a `400`. Since the caller supplies
`xpResourcesDirPath`, this is what keeps the endpoint from being used to read arbitrary files.

## Config

If you have enabled local vhost routing through _com.enonic.xp.web.vhost.cfg_, you need a mapping
that exposes the _api_ endpoint:

```ini
enabled = true

mapping.api.host = localhost
mapping.api.source = /api
mapping.api.target = /api
mapping.api.idProvider.system = default
```

### Reserved query parameters

| Parameter            | Purpose                                                                                  |
|----------------------|------------------------------------------------------------------------------------------|
| `template`           | Renders this string as an inline template instead of loading one from disk                 |
| `renderMode`         | `freemarker` or `thymeleaf`. Inferred from the extension (`.ftl`/`.ftlh`/`.ftlx` → FreeMarker, `.html` → Thymeleaf) when omitted |
| `xpResourcesDirPath` | **Required.** The directory to resolve templates from                                     |
| `xpAppName`          | Application to resolve templates from when they are not on disk                            |
| `javaTypes`          | JSON mapping model keys to a type (`number`, `localDate`, `zonedDateTime`, `region`, ...)  |
| `matchers`           | JSON mapping a type to a `/regex/` matched against model keys                              |

Any other query parameter is passed to the template as a model value.

## Getting started

Install the application from [Enonic Market](https://market.enonic.com/vendors/item-consulting-as/storybook).

You will find information on how to set up your project in the [xp-storybook-utils documentation](https://github.com/ItemConsulting/xp-storybook-utils).

## Deploying

### Building

To build the project run the following code

```bash
enonic project build
```

### Deploy locally

Deploy locally for testing purposes:

```bash
enonic project deploy
```

### Deploy to Maven

```bash
./gradlew publish
```
