import type { ScriptValue } from "@enonic-types/core";
import type { RenderParams } from "/lib/storybook/params";

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

const service = __.newBean<ThymeleafService>("no.item.storybook.thymeleaf.ThymeleafService");

export function render(params: RenderParams, model: Record<string, unknown>): string {
  if (params.type === "file") {
    return renderFile(params.filePath, model, params.xpResourcesDirPath);
  } else {
    return renderInlineTemplate(params.template, model, params.xpResourcesDirPath);
  }
}

export function renderFile(id: string, model: Record<string, unknown>, xpResourcesDirPath: string): string {
  const processor = service.newFileProcessor(xpResourcesDirPath);

  processor.filePath = "/" + id;
  processor.model = __.toScriptValue(model);

  return processor.process();
}

export function renderInlineTemplate(
  template: string,
  model: Record<string, unknown>,
  xpResourcesDirPath: string,
): string {
  const processor = service.newInlineTemplateProcessor(xpResourcesDirPath);

  processor.template = template;
  processor.model = __.toScriptValue(model);

  return processor.process();
}
