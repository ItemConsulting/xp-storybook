import type { ScriptValue } from "@enonic-types/core";

type FreemarkerService = {
  newFileProcessor(): {
    model: ScriptValue;
    baseDirPaths: string[];
    filePath: string;
    process(): string;
  };

  newInlineTemplateProcessor(): {
    model: ScriptValue;
    baseDirPaths: string[];
    template: string;
    process(): string;
  };
};

export type RenderParams = string | { template: string };

const service = __.newBean<FreemarkerService>("no.item.storybook.freemarker.FreemarkerService");

export function render<T = unknown>(params: RenderParams, model: T): string {
  return typeof params === "string" ? renderFile(params, model) : renderInlineTemplate(params.template, model);
}

export function renderFile<T = unknown>(id: string, model: T): string {
  const processor = service.newFileProcessor();

  processor.baseDirPaths = getResourcesDirPaths(app.config.xpResourcesDirPath);
  processor.filePath = id;
  processor.model = __.toScriptValue(model);

  return processor.process();
}

export function renderInlineTemplate<T = unknown>(template: string, model: T): string {
  const processor = service.newInlineTemplateProcessor();

  processor.baseDirPaths = getResourcesDirPaths(app.config.xpResourcesDirPath);
  processor.template = template;
  processor.model = __.toScriptValue(model);

  return processor.process();
}

function getResourcesDirPaths(str: string | undefined): string[] {
  return str?.split(",").map((str) => str.trim()) ?? [];
}
