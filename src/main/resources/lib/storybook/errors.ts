/**
 * Errors that happened while rendering a template. Doubles as the FreeMarker `TemplateExceptionHandler` that
 * collects them, since template errors are rendered into the output instead of being thrown.
 */
export type TemplateErrors = {
  hasErrors(): boolean;

  /**
   * Every collected error message, or `null` if nothing failed.
   */
  getMessage(): string | null;
};
