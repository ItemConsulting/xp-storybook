import { render as renderFreemarker, getConfiguration } from "/lib/freemarker";
import { Paths, Files, TemplateExceptionHandler } from "/lib/storybook/java";
import type { RenderParams } from "/lib/storybook/params";

const storybookService = __.newBean<{
  createLegacyDirectives(baseDirPath: string): Record<string, unknown>;
  getPortalObject(baseDirPath: string): unknown;
  getFileAndResourceTemplateLoader(dirPaths: string[], appName?: string): unknown;
}>("no.item.storybook.freemarker.StorybookScriptBean");

export function render(params: RenderParams, model: Record<string, unknown>): string {
  const dirPaths = getResourcesDirPaths(params.xpResourcesDirPath);
  // If view is a filepath, look up if it exists. `name` indicates inline template.
  const baseDir = params.type === "file" ? getBaseDirIfFileExists(dirPaths, params.filePath) : dirPaths[0];

  const configuration = getConfiguration();
  configuration.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
  configuration.setTemplateLoader(storybookService.getFileAndResourceTemplateLoader(dirPaths, params.xpAppName));
  configuration.setSharedVariable("portal", storybookService.getPortalObject(baseDir));

  addLegacyDirectivesIfNoConflict(model, baseDir);

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

function addLegacyDirectivesIfNoConflict(model: Record<string, unknown>, baseDir: string): void {
  const directives = storybookService.createLegacyDirectives(baseDir);

  [
    "component",
    "pageUrl",
    "imageUrl",
    "attachmentUrl",
    "componentUrl",
    "serviceUrl",
    "processHtml",
    "imagePlaceholder",
    "assetUrl",
    "localize",
  ]
    .filter((key) => model[key] === undefined)
    .forEach((key) => {
      model[key] = directives[key];
    });
}
