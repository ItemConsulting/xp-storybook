import { endsWith, traverse } from "/lib/storybook/utils";
import { LocalDate, LocalDateTime, ZonedDateTime } from "/lib/time";

const UN_JSONIFIED_OBJECT_STRING = "[object Object]";

/** Model keys ending in this are regions, whose components are collected for rendering. */
export const REGION_KEY_SUFFIX = "Region";

const DIGITS = /^\d+$/;
const LOCAL_DATE = /^\d{4}-\d{2}-\d{2}$/;
const LOCAL_DATE_TIME = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2}(\.\d+)?)?$/;
const ZONED_DATE_TIME = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2}(\.\d+)?)?(Z|[+-]\d{2}:\d{2})(\[[^\]]+\])?$/;

export function deserializeJsonEntries(params: Record<string, string | undefined>): Record<string, unknown> {
  return traverse(params, (key, value) => (typeof value === "string" ? deserializeByKey(key, value) : value));
}

/**
 * Deserializes a value going by its key: `…Date` is a date of whatever kind its shape
 * says, and `…Count` or `…Number` is an integer if it is all digits. Anything else, a `…Region` included, is parsed if it
 * looks like JSON.
 */
function deserializeByKey(key: string, value: string): unknown {
  if (endsWith(key, "Date")) {
    return parseDate(key, value);
  } else if ((endsWith(key, "Count") || endsWith(key, "Number")) && DIGITS.test(value)) {
    return parseInt(value, 10);
  } else if (isJsonString(value)) {
    try {
      return JSON.parse(value);
    } catch {
      log.warning(`Could not parse "${key}" as JSON: ${value}`);
    }
  }
  return value;
}

function parseDate(key: string, value: string): unknown {
  if (LOCAL_DATE.test(value)) {
    return LocalDate.parse(value);
  } else if (LOCAL_DATE_TIME.test(value)) {
    return LocalDateTime.parse(value);
  } else if (ZONED_DATE_TIME.test(value)) {
    return ZonedDateTime.parse(value);
  }

  log.warning(`"${key}" ends in "Date", but "${value}" is not an ISO date, date-time or zoned date-time`);
  return value;
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
