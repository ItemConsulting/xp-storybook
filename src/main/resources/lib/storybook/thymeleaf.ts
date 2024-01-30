type ThymeleafService = {
  newFileProcessor(baseDirPath: string): {
    model: ScriptValue;
    filePath: string;
    process(): string;
  };

  newInlineTemplateProcessor(baseDirPath: string): {
    model: ScriptValue;
    template: string;
    process(): string;
  };
};

/**
 * Freemarker template related functions.
 */
const service = __.newBean<ThymeleafService>("no.item.storybook.thymeleaf.ThymeleafService");

export function renderFile<T = unknown>(filePath: string, model: T): string {
  const processor = service.newFileProcessor(getBaseDirPath());

  //processor.baseDirPath = getBaseDirPath(filePath);
  processor.filePath = cleanFilePath(filePath);
  processor.model = __.toScriptValue(model);

  return processor.process();
}

/**
 * This function renders a template using Thymeleaf.
 */
export function renderInlineTemplate<T = unknown>(template: string, model: T): string {
  const processor = service.newInlineTemplateProcessor(getBaseDirPath());

  processor.template = template;
  processor.model = __.toScriptValue(model);

  return processor.process();
}

export function getBaseDirPath(): string {
  const baseDirPath = app.config.xpResourcesDirPath;

  if (!baseDirPath) {
    throw new Error(`Please configure "xpResourcesDirPath" in your "no.item.storybook.cfg".`);
  }

  return baseDirPath;
}

function cleanFilePath(filePath: string): string {
  const index = filePath.indexOf("?");

  return index === -1 ? filePath : filePath.substring(0, index);
}
