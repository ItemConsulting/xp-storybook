import { render as renderFreemarker } from "/lib/storybook/freemarker";
import { render as renderThymeleaf } from "/lib/storybook/thymeleaf";
import { capitalize, substringAfter } from "/lib/storybook/utils";
import { insertChildComponents } from "/lib/storybook/regions";
import { parseParams } from "/lib/storybook/params";
import type { Request, Response } from "@item-enonic-types/global/controller";

const MODE_FREEMARKER = "freemarker";
const MODE_THYMELEAF = "thymeleaf";

type Mode = typeof MODE_FREEMARKER | typeof MODE_THYMELEAF;

const HEADERS = {
  "Access-Control-Allow-Origin": "*",
};

export function all(req: Request): Response<string> {
  if (app.config.iAmNotFoolishEnoughToDeployThisInProduction !== "true") {
    return {
      status: 401,
      headers: HEADERS,
    };
  } else if (app.config.xpResourcesDirPath === undefined) {
    return {
      status: 500,
      body: `Please configure "xpResourcesDirPath" in your "no.item.storybook.cfg".`,
      headers: HEADERS,
    };
  }

  const mode = resolveMode(req);

  try {
    const parsedParams = parseParams(req.params);
    const { template, model, components, views } = parsedParams;
    const id = substringAfter(req.path, "/storybook-preview/"); // TODO Make this work on Windows

    if (template || id) {
      const renderFn = mode === MODE_THYMELEAF ? renderThymeleaf : renderFreemarker;
      const renderedBody = renderFn(template ? { template } : id, model);
      const body = components.reduce(
        (str, component) => insertChildComponents(str, views, component, model, renderFn, model.locale),
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
      body: "You need to provide either <code>id</code> or <code>template</code> query params.",
      headers: HEADERS,
    };
  } catch (e) {
    log.error(`Could not create ${capitalize(mode)} preview`, e);
    return {
      status: 500,
      body: `<pre style="white-space: pre-wrap;">${e.message}</pre>`,
      headers: HEADERS,
    };
  }
}

function resolveMode(req: Request): Mode {
  if (req.params.renderMode === MODE_FREEMARKER || req.params.renderMode === MODE_THYMELEAF) {
    return req.params.renderMode;
  } else if (req.rawPath.endsWith(".ftl")) {
    return MODE_FREEMARKER;
  } else if (req.rawPath.endsWith(".html")) {
    return MODE_THYMELEAF;
  } else if (app.config.renderMode === MODE_FREEMARKER || app.config.renderMode === MODE_THYMELEAF) {
    return app.config.renderMode;
  }

  log.warning(
    `Can not resolve render mode. Use "renderMode={thymeleaf,freemarker}" query param to change. Defaulting to Freemarker.`,
  );
  return MODE_FREEMARKER;
}
