import { split } from "/lib/storybook/utils";
import {
  type Component,
  type ComponentDescriptor,
  findRegions,
  getRegionComponents,
  isComponentDescriptor,
} from "/lib/storybook/regions";
import { deserializeJsonEntries, isJsonString, parseMatchers } from "/lib/storybook/deserializing";

export type FileRenderParams = {
  type: "file";
  filePath: string;
  xpResourcesDirPath: string;
  xpAppName: string;
};

export type InlineRenderParams = {
  type: "inline";
  template: string;
  name: string;
  xpResourcesDirPath: string;
  xpAppName: string;
};

export type RenderParams = FileRenderParams | InlineRenderParams;

export type ViewMap = Record<ComponentDescriptor, RenderParams>;

export type ParsedParams = {
  template?: string;
  views: ViewMap;
  model: Record<string, unknown>;
  components: Component[];
  xpResourcesDirPath: string;
  xpAppName?: string;
};

export function parseParams(params: Record<string, string>): ParsedParams {
  const { template, javaTypes, matchers, ...extra } = params;
  const [views, rawModel] = split(extra, (value, key) => isComponentDescriptor(key));
  const parsedMatchers = parseMatchers(JSON.parse(matchers ?? "{}"));
  const parsedJavaTypes = JSON.parse(javaTypes ?? "{}");
  const model = deserializeJsonEntries(rawModel, parsedMatchers, parsedJavaTypes);

  return {
    template,
    views: parseViews(views, params.xpResourcesDirPath, params.xpAppName),
    model,
    components: parsedMatchers.region ? getRegionComponents(findRegions(model, parsedMatchers.region)) : [],
    xpResourcesDirPath: params.xpResourcesDirPath ?? app.config.xpResourcesDirPath,
    xpAppName: params.xpAppName,
  };
}

function parseViews(rec: Record<string, string | undefined>, xpResourcesDirPath: string, xpAppName: string): ViewMap {
  return Object.keys(rec).reduce<ViewMap>((res, key) => {
    const value: RenderParams = extractInlineTemplate(rec[key], key, xpResourcesDirPath, xpAppName) ?? {
      type: "file",
      filePath: rec[key],
      xpResourcesDirPath,
      xpAppName,
    };

    if (isComponentDescriptor(key)) {
      res[key] = value;
    }

    return res;
  }, {});
}

function extractInlineTemplate(
  str: string,
  key: string,
  xpResourcesDirPath: string,
  xpAppName: string,
): InlineRenderParams | undefined {
  if (!isJsonString(str)) {
    return undefined;
  }

  const parsed = JSON.parse(str) as { template?: string };

  return parsed.template
    ? { type: "inline", template: parsed.template, name: key, xpResourcesDirPath, xpAppName }
    : undefined;
}
