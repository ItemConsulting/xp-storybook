---
"xp-storybook": major
---

Upgrade to Enonic XP 8

- Requires XP 8.0.0 or later, and JDK 25 to build.
- The app now renders only when Enonic XP runs in development mode, which `enonic sandbox start` does by default.
- Descriptors migrated to the XP 8 YAML format: `application.xml` is now `enonic.yaml`, and `site/site.xml` is now `cms/cms.yaml`.
- The rendering endpoint is now a Universal API at `/api/no.item.storybook:preview/<template-path>`.
- Logs a security warning on every startup (not only in production mode) stating that the app renders caller-supplied templates and is a local development tool only.
- CORS is now restricted to loopback origins. The endpoint previously answered every request with `Access-Control-Allow-Origin: *`. The header is now echoed only for `localhost`/`127.0.0.1`/`[::1]` origins, which is where Storybook and the Vitest runner live.
