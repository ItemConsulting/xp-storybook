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
};

export type InlineRenderParams = {
  type: "inline";
  template: string;
  name: string;
};

export type RenderParams = FileRenderParams | InlineRenderParams;

export type ViewMap = Record<ComponentDescriptor, RenderParams>;

export type ParsedParams = {
  template?: string;
  views: ViewMap;
  model: Record<string, unknown>;
  components: Component[];
};

export function parseParams(params: Record<string, string>): ParsedParams {
  const { template, javaTypes, matchers, ...extra } = params;
  const [views, rawModel] = split(extra, (value, key) => isComponentDescriptor(key));
  const parsedMatchers = parseMatchers(JSON.parse(matchers ?? "{}"));
  const parsedJavaTypes = JSON.parse(javaTypes ?? "{}");
  const model = deserializeJsonEntries(rawModel, parsedMatchers, parsedJavaTypes);

  return {
    template,
    views: parseViews(views),
    model,
    components: parsedMatchers.region ? getRegionComponents(findRegions(model, parsedMatchers.region)) : [],
  };
}

function parseViews(rec: Record<string, string | undefined>): ViewMap {
  return Object.keys(rec).reduce<ViewMap>((res, key) => {
    const value = extractInlineTemplate(rec[key], key) ?? { type: "file", filePath: rec[key] };

    if (isComponentDescriptor(key)) {
      res[key] = value;
    }

    return res;
  }, {});
}

function extractInlineTemplate(str: string, key: string): InlineRenderParams | undefined {
  if (!isJsonString(str)) {
    return undefined;
  }

  const parsed = JSON.parse(str) as { template?: string };

  return parsed.template ? { type: "inline", template: parsed.template, name: key } : undefined;
}
