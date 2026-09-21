import { Files, Paths } from "/lib/storybook/java";

/**
 * The directories to resolve templates from. `xpResourcesDirPath` takes a comma separated list, so that a project
 * can render templates that live next to the ones in a shared library.
 */
export function getResourcesDirPaths(str: string | undefined): string[] {
  return str?.split(",").map((str) => str.trim()) ?? [];
}

/**
 * The directory `filePath` was found in, which is the one holding the i18n bundle that belongs to it.
 *
 * @returns `undefined` when no directory holds the file, in which case it is expected to come from an application's
 *     resources instead.
 */
export function getBaseDirIfFileExists(baseDirPaths: string[], filePath: string): string | undefined {
  for (const baseDir of baseDirPaths) {
    if (Files.exists(Paths.get(baseDir, filePath))) {
      return baseDir;
    }
  }

  return undefined;
}
