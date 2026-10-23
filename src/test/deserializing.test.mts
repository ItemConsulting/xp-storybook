import { describe, it } from "node:test";
import { deserializeJsonEntries, isJsonString } from "/lib/storybook/deserializing";
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

describe("deserializeJsonEntries", () => {
  it("parses JSON-looking values and leaves plain strings alone", () => {
    expect(deserializeJsonEntries({ list: "[1,2]", name: "Ada" })).toEqual({ list: [1, 2], name: "Ada" });
  });

  it("applies the key conventions", () => {
    expect(
      deserializeJsonEntries({ itemCount: "7", publishDate: "2024-01-01", mainRegion: '{"components":[]}' }),
    ).toEqual({
      itemCount: 7,
      publishDate: { kind: "LocalDate", v: "2024-01-01" },
      mainRegion: { components: [] },
    });
  });

  it("treats a key ending in Count or Number as an integer", () => {
    expect(deserializeJsonEntries({ itemCount: "7", pageNumber: "8" })).toEqual({ itemCount: 7, pageNumber: 8 });
  });

  it("leaves a Count or Number value that is not all digits as a string", () => {
    expect(deserializeJsonEntries({ phoneNumber: "+47 123 45 678", itemCount: "", pageNumber: "-1" })).toEqual({
      phoneNumber: "+47 123 45 678",
      itemCount: "",
      pageNumber: "-1",
    });
  });

  it("picks the date type from the shape of a value whose key ends in Date", () => {
    const dates = {
      a: "2024-01-01",
      b: "2024-01-01T12:30",
      c: "2024-01-01T12:30:15.123",
      d: "2024-01-01T12:30Z",
      e: "2024-01-01T12:30:00+01:00",
      f: "2024-01-01T12:30:00+01:00[Europe/Oslo]",
    };
    const kinds = Object.keys(dates).map((key) => {
      const model = deserializeJsonEntries({ [`${key}Date`]: dates[key as keyof typeof dates] });
      return (model[`${key}Date`] as { kind: string }).kind;
    });

    expect(kinds).toEqual([
      "LocalDate",
      "LocalDateTime",
      "LocalDateTime",
      "ZonedDateTime",
      "ZonedDateTime",
      "ZonedDateTime",
    ]);
  });

  it("keeps the raw string and warns when a Date key holds something that is not a date", (t) => {
    const warn = t.mock.method(console, "warn", () => undefined);

    expect(deserializeJsonEntries({ publishDate: "tomorrow" })).toEqual({ publishDate: "tomorrow" });
    expect(warn).toHaveBeenCalled();
  });

  it("matches the key suffix case-sensitively, and only at the end", () => {
    expect(deserializeJsonEntries({ date: "2024-01-01", countdown: "7" })).toEqual({
      date: "2024-01-01",
      countdown: "7",
    });
  });

  it("applies the key conventions to nested keys", () => {
    expect(deserializeJsonEntries({ outer: '{"itemCount":"7"}' })).toEqual({ outer: { itemCount: 7 } });
  });

  it("keeps the raw string and warns when JSON parsing fails", (t) => {
    const warn = t.mock.method(console, "warn", () => undefined);

    // Passes isJsonString (starts "{", ends "}") but is not valid JSON.
    expect(deserializeJsonEntries({ broken: "{nope}" })).toEqual({ broken: "{nope}" });
    expect(warn).toHaveBeenCalled();
  });
});
