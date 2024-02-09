type FreemarkerService = {
  newFileProcessor(): {
    model: ScriptValue;
    baseDirPath?: string;
    filePath: string;
    process(): string;
  };

  newInlineTemplateProcessor(): {
    model: ScriptValue;
    baseDirPath?: string;
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

  processor.baseDirPath = app.config.xpResourcesDirPath;
  processor.filePath = app.config.xpResourcesDirPath + "/" + id;
  processor.model = __.toScriptValue(model);

  return processor.process();
}

export function renderInlineTemplate<T = unknown>(template: string, model: T): string {
  const processor = service.newInlineTemplateProcessor();

  processor.baseDirPath = app.config.xpResourcesDirPath;
  processor.template = template;
  processor.model = __.toScriptValue(model);

  return processor.process();
}
