import type { ScriptValue } from "@enonic-types/core";

type ThymeleafService = {
  newFileProcessor(baseDirPath?: string): {
    model: ScriptValue;
    filePath: string;
    process(): string;
  };

  newInlineTemplateProcessor(baseDirPath?: string): {
    model: ScriptValue;
    template: string;
    process(): string;
  };
};

export type RenderParams = string | { template: string };

const service = __.newBean<ThymeleafService>("no.item.storybook.thymeleaf.ThymeleafService");

export function render<T = unknown>(params: RenderParams, model: T): string {
  if (typeof params === "string") {
    return renderFile<T>(params, model);
  } else {
    return renderInlineTemplate<T>(params.template, model);
  }
}

export function renderFile<T = unknown>(id: string, model: T): string {
  const processor = service.newFileProcessor(app.config.xpResourcesDirPath);

  processor.filePath = "/" + id;
  processor.model = __.toScriptValue(model);

  return processor.process();
}

export function renderInlineTemplate<T = unknown>(template: string, model: T): string {
  const processor = service.newInlineTemplateProcessor(app.config.xpResourcesDirPath);

  processor.template = template;
  processor.model = __.toScriptValue(model);

  return processor.process();
}
