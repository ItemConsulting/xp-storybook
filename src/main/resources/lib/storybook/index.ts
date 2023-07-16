export type TextTemplate = {
  template: string;
  baseDirPath?: string;
};

export type File = {
  filePath: string;
  baseDirPath: string;
};

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
 *
 * @param {File} view path and base resource path
 * @param {object} model Model that is passed to the view.
 * @returns {string} The rendered output.
 */
export function renderFile<T = unknown>(view: File, model: T): string {
  const processor = service.newFileProcessor();

  processor.baseDirPath = view.baseDirPath;
  processor.filePath = view.filePath;
  processor.model = __.toScriptValue(model);

  return processor.process();
}

/**
 * This function renders a view using Freemarker.
 *
 * @param {TextTemplate} view inline template or local file path
 * @param {object} model Model that is passed to the view.
 * @returns {string} The rendered output.
 */
export function renderInlineTemplate<T = unknown>(view: TextTemplate, model: T): string {
  const processor = service.newInlineTemplateProcessor();

  if (view.baseDirPath) {
    processor.baseDirPath = view.baseDirPath;
  }
  processor.template = view.template;
  processor.model = __.toScriptValue(model);

  const res = processor.process();

  log.info("Rendered inline " + res);

  return res;
}
