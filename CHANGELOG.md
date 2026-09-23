# xp-storybook

## 2.0.0

### Major Changes

- 0bce9db: Upgrade to Enonic XP 8
  
  - Requires XP 8.0.0 or later, and JDK 25 to build.
  - The app now renders only when Enonic XP runs in development mode, which `enonic sandbox start` does by default.
  - Descriptors migrated to the XP 8 YAML format: `application.xml` is now `enonic.yaml`, and `site/site.xml` is now `cms/cms.yaml`.
  - The rendering endpoint is now a Universal API at `/api/no.item.storybook:preview/<template-path>`.
  - Logs a security warning on every startup (not only in production mode) stating that the app renders caller-supplied templates and is a local development tool only.
  - CORS is now restricted to loopback origins. The endpoint previously answered every request with `Access-Control-Allow-Origin: *`. The header is now echoed only for `localhost`/`127.0.0.1`/`[::1]` origins, which is where Storybook and the Vitest runner live.
- 098b23e: Read templates through Enonic XP's resource service instead of from a caller-supplied directory

### Minor Changes

- 3d4febf: Check site directory for i18n-directory if not found under resources
- 7826d11: Bring the Thymeleaf renderer up to parity with the FreeMarker one
  
  - A template that is not on disk is looked up in the named application's resources.
  - The Thymeleaf engine is now built per render instead of being configured once and kept.
  - A Thymeleaf template that reaches outside its resource directory through a relative include is refused, and an included file must be a template.
  - Relative includes resolve against the directory the including template was found in, rather than the name it was requested under.
  - A failing Thymeleaf template no longer replaces the whole page.
  - Exception messages are escaped before being written into the error response.
- c937c0a: Respond with status 500 when a FreeMarker template fails to render

### Patch Changes

- 4db5ab8: Apply values when using `portal.localize(key, values)`

## 1.5.0

### Minor Changes

- 5e51c92: Take resourceDirPath and appName as query parameters
- 5e51c92: Move endpoint to "/webapp/no.item.storybook"

## 1.4.0

### Minor Changes

- b7dd0ba: Fix failing portal.processHtml function in FreeMarker
- b7dd0ba: Fix failing inline templating for FreeMarker

## 1.3.0

### Minor Changes

- dc457f6: Use latest version of Freemarker
- cc6b05f: Fix localize in Freemarker-preview when multiple baseDirPaths
- dc457f6: Interpret ftlh file extension as Freemarker
- dc457f6: Add Storybook-compatible version of `PortalObject` where `localize` and `assetUrl` use file system.

## 1.2.0

### Minor Changes

- 3b68b67: Add support for deserializing java.time.LocalDate

## 1.1.1

### Patch Changes

- 1013d93: Fix issue where undefined values can crash the renderer

## 1.1.0

### Minor Changes

- 59538b3: Migrate build tool to tsup
- 232056d: Allow comma separated list in xpResourcesDirPath

## 1.0.4

### Minor Changes

- 232056d: Allow comma separated list in xpResourcesDirPath

## 1.0.3

### Minor Changes

- 40f625f: Roll back change of i18n bundle path

## 1.0.2

### Minor Changes

- 482b039: Add support for `assetUrl` in inline templates

## 1.0.1

### Minor Changes

- eff6823: Fixed broken `[@localize /]` in Freemarker-templates

## 1.0.0

### Minor Changes

- 4fafc49: Initial release. Storybook integration for Freemarker or Thymeleaf templates
