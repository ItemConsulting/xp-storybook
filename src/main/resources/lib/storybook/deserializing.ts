import { LocalDateTime, ZonedDateTime } from "/lib/time";
import { pick, traverse } from "/lib/storybook/utils";

export type MatcherMap = Record<string, RegExp>;

export function deserializeJsonEntries(
  params: Record<string, string | undefined>,
  parsedMatchers: MatcherMap,
  parsedJavaTypes: Record<string, string>
): Record<string, unknown> {
  return traverse(params, (key, value, path) => {
    const javaType = pick(parsedJavaTypes, path) ?? matchForJavaType(key, parsedMatchers);
    if (javaType && typeof value === "string") {
      return deserializeJavaObjects(value, javaType);
    } else if (typeof value === "string" && isJsonString(value)) {
      try {
        return JSON.parse(value);
      } catch (e) {
        log.warning(`Could not parse "${key}" as JSON: ` + value);
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
    case "zonedDateTime":
      return ZonedDateTime.parse(value);
    case "localDateTime":
      return LocalDateTime.parse(value);
    case "number":
      return Number.parseInt(value);
    case "string":
    default:
      return value;
  }
}

export function isJsonString(str: string): boolean {
  return isArrayString(str) || isObjectString(str) || isBooleanString(str) || isNullString(str);
}

function isArrayString(str: string): boolean {
  return str[0] == "[" && str[1] !== "#" && str[str.length - 1] === "]";
}

function isObjectString(str: unknown): boolean {
  return typeof str === "string" && str[0] == "{" && str[str.length - 1] === "}";
}

function isBooleanString(str: string): boolean {
  return str === "true" || str === "false";
}

function isNullString(str: string): boolean {
  return str === "null";
}
