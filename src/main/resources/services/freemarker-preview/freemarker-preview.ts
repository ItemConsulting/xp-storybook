import { deserializeJsonEntries } from "/lib/storybook/deserializing";
import { renderFile, renderInlineTemplate } from "/lib/storybook";
import type { Request, Response } from "@item-enonic-types/global/controller";

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
      return {
        status: 200,
        body: renderFile({ filePath, baseDirPath }, model),
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
