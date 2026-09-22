const bean = __.newBean<{
  isValidApplicationKey(appName: string): boolean;
  getApplicationState(appName: string): string;
  resourceExists(appName: string, path: string): boolean;
  getResolverName(appName: string, path: string): string | null;
  getInstalledApplicationNames(): string;
}>("no.item.storybook.app.ApplicationScriptBean");

export const STATE_MISSING = "MISSING";
export const STATE_STOPPED = "STOPPED";

/** XP tags a resource read from the installed jar rather than from an application's source directory. */
const BUNDLE_RESOLVER = "bundle";

export function isValidApplicationKey(appName: string): boolean {
  return bean.isValidApplicationKey(appName);
}

/** `MISSING`, `STOPPED` or `STARTED`. */
export function getApplicationState(appName: string): string {
  return bean.getApplicationState(appName);
}

export function resourceExists(appName: string, path: string): boolean {
  return bean.resourceExists(appName, path);
}

export function getInstalledApplicationNames(): string {
  return bean.getInstalledApplicationNames();
}

/**
 * Whether the template came out of the installed jar instead of the application's source directory, which means the
 * application was not built with dev source paths and edits on disk will not show up.
 */
export function isServedFromJar(appName: string, path: string): boolean {
  return bean.getResolverName(appName, path) === BUNDLE_RESOLVER;
}
