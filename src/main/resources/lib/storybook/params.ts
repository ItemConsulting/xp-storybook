import { split } from "/lib/storybook/utils";
import {
  type Component,
  type ComponentDescriptor,
  type RenderParams,
  findRegions,
  getRegionComponents,
  isComponentDescriptor,
  isRenderParams,
} from "/lib/storybook/regions";
import { deserializeJsonEntries, isJsonString, parseMatchers } from "/lib/storybook/deserializing";

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
    const value = isJsonString(rec[key]!) ? JSON.parse(rec[key] ?? "{}") : rec[key];

    if (isComponentDescriptor(key) && isRenderParams(value)) {
      res[key] = value;
    }

    return res;
  }, {});
}
