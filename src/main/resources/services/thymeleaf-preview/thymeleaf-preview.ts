import { parseMatchers, deserializeJsonEntries } from "/lib/storybook/deserializing";
import { isFile, isTextTemplate, render, type File, type TextTemplate } from "/lib/storybook/thymeleaf";
import { split } from "/lib/storybook/utils";
import {
  findRegions,
  getRegionComponents,
  isRegion,
  type ComponentDescriptor,
  type Component,
  isComponentDescriptor,
} from "/lib/storybook/regions";
import type { Request, Response } from "@item-enonic-types/global/controller";

type ViewMap = Record<ComponentDescriptor, TextTemplate | File>;

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
    const { view, views, model, components } = parseParams(req.params);

    if (view) {
      const renderedBody = render(view, model);
      const body = components.reduce(
        (str, component) => insertChildComponents(str, views, component, model, model.locale),
        renderedBody
      );

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

type ParsedParams = {
  view?: File | TextTemplate;
  views: Record<ComponentDescriptor, TextTemplate | File>;
  model: Record<string, unknown>;
  components: Component[];
};

function parseParams(params: Request["params"]): ParsedParams {
  const { template, baseDirPath, filePath, javaTypes, matchers, ...extra } = params;
  const [views, rawModel] = split(extra, (value, key) => isComponentDescriptor(key));
  const parsedMatchers = parseMatchers(JSON.parse(matchers ?? "{}"));
  const parsedJavaTypes = JSON.parse(javaTypes ?? "{}");
  const model = deserializeJsonEntries(rawModel, parsedMatchers, parsedJavaTypes);

  return {
    view: template
      ? {
          template,
          baseDirPath,
        }
      : filePath && baseDirPath
      ? {
          filePath,
          baseDirPath,
        }
      : undefined,
    views: parseViews(views),
    model,
    components: parsedMatchers.region ? getRegionComponents(findRegions(model, parsedMatchers.region)) : [],
  };
}

function parseViews(rec: Record<string, string | undefined>): ViewMap {
  return Object.keys(rec).reduce<ViewMap>((res, key) => {
    const value = JSON.parse(rec[key] ?? "{}");

    if (isComponentDescriptor(key) && (isTextTemplate(value) || isFile(value))) {
      res[key] = value;
    }

    return res;
  }, {});
}

function insertChildComponents(
  body: string,
  views: Record<ComponentDescriptor, TextTemplate | File>,
  component: Component,
  model: Record<string, unknown>,
  locale?: unknown
): string {
  const view = views[component.descriptor];

  if (view === undefined) {
    log.warning(`You have not specified a template for the descriptor: "${component.descriptor}"`);
    return body;
  }

  const renderedBody = body.replace(
    `<!--# COMPONENT ${component.path} -->`,
    render(view, {
      locale,
      ...component.config,
    })
  );

  if (component.type !== "layout") {
    return renderedBody;
  } else {
    return getRegionComponents(Object.values(component.config).filter(isRegion)).reduce(
      (str, comp) => insertChildComponents(str, views, comp, model, locale),
      renderedBody
    );
  }
}
