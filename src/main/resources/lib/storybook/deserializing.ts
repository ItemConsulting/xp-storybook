import { endsWith, traverse } from "/lib/storybook/utils";
import { LocalDate, LocalDateTime, ZonedDateTime } from "/lib/time";

const UN_JSONIFIED_OBJECT_STRING = "[object Object]";

/** Model keys ending in this are regions, whose components are collected for rendering. */
export const REGION_KEY_SUFFIX = "Region";

const INTEGER = /^-?\d+$/;
const LOCAL_DATE = /^\d{4}-\d{2}-\d{2}$/;
const LOCAL_DATE_TIME = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2}(\.\d+)?)?$/;
const ZONED_DATE_TIME = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2}(\.\d+)?)?(Z|[+-]\d{2}:\d{2})(\[[^\]]+\])?$/;

export function deserializeJsonEntries(params: Record<string, string | undefined>): Record<string, unknown> {
  return traverse(params, (key, value) => (typeof value === "string" ? deserializeByKey(key, value) : value));
}

/**
 * Deserializes a value going by its key: `…Date` is a date of whatever kind its shape says, and `…Count` or `…Int` is
 * an integer if it is one. Anything else, a `…Region` included, is parsed if it looks like JSON.
 *
 * A value that cannot be parsed, like the date-shaped "2024-02-30", is kept as a string, so one bad value never fails
 * the whole preview.
 */
function deserializeByKey(key: string, value: string): unknown {
  const isDate = endsWith(key, "Date");

  try {
    if (isDate && LOCAL_DATE.test(value)) {
      return LocalDate.parse(value);
    } else if (isDate && LOCAL_DATE_TIME.test(value)) {
      return LocalDateTime.parse(value);
    } else if (isDate && ZONED_DATE_TIME.test(value)) {
      return ZonedDateTime.parse(value);
    } else if ((endsWith(key, "Count") || endsWith(key, "Int")) && INTEGER.test(value)) {
      return parseInt(value, 10);
    } else if (isJsonString(value)) {
      return JSON.parse(value);
    }
  } catch (e) {
    log.warning(`Could not parse "${key}": ${value} (${e})`);
    return value;
  }

  if (isDate) {
    log.warning(`"${key}" ends in "Date", but "${value}" is not an ISO date, date-time or zoned date-time`);
  }
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
