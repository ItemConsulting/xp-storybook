import { parseMatchers, deserializeJsonEntries } from "/lib/storybook/deserializing";
import { renderFile, renderInlineTemplate } from "/lib/storybook/thymeleaf";
import { split } from "/lib/storybook/utils";
import {
  findRegions,
  getRegionComponents,
  isRegion,
  isComponentDescriptor,
  type ComponentDescriptor,
  type Component,
} from "/lib/storybook/regions";
import type { Request, Response } from "@item-enonic-types/global/controller";

type RenderParams = { template: string; filePath?: string } | { template?: never; filePath: string };
type ViewMap = Record<ComponentDescriptor, RenderParams>;

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
    const parsedParams = parseParams(req.params);
    const { template, filePath, model, components, views } = parsedParams;
    const renderParams = { filePath, template };

    if (isRenderParams(renderParams)) {
      const renderedBody = render(renderParams, model);
      const body = components.reduce(
        (str, component) => insertChildComponents(str, views, component, model, model.locale),
        renderedBody,
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
  template?: string;
  filePath?: string;
  views: ViewMap;
  model: Record<string, unknown>;
  components: Component[];
};

function parseParams(params: Request["params"]): ParsedParams {
  const { template, filePath, javaTypes, matchers, ...extra } = params;
  const [views, rawModel] = split(extra, (value, key) => isComponentDescriptor(key));
  const parsedMatchers = parseMatchers(JSON.parse(matchers ?? "{}"));
  const parsedJavaTypes = JSON.parse(javaTypes ?? "{}");
  const model = deserializeJsonEntries(rawModel, parsedMatchers, parsedJavaTypes);

  return {
    template,
    filePath,
    views: parseViews(views),
    model,
    components: parsedMatchers.region ? getRegionComponents(findRegions(model, parsedMatchers.region)) : [],
  };
}

function parseViews(rec: Record<string, string | undefined>): ViewMap {
  return Object.keys(rec).reduce<ViewMap>((res, key) => {
    const value = JSON.parse(rec[key] ?? "{}");

    if (isComponentDescriptor(key) && isRenderParams(value)) {
      res[key] = value;
    }

    return res;
  }, {});
}

function insertChildComponents(
  body: string,
  views: ViewMap,
  component: Component,
  model: Record<string, unknown>,
  locale?: unknown,
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
    }),
  );

  if (component.type !== "layout") {
    return renderedBody;
  } else {
    return getRegionComponents(Object.values(component.config).filter(isRegion)).reduce(
      (str, comp) => insertChildComponents(str, views, comp, model, locale),
      renderedBody,
    );
  }
}

function render<T = unknown>(view: RenderParams, model: T): string {
  if ("template" in view && typeof view.template === "string") {
    return renderInlineTemplate<T>(view.template, model);
  } else {
    return renderFile<T>(view.filePath, model);
  }
}

function isRenderParams(value: unknown): value is RenderParams {
  const params = value as RenderParams;

  return params.template !== undefined || params.filePath !== undefined;
}
