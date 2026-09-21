import type { Request, Response } from "@enonic-types/core";
import { newTemplateErrors, render as renderFreemarker } from "/lib/storybook/freemarker";
import { RunMode } from "/lib/storybook/java";
import { isTemplatePath, parseParams, type RenderFn, type RenderParams } from "/lib/storybook/params";
import { insertChildComponents } from "/lib/storybook/regions";
import { render as renderThymeleaf } from "/lib/storybook/thymeleaf";
import { capitalize, endsWith } from "/lib/storybook/utils";

const MODE_FREEMARKER = "freemarker";
const MODE_THYMELEAF = "thymeleaf";

type Mode = typeof MODE_FREEMARKER | typeof MODE_THYMELEAF;

// Storybook and the Vitest/Playwright runner always call from localhost. Echoing only loopback
// origins keeps an ordinary web page the developer happens to visit from reading what this
// endpoint renders — which, since the caller chooses xpResourcesDirPath, is any file the XP
// process can read.
const LOOPBACK_ORIGIN = /^https?:\/\/(localhost|127\.0\.0\.1|\[::1\])(:\d+)?$/;

function corsHeaders(req: Request): Record<string, string> {
  const origin = req.getHeader("Origin");

  return origin && LOOPBACK_ORIGIN.test(origin) ? { "Access-Control-Allow-Origin": origin, Vary: "Origin" } : {};
}

type QueryParams = {
  xpAppName: string;
  xpResourcesDirPath: string;
  renderMode: string;
  template: string;
  javaTypes: string;
  matchers: string;
};

export function all(req: Request<{ params: QueryParams }>): Response {
  const HEADERS = corsHeaders(req);

  // 403 rather than 401: XP hands a 401 to the bound ID provider's handle401 hook, which may
  // replace this body with a login page that then renders into the story canvas.
  if (!RunMode.isDev()) {
    return {
      status: 403,
      contentType: "text/plain",
      body:
        `${app.name} only renders templates when Enonic XP runs in development mode, and this ` +
        `server is in production mode. Sandboxes start in dev mode by default ("enonic sandbox ` +
        `start"; --prod disables it). This app renders arbitrary templates from disk and must ` +
        `never run in production.`,
      headers: HEADERS,
    };
  } else if (req.params.xpResourcesDirPath === undefined) {
    return {
      status: 400,
      contentType: "text/plain",
      body: `Missing required query parameter "xpResourcesDirPath": the directory to resolve templates from.`,
      headers: HEADERS,
    };
  }

  const mode = resolveMode(req);

  try {
    const parsedParams = parseParams(req.params as Record<string, string>);
    const { template, model, components, views, xpResourcesDirPath, xpAppName } = parsedParams;
    const id = resolveTemplateId(req);

    // The caller chooses xpResourcesDirPath, so only template files may be loaded from it —
    // otherwise any readable file could be rendered back, verbatim, to whoever asked.
    if (id && !isTemplatePath(id)) {
      return {
        status: 400,
        contentType: "text/plain",
        body: `"${id}" is not a template. Only .ftl, .ftlh, .ftlx and .html files can be rendered.`,
        headers: HEADERS,
      };
    }

    if (template || id) {
      // FreeMarker renders its errors into the output rather than throwing, so they are collected on the side.
      const templateErrors = newTemplateErrors();
      const render: RenderFn = mode === MODE_THYMELEAF ? renderThymeleaf : renderFreemarker;
      const renderFn: RenderFn = (params, viewModel) => render(params, viewModel, templateErrors);
      const renderParams: RenderParams = template
        ? {
            type: "inline",
            template,
            name: "inline-storybook.ftl",
            xpResourcesDirPath,
            xpAppName,
          }
        : {
            type: "file",
            filePath: id,
            xpResourcesDirPath,
            xpAppName,
          };

      const renderedBody = renderFn(renderParams, model);

      const body = components.reduce(
        (str, component) => insertChildComponents(str, views, component, model, renderFn, model.locale),
        renderedBody,
      );

      if (templateErrors.hasErrors()) {
        log.error(`Could not create ${capitalize(mode)} preview\n${templateErrors.getMessage()}`);

        // The body still holds the rendered output, with the errors rendered in place.
        return {
          status: 500,
          body,
          headers: HEADERS,
        };
      }

      return {
        status: 200,
        body,
        headers: HEADERS,
      };
    }

    return {
      status: 400,
      body: "Provide a template path after the API name, or a <code>template</code> query param.",
      headers: HEADERS,
    };
  } catch (e) {
    log.error(`Could not create ${capitalize(mode)} preview`, e);
    return {
      status: 500,
      body: `<pre style="white-space: pre-wrap;">${(e as { message?: string }).message}</pre>`,
      headers: HEADERS,
    };
  }
}

/**
 * The path of the template to render, relative to `xpResourcesDirPath`.
 *
 * A Universal API receives everything after its descriptor segment as the request subpath, so the
 * app and API names are stripped via `contextPath` rather than hardcoded.
 */
function resolveTemplateId(req: Request<{ params: QueryParams }>): string {
  const contextPath = req.contextPath;
  const subPath =
    contextPath && req.path.indexOf(contextPath) === 0 ? req.path.substring(contextPath.length) : req.path;

  return subPath.replace(/^\/+/, "");
}

function resolveMode(req: Request<{ params: QueryParams }>): Mode {
  if (req.params.renderMode === MODE_FREEMARKER || req.params.renderMode === MODE_THYMELEAF) {
    return req.params.renderMode;
  } else if (endsWith(req.rawPath, ".ftl") || endsWith(req.rawPath, ".ftlh") || endsWith(req.rawPath, ".ftlx")) {
    return MODE_FREEMARKER;
  } else if (endsWith(req.rawPath, ".html")) {
    return MODE_THYMELEAF;
  }

  log.warning(
    `Can not resolve render mode. Use "renderMode={thymeleaf,freemarker}" query param to change. Defaulting to Freemarker.`,
  );
  return MODE_FREEMARKER;
}
