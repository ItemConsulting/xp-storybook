import { render as renderFreemarker, getConfiguration } from "/lib/freemarker";
import { MultiProjectTemplateLoader, Paths, Files, TemplateExceptionHandler } from "/lib/storybook/java";

const storybookService = __.newBean<{
  createLegacyDirectives(baseDirPath: string): Record<string, unknown>;
  getPortalObject(baseDirPath: string): unknown;
}>("no.item.storybook.freemarker.StorybookScriptBean");

export function render(view: string, model: Record<string, unknown>, name?: string): string {
  const dirPaths = getResourcesDirPaths(app.config.xpResourcesDirPath);
  // If view is a filepath, look up if it exists. `name` indicates inline template.
  const baseDir = name ? dirPaths[0] : getBaseDirIfFileExists(dirPaths, view);

  const configuration = getConfiguration();
  configuration.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
  configuration.setTemplateLoader(new MultiProjectTemplateLoader(dirPaths));
  configuration.setSharedVariable("portal", storybookService.getPortalObject(baseDir));

  addLegacyDirectivesIfNoConflict(model, baseDir);

  return renderFreemarker(view, model, name);
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
