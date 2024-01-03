type FreemarkerService = {
  newFileProcessor(): {
    model: ScriptValue;
    baseDirPath: string;
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

/**
 * Freemarker template related functions.
 */
const service = __.newBean<FreemarkerService>("no.item.storybook.FreemarkerService");

/**
 * This function renders a view using Freemarker.
 */
export function renderFile<T = unknown>(filePath: string, model: T): string {
  const processor = service.newFileProcessor();

  processor.baseDirPath = getBaseDirPath(filePath);
  processor.filePath = cleanFilePath(filePath);
  processor.model = __.toScriptValue(model);

  return processor.process();
}

/**
 * This function renders a template using Freemarker.
 */
export function renderInlineTemplate<T = unknown>(
  template: string,
  model: T,
  config?: {
    filePath?: string;
  },
): string {
  const processor = service.newInlineTemplateProcessor();

  if (config?.filePath) {
    processor.baseDirPath = getBaseDirPath(config?.filePath);
  }
  processor.template = template;
  processor.model = __.toScriptValue(model);

  return processor.process();
}

export function getBaseDirPath(filePath: string): string {
  const resourcesDir = "resources";
  const srcDirIndex = filePath.indexOf(resourcesDir);

  return filePath.substring(0, srcDirIndex + resourcesDir.length);
}

function cleanFilePath(filePath: string): string {
  const index = filePath.indexOf("?");

  return index === -1 ? filePath : filePath.substring(0, index);
}
