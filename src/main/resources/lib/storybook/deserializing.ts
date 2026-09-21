import { pick, traverse } from "/lib/storybook/utils";
import { LocalDate, LocalDateTime, ZonedDateTime } from "/lib/time";

const UN_JSONIFIED_OBJECT_STRING = "[object Object]";

export type MatcherMap = Record<string, RegExp>;

/** javaTypes mirror the shape of the model, so a nested key is addressed by its path. */
export type JavaTypeMap = { [key: string]: string | JavaTypeMap };

export function deserializeJsonEntries(
  params: Record<string, string | undefined>,
  parsedMatchers: MatcherMap,
  parsedJavaTypes: JavaTypeMap,
): Record<string, unknown> {
  return traverse(params, (key, value, path) => {
    const javaType = pick(parsedJavaTypes, path) ?? matchForJavaType(key, parsedMatchers);
    if (typeof javaType === "string" && typeof value === "string") {
      return deserializeJavaObjects(value, javaType);
    } else if (typeof value === "string" && isJsonString(value)) {
      try {
        return JSON.parse(value);
      } catch {
        log.warning(`Could not parse "${key}" as JSON: ${value}`);
      }
    }
    return value;
  });
}

function matchForJavaType(key: string, parsedMatchers: Record<string, RegExp>): string | undefined {
  for (const matcherKey in parsedMatchers) {
    if (parsedMatchers[matcherKey].test(key)) {
      return matcherKey;
    }
  }

  return undefined;
}

export function parseMatchers(matchers: Record<string, string>): MatcherMap {
  const res: MatcherMap = {};

  for (const key in matchers) {
    const regex = stringToRegex(matchers[key]);
    if (regex) {
      res[key] = regex;
    }
  }

  return res;
}

function stringToRegex(str: string): RegExp | undefined {
  const re = /\/(.+)\/([gim]?)/;
  const match = str.match(re);
  if (match) {
    return new RegExp(match[1], match[2]);
  }
  return undefined;
}

export function deserializeJavaObjects(value: string, type: string): unknown {
  switch (type) {
    case "localDate":
      return LocalDate.parse(value);
    case "zonedDateTime":
      return ZonedDateTime.parse(value);
    case "localDateTime":
      return LocalDateTime.parse(value);
    case "number":
      return parseInt(value, 10);
    case "region":
      return JSON.parse(value);
    // `type` is a plain string, so this case is the only place documenting "string" as supported.
    // biome-ignore lint/complexity/noUselessSwitchCase: documents a supported value
    case "string":
    default:
      return value;
  }
}

export function isJsonString(str: string): boolean {
  return isArrayString(str) || isObjectString(str) || isBooleanString(str) || isNullString(str);
}

function isArrayString(str: string): boolean {
  return str !== UN_JSONIFIED_OBJECT_STRING && str[0] === "[" && str[1] !== "#" && str[str.length - 1] === "]";
}

function isObjectString(str: unknown): boolean {
  return typeof str === "string" && str[0] === "{" && str[str.length - 1] === "}";
}

function isBooleanString(str: string): boolean {
  return str === "true" || str === "false";
}

function isNullString(str: string): boolean {
  return str === "null";
}
