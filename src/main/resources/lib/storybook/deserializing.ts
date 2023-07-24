import { LocalDateTime, ZonedDateTime } from "/lib/time";
import { pick, traverse } from "/lib/storybook/utils";

export function deserializeJsonEntries(rec: Record<string, string | undefined>): Record<string, unknown> {
  const { javaTypes, matchers, ...params } = rec;
  const parsedJavaTypes: Record<string, string> = JSON.parse(javaTypes ?? "{}");
  const parsedMatchers = createMatchers(JSON.parse(matchers ?? "{}"));

  return traverse(params, (key, value, path) => {
    const javaType = pick(parsedJavaTypes, path) ?? matchForJavaType(key, parsedMatchers);

    if (typeof value === "string" && isJsonString(value)) {
      try {
        return JSON.parse(value);
      } catch (e) {
        log.warning(`Could not parse "${key}" as JSON: ` + value);
      }
    } else if (javaType && typeof value === "string") {
      return deserializeJavaObjects(value, javaType);
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

export function createMatchers(matchers: Record<string, string>): Record<string, RegExp> {
  const res: Record<string, RegExp> = {};

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
    case "ZonedDateTime":
      return ZonedDateTime.parse(value);
    case "LocalDateTime":
      return LocalDateTime.parse(value);
    case "Number":
      return Number.parseInt(value);
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
