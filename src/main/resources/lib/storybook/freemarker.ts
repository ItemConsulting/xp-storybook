import {
  MultiProjectTemplateLoader,
  Paths,
  Files,
  type FreemarkerScriptBean,
  type StorybookScriptBean,
} from "/lib/storybook/java";

export type RenderParams = string | { template: string };

const service = __.newBean<FreemarkerScriptBean>("no.item.freemarker.FreemarkerScriptBean");
const storybookService = __.newBean<StorybookScriptBean>("no.item.storybook.freemarker.StorybookScriptBean");

export function render(view: RenderParams, model: Record<string, unknown>): string {
  const dirPaths = getResourcesDirPaths(app.config.xpResourcesDirPath);
  const processor = service.newProcessor();
  const baseDir = isFilePath(view) ? getBaseDirIfFileExists(dirPaths, view) : dirPaths[0];

  service.useHtmlDebugExceptionHandler();
  service.setTemplateLoader(new MultiProjectTemplateLoader(dirPaths));
  service.setPortalObject(storybookService.getPortalObject(baseDir));

  addLegacyDirectivesIfNoConflict(model, baseDir);

  return typeof view === "string"
    ? processor.process(view, __.toScriptValue(model))
    : processor.processInline(view.template, __.toScriptValue(model));
}

function isFilePath(params: RenderParams): params is string {
  return typeof params === "string";
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
