/**
 * Errors that happened while rendering a template.
 *
 * Doubles as the FreeMarker `TemplateExceptionHandler`, since FreeMarker renders template errors into the output
 * instead of throwing. Thymeleaf throws instead, so the Thymeleaf renderer reports its errors with `add()`.
 */
export type TemplateErrors = {
  /**
   * Record an error from a template engine that reports failures by throwing.
   */
  add(message: string): void;

  hasErrors(): boolean;

  /**
   * Every collected error message, or `null` if nothing failed.
   */
  getMessage(): string | null;
};

const bean = __.newBean<{ newTemplateErrors(): TemplateErrors }>("no.item.storybook.render.RenderScriptBean");

/**
 * Creates a collector for the errors of all templates rendered while serving one request.
 */
export function newTemplateErrors(): TemplateErrors {
  return bean.newTemplateErrors();
}
