import { render, type FreemarkerView } from "/lib/tineikt/freemarker";
import type { Request, Response } from "@item-enonic-types/global/controller";
import type { ResourceKey } from "@enonic-types/lib-export";

interface ResourceKeyConstructor {
  from(uri: string): ResourceKey;
}

const ResourceKey = Java.type<ResourceKeyConstructor>("com.enonic.xp.resource.ResourceKey");

const HEADERS = {
  "Access-Control-Allow-Origin": "*",
};

export function all(req: Request): Response<string> {
  try {
    const model = JSON.parse(req.params.model ?? "{}");
    const view: FreemarkerView = req.params.template
      ? {
          template: req.params.template,
          baseDirPath: req.params.baseDirPath,
        }
      : req.params.filePath
      ? {
          filePath: req.params.filePath,
          baseDirPath: req.params.baseDirPath,
        }
      : getResourceKeyByPath(req);

    return {
      status: 200,
      body: render(view, model),
      headers: HEADERS,
    };
  } catch (e) {
    return {
      status: 500,
      body: `<pre style="white-space: pre-wrap;">${e.message}</pre>`,
      headers: HEADERS,
    };
  }
}

function getResourceKeyByPath(req: Request): ResourceKey {
  const filePath = getSubstringAfter(req.rawPath, "freemarker-preview");

  if (req.params.appName) {
    return ResourceKey.from(`${req.params.appName}:${filePath}`);
  } else {
    throw `You need to send query parameter "appName" to to specify which application the resource is in.`;
  }
}

function getSubstringAfter(str: string, separator: string) {
  return str.slice(str.indexOf(separator) + separator.length);
}
