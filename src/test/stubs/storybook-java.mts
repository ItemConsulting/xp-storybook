// Stub for "/lib/storybook/java". The real module reaches into the JVM through Java.type(), which
// Node has no equivalent for. Paths and Files are backed by node:fs here, so the directory
// resolution in resources.ts is tested against a real filesystem rather than a fake one.
import { existsSync } from "node:fs";
import { join } from "node:path";

export type Path = string;

export const Paths = {
  get: (path: string, ...more: string[]): Path => join(path, ...more),
};

export const Files = {
  exists: (path: Path): boolean => existsSync(path),
};

export const TemplateExceptionHandler = { HTML_DEBUG_HANDLER: "HTML_DEBUG_HANDLER" };
export const TemplateClassResolver = { ALLOWS_NOTHING_RESOLVER: "ALLOWS_NOTHING_RESOLVER" };
export const RunMode = { isDev: (): boolean => true };
