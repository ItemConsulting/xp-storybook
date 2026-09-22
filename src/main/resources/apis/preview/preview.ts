import type { Request, Response } from "@enonic-types/core";
import {
  getApplicationState,
  getInstalledApplicationNames,
  isServedFromJar,
  isValidApplicationKey,
  resourceExists,
  STATE_MISSING,
  STATE_STOPPED,
} from "/lib/storybook/app";
import { newTemplateErrors } from "/lib/storybook/errors";
import { render as renderFreemarker } from "/lib/storybook/freemarker";
import { RunMode } from "/lib/storybook/java";
import { isTemplatePath, parseParams, type RenderFn, type RenderParams } from "/lib/storybook/params";
import { insertChildComponents } from "/lib/storybook/regions";
import { render as renderThymeleaf } from "/lib/storybook/thymeleaf";
import { capitalize, endsWith, escapeHtml } from "/lib/storybook/utils";

const MODE_FREEMARKER = "freemarker";
const MODE_THYMELEAF = "thymeleaf";

type Mode = typeof MODE_FREEMARKER | typeof MODE_THYMELEAF;

// Storybook and the Vitest/Playwright runner always call from localhost. Echoing only loopback
// origins keeps an ordinary web page the developer happens to visit from reading what this
// endpoint renders — a caller-supplied template, which is equivalent to running caller-supplied
// code.
const LOOPBACK_ORIGIN = /^https?:\/\/(localhost|127\.0\.0\.1|\[::1\])(:\d+)?$/;

function corsHeaders(req: Request): Record<string, string> {
  const origin = req.getHeader("Origin");

  return origin && LOOPBACK_ORIGIN.test(origin) ? { "Access-Control-Allow-Origin": origin, Vary: "Origin" } : {};
}

type QueryParams = {
  xpAppName: string;
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
  }

  const application = checkApplication(req.params.xpAppName, HEADERS);

  if (application) {
    return application;
  }

  const mode = resolveMode(req);

  try {
    const parsedParams = parseParams(req.params as Record<string, string>);
    const { template, model, components, views, xpAppName } = parsedParams;
    const id = resolveTemplateId(req);

    if (id && !isTemplatePath(id)) {
      return {
        status: 400,
        contentType: "text/plain",
        body: `"${id}" is not a template. Only .ftl, .ftlh, .ftlx and .html files can be rendered.`,
        headers: HEADERS,
      };
    }

    if (id) {
      const missing = checkTemplateExists(xpAppName, id, HEADERS);

      if (missing) {
        return missing;
      }

      warnIfServedFromJar(xpAppName, id);
    }

    if (template || id) {
      // A failing template must not take the whole page down: both flavors render the error where the template
      // would have gone and collect it here, so the response can carry the markup that did render.
      const templateErrors = newTemplateErrors();
      const render: RenderFn = mode === MODE_THYMELEAF ? renderThymeleaf : renderFreemarker;
      const renderFn: RenderFn = (params, viewModel) => render(params, viewModel, templateErrors);
      const renderParams: RenderParams = template
        ? {
            type: "inline",
            template,
            // Only used to name the template in error messages, so it follows the flavor being rendered.
            name: mode === MODE_THYMELEAF ? "inline-storybook.html" : "inline-storybook.ftl",
            xpAppName,
          }
        : {
            type: "file",
            filePath: id,
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
      body: `<pre style="white-space: pre-wrap;">${escapeHtml((e as { message?: string }).message ?? String(e))}</pre>`,
      headers: HEADERS,
    };
  }
}

/** Reports a missing, misspelled or undeployed application as such, rather than as a template that was not found. */
function checkApplication(xpAppName: string | undefined, headers: Record<string, string>): Response | null {
  if (!xpAppName) {
    return {
      status: 400,
      contentType: "text/plain",
      body:
        `Missing required query parameter "xpAppName": the application whose resources hold the template. The ` +
        `Storybook framework reads it from "appName" in gradle.properties.`,
      headers,
    };
  }

  if (!isValidApplicationKey(xpAppName)) {
    return {
      status: 400,
      contentType: "text/plain",
      body: `"${xpAppName}" is not a valid application key.`,
      headers,
    };
  }

  const state = getApplicationState(xpAppName);

  if (state === STATE_MISSING) {
    return {
      status: 404,
      contentType: "text/plain",
      body:
        `Application "${xpAppName}" is not installed on this server. Run "enonic project dev" in the project, ` +
        `or check that "appName" in gradle.properties names the application you deployed.\n\nInstalled ` +
        `applications: ${getInstalledApplicationNames()}`,
      headers,
    };
  }

  if (state === STATE_STOPPED) {
    return {
      status: 404,
      contentType: "text/plain",
      body: `Application "${xpAppName}" is installed but not started. Check server.log for why it failed to start.`,
      headers,
    };
  }

  return null;
}

/**
 * Checked for the template that was asked for, and only that one. A child component that is missing keeps being
 * reported per component through the error collector, so that its siblings still render.
 */
function checkTemplateExists(xpAppName: string, id: string, headers: Record<string, string>): Response | null {
  if (resourceExists(xpAppName, id)) {
    return null;
  }

  return {
    status: 404,
    contentType: "text/plain",
    body:
      `"${xpAppName}:/${id}" does not exist. In development mode Enonic XP serves an application's resources from ` +
      `its source directory, so check the path, and that the application has been deployed since the file was added.`,
    headers,
  };
}

/**
 * A template served from the installed jar rather than from a source directory will not reflect edits on disk. The
 * usual cause is an application deployed without dev source paths, but a template that ships inside a bundled
 * library or a Market application is always served this way — both are legitimate, so this only warrants a warning.
 */
function warnIfServedFromJar(xpAppName: string, id: string): void {
  if (isServedFromJar(xpAppName, id)) {
    log.warning(
      `"${xpAppName}:/${id}" is served from the installed jar, so edits to it will not appear until the ` +
        `application is rebuilt. If it is a template of your own, run "enonic project dev" in that project, or ` +
        `build it with the "env=dev" Gradle property, so the application carries its source paths.`,
    );
  }
}

/**
 * The path of the template to render, relative to the root of `xpAppName`'s resources — that is, an XP resource
 * path.
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

/**
 * Which template engine renders the request, taken from the extension of the path.
 *
 * An inline template has no extension of its own, which is why a story that uses one still names the template file it
 * imports: the path it is requested under is what picks the flavor.
 */
function resolveMode(req: Request<{ params: QueryParams }>): Mode {
  if (endsWith(req.rawPath, ".ftl") || endsWith(req.rawPath, ".ftlh") || endsWith(req.rawPath, ".ftlx")) {
    return MODE_FREEMARKER;
  } else if (endsWith(req.rawPath, ".html")) {
    return MODE_THYMELEAF;
  }

  log.warning(
    `Could not tell which template engine to use from "${req.rawPath}", so FreeMarker was assumed. Request the ` +
      `template under a path ending in .ftl, .ftlh, .ftlx or .html — for an inline template, that means naming the ` +
      `template file the story imports.`,
  );
  return MODE_FREEMARKER;
}
