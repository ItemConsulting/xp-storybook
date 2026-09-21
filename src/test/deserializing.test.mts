import { describe, it } from "node:test";
import {
  deserializeJavaObjects,
  deserializeJsonEntries,
  isJsonString,
  parseMatchers,
} from "/lib/storybook/deserializing";
import { expect } from "./expect.mts";

describe("isJsonString", () => {
  it("accepts arrays, objects, booleans and null", () => {
    expect(isJsonString("[1,2]")).toBe(true);
    expect(isJsonString('{"a":1}')).toBe(true);
    expect(isJsonString("true")).toBe(true);
    expect(isJsonString("false")).toBe(true);
    expect(isJsonString("null")).toBe(true);
  });

  it("rejects plain strings and numbers", () => {
    expect(isJsonString("hello")).toBe(false);
    expect(isJsonString("42")).toBe(false);
    expect(isJsonString("")).toBe(false);
  });

  it('rejects a stringified object that lost its content ("[object Object]")', () => {
    expect(isJsonString("[object Object]")).toBe(false);
  });

  it("rejects an array-looking string whose second character is #", () => {
    // Guards against Storybook's own "[#...]" placeholder syntax being parsed as JSON.
    expect(isJsonString("[#ref]")).toBe(false);
  });
});

describe("parseMatchers", () => {
  it("converts /pattern/flags strings into RegExps", () => {
    const matchers = parseMatchers({ number: "/count$/i" });
    expect(matchers.number).toBeInstanceOf(RegExp);
    expect(matchers.number.source).toBe("count$");
    expect(matchers.number.flags).toBe("i");
  });

  it("parses a pattern with no flags", () => {
    expect(parseMatchers({ localDate: "/date/" }).localDate.flags).toBe("");
  });

  it("drops entries that are not in /…/ form", () => {
    expect(parseMatchers({ bad: "count$" })).toEqual({});
  });
});

describe("deserializeJavaObjects", () => {
  it("parses numbers in base 10", () => {
    expect(deserializeJavaObjects("42", "number")).toBe(42);
  });

  it("does not treat a leading 0x as hexadecimal", () => {
    // parseInt without a radix would return 16 here.
    expect(deserializeJavaObjects("0x10", "number")).toBe(0);
  });

  it("parses regions as JSON", () => {
    expect(deserializeJavaObjects('{"components":[]}', "region")).toEqual({ components: [] });
  });

  it("passes strings through unchanged", () => {
    expect(deserializeJavaObjects("hello", "string")).toBe("hello");
  });

  it("passes unknown types through unchanged", () => {
    expect(deserializeJavaObjects("hello", "somethingElse")).toBe("hello");
  });

  it("delegates the date types to /lib/time", () => {
    expect(deserializeJavaObjects("2024-01-01", "localDate")).toEqual({ kind: "LocalDate", v: "2024-01-01" });
    expect(deserializeJavaObjects("2024-01-01T00:00", "localDateTime")).toEqual({
      kind: "LocalDateTime",
      v: "2024-01-01T00:00",
    });
    expect(deserializeJavaObjects("2024-01-01T00:00Z", "zonedDateTime")).toEqual({
      kind: "ZonedDateTime",
      v: "2024-01-01T00:00Z",
    });
  });
});

describe("deserializeJsonEntries", () => {
  it("parses JSON-looking values and leaves plain strings alone", () => {
    expect(deserializeJsonEntries({ list: "[1,2]", name: "Ada" }, {}, {})).toEqual({ list: [1, 2], name: "Ada" });
  });

  it("applies an explicit javaType by path, in preference to matchers", () => {
    const matchers = parseMatchers({ string: "/count/" });
    expect(deserializeJsonEntries({ count: "7" }, matchers, { count: "number" })).toEqual({ count: 7 });
  });

  it("applies a matcher when no explicit javaType is given", () => {
    const matchers = parseMatchers({ number: "/Count$/" });
    expect(deserializeJsonEntries({ itemCount: "7" }, matchers, {})).toEqual({ itemCount: 7 });
  });

  it("resolves javaTypes for nested keys by path", () => {
    expect(deserializeJsonEntries({ outer: '{"inner":"7"}' }, {}, { outer: { inner: "number" } })).toEqual({
      outer: { inner: 7 },
    });
  });

  it("keeps the raw string and warns when JSON parsing fails", (t) => {
    const warn = t.mock.method(console, "warn", () => undefined);

    // Passes isJsonString (starts "{", ends "}") but is not valid JSON.
    expect(deserializeJsonEntries({ broken: "{nope}" }, {}, {})).toEqual({ broken: "{nope}" });
    expect(warn).toHaveBeenCalled();
  });
});
