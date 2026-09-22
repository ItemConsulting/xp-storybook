### The preview must always show the files as they are right now

This app exists so that a developer can edit a template, a fragment or a phrase and see the result
by reloading the story — no rebuild, no redeploy, no restart. That is the product. A change that
makes any part of a render go stale has broken it, even if every test still passes.

So the rule for anything on the render path: **read it again on every request.** Never add a cache,
and be suspicious of any XP or library API that has one, however convenient its semantics are.

### How a file on disk reaches the renderer

Templates and phrases are read through `ResourceService`, keyed on the `ApplicationKey` the caller
passed as `xpAppName`. Nothing takes a filesystem path.

That still reaches the working tree because of XP's dev mode: an application built with the
`env=dev` Gradle property (`enonic project dev`, or `./gradlew deploy -Penv=dev`; a plain
`enonic project deploy` does **not** set it) carries an `X-Source-Paths` manifest header naming its
source directories, and `ApplicationFactory` then puts a `ClassLoaderApplicationUrlResolver` over
those directories *in front of* the installed jar. `ResourceService.getResource` is uncached and
returns a `file:` URL, so every call hits the file.

Two consequences worth knowing:

- An application built without `-Penv=dev` still renders, but from its jar. The endpoint detects
  this with `Resource.getResolverName() == "bundle"` and logs a warning; it is a legitimate case
  (previewing something from Enonic Market), so it never fails the request.
- An XP library has no application key. It is previewed through an application that `include`s it
  and adds the library's resources to its `devSourcePaths`. See the README.

### Caches that have to stay off

| Where | What keeps it live |
| --- | --- |
| Thymeleaf template resolution | `StorybookTemplateResolver.computeValidity` returns `NonCacheableCacheEntryValidity`. Do not "optimise" this. |
| Thymeleaf engine | A `TemplateEngine` refuses to have its resolver replaced once it has rendered, so `ThymeleafService` builds a new one per render. It is also what keeps two requests with different `xpAppName` independent. |
| FreeMarker | `freemarker.ts` sets the template loader, the `portal` object and the exception handler on the shared `Configuration` on every render. lib-xp-freemarker sets `templateUpdateDelayMilliseconds(0)` in dev mode. |
| Phrases | `no.item.storybook.i18n.Phrases` reads the `/i18n` resources per call. |

**Do not replace `Phrases` with XP's `LocaleService`.** It is the obvious move — it is the real
thing, with the right semantics — and it was tried and reverted. `LocaleServiceImpl` caches a
`MessageBundle` per application and locale, so an edited `phrases.properties` does not appear until
the application is deployed again. `Phrases` deliberately reimplements what `LocaleServiceImpl` and
`MessageBundleImpl` do (same default `/i18n/phrases` base name, same `ResourceBundle.Control`
candidate-locale chain merged least-specific-first, same `MessageFormat` handling) so that a phrase
resolves exactly as it would in production, while still being read fresh. Keep those two in step if
XP's behaviour changes.

`PhrasesTest.reads_the_bundle_again_on_every_call` and
`ThymeleafViewFunctionsTest`'s throwing fake `ViewFunctionService` exist to catch a regression here.

### The other invariant: no caller-supplied paths

The endpoint is reachable by GET from any page in the developer's browser, and an inline
`?template=` puts the caller in full control of what gets rendered. Reads are therefore confined to
one application's resources: a `ResourceKey` normalises with `Files.simplifyPath`, so `..` clamps at
the application root, and both template loaders check every name against
`no.item.storybook.render.TemplateExtensions`, so an inline template cannot include the `.ts`,
`.properties` or `.yaml` files that sit under `src/main/resources` in dev. Keep that check on both
loaders — the endpoint only checks the template it was *asked* for, not the ones that one pulls in,
and the FreeMarker side went without it for a while.

### Verifying a change on the render path

Tests cannot prove liveness, because the fake resource service has no cache to begin with. Check it
against a sandbox:

```bash
./gradlew build -Penv=dev                 # JDK 25; plain `build` omits X-Source-Paths
cp build/libs/xp-storybook.jar ~/.enonic/sandboxes/<name>/home/deploy/
curl -sG "http://localhost:8080/api/no.item.storybook:preview/<path>" \
  --data-urlencode "xpAppName=no.item.storybook"
# edit the template or phrases.properties on disk, then repeat the curl.
# The output must change with no rebuild and no redeploy.
```

Do this for a template *and* for `phrases.properties`, in both flavors — they take different code
paths, and the FreeMarker and Thymeleaf sides have drifted apart before.
