import { createMatchers, deserializeJsonEntries } from "/lib/storybook/deserializing";
import { isFile, isTextTemplate, renderFile, renderInlineTemplate, type File, type TextTemplate } from "/lib/storybook";
import { flatMap } from "/lib/storybook/utils";
import type { Request, Response } from "@item-enonic-types/global/controller";
import type { Component, Region } from "@enonic-types/core";

const HEADERS = {
  "Access-Control-Allow-Origin": "*",
};

export function all(req: Request): Response<string> {
  if (app.config.iAmNotFoolishEnoughToDeployThisInProduction !== "true") {
    return {
      status: 401,
      headers: HEADERS,
    };
  }

  try {
    const { template, baseDirPath, filePath, ...params } = req.params;
    const model = deserializeJsonEntries(params);

    if (template) {
      return {
        status: 200,
        body: renderInlineTemplate({ template, baseDirPath }, model),
        headers: HEADERS,
      };
    } else if (filePath && baseDirPath) {
      const renderedBody = renderFile({ filePath, baseDirPath }, model);
      const components = flatMap(findRegions(req.params), (region) => region.components);

      const body = components.reduce((str, component) => insertChildComponents(str, component, model), renderedBody);

      return {
        status: 200,
        body,
        headers: HEADERS,
      };
    }

    return {
      status: 400,
      body: "You need to provide either <code>filePath</code> or <code>template</code> query params.",
      headers: HEADERS,
    };
  } catch (e) {
    log.error("Could not create Freemarker preview", e);
    return {
      status: 500,
      body: `<pre style="white-space: pre-wrap;">${e.message}</pre>`,
      headers: HEADERS,
    };
  }
}

function insertChildComponents(body: string, component: Component, model: Record<string, unknown>): string {
  const view = model[component.descriptor] as TextTemplate | File;

  if (view === undefined) {
    log.warning(`You have not specified a template for the descriptor: "${component.descriptor}"`);
    return body;
  }
  if (isTextTemplate(view)) {
    const renderedBody = body.replace(
      `<!--# COMPONENT ${component.path} -->`,
      renderInlineTemplate(view, component.config)
    );

    return getComponentsForLayout(component).reduce(
      (str, comp) => insertChildComponents(str, comp, model),
      renderedBody
    );
  } else if (isFile(view)) {
    const renderedBody = body.replace(`<!--# COMPONENT ${component.path} -->`, renderFile(view, component.config));

    return getComponentsForLayout(component).reduce(
      (str, comp) => insertChildComponents(str, comp, model),
      renderedBody
    );
  } else {
    log.warning(`The template you have specified for descriptor: "${component.descriptor}" is not valid`);
    return body;
  }
}

function isRegion(value: unknown): value is Region {
  const region = value as Region;

  return region?.components !== undefined;
}

function findRegions(rec: Record<string, string | undefined>): Array<Region> {
  const parsedMatchers = createMatchers(JSON.parse(rec.matchers ?? "{}"));
  const { Region } = parsedMatchers;

  return Region
    ? Object.keys(rec)
        .filter((key) => Region.test(key))
        .map((key) => JSON.parse(rec[key] ?? "{}"))
    : [];
}

function getComponentsForLayout(component: Component): Component[] {
  if (component.type === "layout") {
    return flatMap(
      Object.values(component.config as Record<string, unknown>).filter(isRegion),
      (region) => region.components
    );
  } else {
    return [];
  }
}
