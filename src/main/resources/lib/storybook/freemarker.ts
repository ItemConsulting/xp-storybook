import { type Configuration, getConfiguration, render as renderFreemarker } from "/lib/freemarker";
import type { TemplateErrors } from "/lib/storybook/errors";
import { Files, Paths, TemplateClassResolver, TemplateExceptionHandler } from "/lib/storybook/java";
import type { RenderParams } from "/lib/storybook/params";

// lib-freemarker's Configuration type omits setNewBuiltinClassResolver (inherited from Configurable).
type HardenedConfiguration = Configuration & {
  setNewBuiltinClassResolver(resolver: unknown): void;
};

const storybookService = __.newBean<{
  getPortalObject(baseDirPath: string | undefined): unknown;
  getFileAndResourceTemplateLoader(dirPaths: string[], appName?: string): unknown;
  newTemplateErrorCollector(): TemplateErrors;
}>("no.item.storybook.freemarker.StorybookScriptBean");

/**
 * Creates a collector for the errors of all templates rendered while serving one request.
 */
export function newTemplateErrors(): TemplateErrors {
  return storybookService.newTemplateErrorCollector();
}

export function render(params: RenderParams, model: Record<string, unknown>, templateErrors?: TemplateErrors): string {
  const dirPaths = getResourcesDirPaths(params.xpResourcesDirPath);
  // If view is a filepath, look up if it exists. `name` indicates inline template.
  const baseDir = params.type === "file" ? getBaseDirIfFileExists(dirPaths, params.filePath) : dirPaths[0];

  const configuration = getConfiguration() as HardenedConfiguration;
  // Deny FreeMarker's `?new` built-in. The dev-mode gate is the real control; this is defence in
  // depth that closes the reflective path from a caller-supplied template to arbitrary code.
  configuration.setNewBuiltinClassResolver(TemplateClassResolver.ALLOWS_NOTHING_RESOLVER);
  configuration.setTemplateExceptionHandler(templateErrors ?? TemplateExceptionHandler.HTML_DEBUG_HANDLER);
  configuration.setTemplateLoader(storybookService.getFileAndResourceTemplateLoader(dirPaths, params.xpAppName));
  configuration.setSharedVariable("portal", storybookService.getPortalObject(baseDir));

  if (params.type === "file") {
    return renderFreemarker(params.filePath, model);
  } else {
    return renderFreemarker(params.template, model, params.name);
  }
}

function getResourcesDirPaths(str: string | undefined): string[] {
  return str?.split(",").map((str) => str.trim()) ?? [];
}

function getBaseDirIfFileExists(baseDirPaths: string[], filePath: string): string | undefined {
  for (const baseDir of baseDirPaths) {
    if (Files.exists(Paths.get(baseDir, filePath))) {
      return baseDir;
    }
  }

  return undefined;
}
