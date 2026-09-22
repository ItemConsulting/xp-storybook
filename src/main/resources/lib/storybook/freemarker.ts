import { type Configuration, getConfiguration, render as renderFreemarker } from "/lib/freemarker";
import type { TemplateErrors } from "/lib/storybook/errors";
import { TemplateClassResolver, TemplateExceptionHandler } from "/lib/storybook/java";
import type { RenderParams } from "/lib/storybook/params";

// lib-freemarker's Configuration type omits setNewBuiltinClassResolver (inherited from Configurable).
type HardenedConfiguration = Configuration & {
  setNewBuiltinClassResolver(resolver: unknown): void;
};

const storybookService = __.newBean<{
  getPortalObject(appName: string): unknown;
  getResourceTemplateLoader(appName: string): unknown;
}>("no.item.storybook.freemarker.StorybookScriptBean");

export function render(params: RenderParams, model: Record<string, unknown>, templateErrors?: TemplateErrors): string {
  const configuration = getConfiguration() as HardenedConfiguration;
  // Deny FreeMarker's `?new` built-in. The dev-mode gate is the real control; this is defence in
  // depth that closes the reflective path from a caller-supplied template to arbitrary code.
  configuration.setNewBuiltinClassResolver(TemplateClassResolver.ALLOWS_NOTHING_RESOLVER);
  configuration.setTemplateExceptionHandler(templateErrors ?? TemplateExceptionHandler.HTML_DEBUG_HANDLER);
  configuration.setTemplateLoader(storybookService.getResourceTemplateLoader(params.xpAppName));
  configuration.setSharedVariable("portal", storybookService.getPortalObject(params.xpAppName));

  if (params.type === "file") {
    return renderFreemarker(params.filePath, model);
  } else {
    return renderFreemarker(params.template, model, params.name);
  }
}
