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

export function render(params: RenderParams, model: Record<string, unknown>): string {
  if (typeof params === "string") {
    return renderFile(params, model);
  } else {
    return renderInlineTemplate(params.template, model);
  }
}

export function renderFile(id: string, model: Record<string, unknown>): string {
  const processor = service.newFileProcessor(app.config.xpResourcesDirPath);

  processor.filePath = "/" + id;
  processor.model = __.toScriptValue(model);

  return processor.process();
}

export function renderInlineTemplate(template: string, model: Record<string, unknown>): string {
  const processor = service.newInlineTemplateProcessor(app.config.xpResourcesDirPath);

  processor.template = template;
  processor.model = __.toScriptValue(model);

  return processor.process();
}
