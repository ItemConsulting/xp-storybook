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

This endpoint can only ever read resources belonging to an installed application.
When XP runs in **development mode** an application built in dev mode serves its resources from `src/main/resources` 
from the disk.

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

| Parameter   | Purpose                                                                                    |
|-------------|--------------------------------------------------------------------------------------------|
| `xpAppName` | **Required.** The application whose resources hold the template                            |
| `template`  | Renders this string as an inline template instead of loading one from disk                 |
| `javaTypes` | JSON mapping model keys to a type (`number`, `localDate`, `zonedDateTime`, `region`, ...)  |
| `matchers`  | JSON mapping a type to a `/regex/` matched against model keys                              |

Any other query parameter is passed to the template as a *model value*.

### Requirements

The application being previewed must be **installed in the sandbox**, and built with dev source
paths if you want edits to show up without a rebuild.

What writes the source paths is the `env=dev` Gradle property: it puts an `X-Source-Paths` manifest
header in the jar, which is what XP reads to serve the application's resources from disk. 

There are two ways to set it:

```sh
# Using gradle for a single deploy (and use Storybook after that)
./gradlew deploy -Penv=dev
```

```sh
# Continuously run builds in the background
enonic project dev
```

> [!CAUTION]
> `enonic project deploy` on its own does **not** set `X-Source-Paths`, so files will not be read from disk!

Applications that are not deployed with `env=dev` will not load files from disk, but use the templates **found in the 
jar-files** (also applications installed from Enonic Market).

## Libraries

A library has no application key of its own, so it cannot be previewed on its own. Point Storybook at
an application that `include`s the library, and add the library's resources to that application's dev
source paths:

```groovy
app {
  rawDevSourcePaths.add(file("../lib-xp-forms/src/main/resources").canonicalPath)
}
```

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
