import { deserializeJsonEntries, isJsonString, parseMatchers } from "/lib/storybook/deserializing";
import type { TemplateErrors } from "/lib/storybook/errors";
import {
  type Component,
  type ComponentDescriptor,
  findRegions,
  getRegionComponents,
  isComponentDescriptor,
} from "/lib/storybook/regions";
import { endsWith, filterObject, split } from "/lib/storybook/utils";

/**
 * Extensions this app is willing to read off disk.
 *
 * The caller chooses `xpResourcesDirPath`, so without this restriction the endpoint would render —
 * and therefore disclose — any file the XP process can read. A template engine emits a file with no
 * directives verbatim, which makes an unrestricted read equivalent to an arbitrary file read.
 */
const TEMPLATE_EXTENSIONS = [".ftl", ".ftlh", ".ftlx", ".html"];

/** Whether `path` names a template this app may load from disk. */
export function isTemplatePath(path: string): boolean {
  const lowerCased = path.toLowerCase();

  return TEMPLATE_EXTENSIONS.some((extension) => endsWith(lowerCased, extension));
}

export type FileRenderParams = {
  type: "file";
  filePath: string;
  xpResourcesDirPath: string;
  xpAppName?: string;
};

export type InlineRenderParams = {
  type: "inline";
  template: string;
  name: string;
  xpResourcesDirPath: string;
  xpAppName?: string;
};

export type RenderParams = FileRenderParams | InlineRenderParams;

export type RenderFn = (
  params: RenderParams,
  model: Record<string, unknown>,
  templateErrors?: TemplateErrors,
) => string;

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
  const { template, javaTypes, matchers } = params;
  // Deliberately not object rest (`...extra`): the bundler lowers it with a helper that calls
  // Array.prototype.includes, which Nashorn does not have. See tsdown.config.mts.
  const extra = filterObject(params, (_value, key) => key !== "template" && key !== "javaTypes" && key !== "matchers");
  const [views, rawModel] = split(extra, (_value, key) => isComponentDescriptor(key));
  const parsedMatchers = parseMatchers(JSON.parse(matchers ?? "{}"));
  const parsedJavaTypes = JSON.parse(javaTypes ?? "{}");
  const model = deserializeJsonEntries(rawModel, parsedMatchers, parsedJavaTypes);

  return {
    template,
    views: parseViews(views, params.xpResourcesDirPath, params.xpAppName),
    model,
    components: parsedMatchers.region ? getRegionComponents(findRegions(model, parsedMatchers.region)) : [],
    xpResourcesDirPath: params.xpResourcesDirPath,
    xpAppName: params.xpAppName,
  };
}

function parseViews(rec: Record<string, string>, xpResourcesDirPath: string, xpAppName?: string): ViewMap {
  return Object.keys(rec).reduce<ViewMap>((res, key) => {
    if (!isComponentDescriptor(key)) {
      return res;
    }

    const inline = extractInlineTemplate(rec[key], key, xpResourcesDirPath, xpAppName);

    if (inline) {
      res[key] = inline;
    } else if (isTemplatePath(rec[key])) {
      res[key] = {
        type: "file",
        filePath: rec[key],
        xpResourcesDirPath,
        xpAppName,
      };
    } else {
      log.warning(`Ignoring view "${key}": "${rec[key]}" is not a template file (${TEMPLATE_EXTENSIONS.join(", ")}).`);
    }

    return res;
  }, {});
}

function extractInlineTemplate(
  str: string,
  key: string,
  xpResourcesDirPath: string,
  xpAppName?: string,
): InlineRenderParams | undefined {
  if (!isJsonString(str)) {
    return undefined;
  }

  const parsed = JSON.parse(str) as { template?: string };

  return parsed.template
    ? { type: "inline", template: parsed.template, name: key, xpResourcesDirPath, xpAppName }
    : undefined;
}
