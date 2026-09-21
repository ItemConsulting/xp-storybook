export const TemplateExceptionHandler = Java.type<{
  HTML_DEBUG_HANDLER: unknown;
}>("freemarker.template.TemplateExceptionHandler");

export type Path = {
  compareTo(other: Path): number;
  endsWith(other: string): boolean;
  endsWith(other: Path): boolean;
  resolve(other: string): boolean;
  resolve(other: Path): boolean;
  startsWith(other: string): boolean;
  startsWith(other: Path): boolean;
  normalize(): Path;
};

export const Paths = Java.type<{
  get(path: string, ...more: string[]): Path;
}>("java.nio.file.Paths");

export const Files = Java.type<{
  exists(path: Path): boolean;
}>("java.nio.file.Files");

/**
 * XP's own run mode. `enonic sandbox start` runs in dev mode by default (`--prod` opts out), while
 * a production installation runs in PROD — which is what lets this app refuse to render there.
 */
export const RunMode = Java.type<{
  isDev(): boolean;
}>("com.enonic.xp.server.RunMode");

/**
 * FreeMarker's resolver for the `?new` built-in. `?new` is the reflective escape hatch that turns a
 * caller-supplied template into arbitrary code (e.g. `"...Execute"?new()`), so we set the
 * configuration's resolver to `ALLOWS_NOTHING_RESOLVER`, which denies `?new` outright.
 */
export const TemplateClassResolver = Java.type<{
  ALLOWS_NOTHING_RESOLVER: unknown;
}>("freemarker.core.TemplateClassResolver");
