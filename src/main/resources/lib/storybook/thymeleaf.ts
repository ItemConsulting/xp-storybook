import type { ScriptValue } from "@enonic-types/core";
import type { TemplateErrors } from "/lib/storybook/errors";
import type { RenderParams } from "/lib/storybook/params";
import { escapeHtml } from "/lib/storybook/utils";

type ThymeleafService = {
  newFileProcessor(appName: string): {
    model: ScriptValue;
    filePath: string;
    process(): string;
  };

  newInlineTemplateProcessor(appName: string): {
    model: ScriptValue;
    template: string;
    process(): string;
  };
};

const service = __.newBean<ThymeleafService>("no.item.storybook.thymeleaf.ThymeleafService");

export function render(params: RenderParams, model: Record<string, unknown>, templateErrors?: TemplateErrors): string {
  // Thymeleaf reports a failing template by throwing, so — unlike FreeMarker, which is handed the collector as its
  // exception handler — the error is caught here and rendered where the template would have gone. A broken component
  // then leaves its siblings intact, and the controller still hears about it through `templateErrors`.
  try {
    if (params.type === "file") {
      const processor = service.newFileProcessor(params.xpAppName);

      processor.filePath = `/${params.filePath}`;
      processor.model = __.toScriptValue(model);

      return processor.process();
    } else {
      const processor = service.newInlineTemplateProcessor(params.xpAppName);

      processor.template = params.template;
      processor.model = __.toScriptValue(model);

      return processor.process();
    }
  } catch (e) {
    if (!templateErrors) {
      throw e;
    }

    const message = (e as { message?: string }).message ?? String(e);
    templateErrors.add(message);

    return errorBlock(message);
  }
}

/**
 * Mirrors what FreeMarker's `HTML_DEBUG_HANDLER` leaves in the output, so a failing component looks the same in
 * either flavor.
 */
function errorBlock(message: string): string {
  return `<pre style="white-space: pre-wrap; color: #a00;">Thymeleaf template error: ${escapeHtml(message)}</pre>`;
}
