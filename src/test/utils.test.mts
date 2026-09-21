import { describe, it } from "node:test";
import {
  capitalize,
  endsWith,
  filterObject,
  flatMap,
  pick,
  split,
  substringAfter,
  traverse,
} from "/lib/storybook/utils";
import { expect } from "./expect.mts";

describe("substringAfter", () => {
  it("returns everything after the first occurrence of the delimiter", () => {
    expect(substringAfter("/api/no.item.storybook:preview/my/view", "no.item.storybook:preview")).toBe("/my/view");
  });

  it("splits on the FIRST occurrence, not the last", () => {
    expect(substringAfter("a/b/c", "/")).toBe("b/c");
  });

  it("returns the whole string when the delimiter is absent", () => {
    // indexOf returns -1, so the substring starts at -1 + length.
    expect(substringAfter("abc", "/")).toBe("abc");
  });
});

describe("pick", () => {
  it("reads a nested value by path", () => {
    expect(pick({ a: { b: { c: "found" } } }, ["a", "b", "c"])).toBe("found");
  });

  it("returns undefined when the path leaves the object", () => {
    expect(pick({ a: "not-an-object" }, ["a", "b"])).toBeUndefined();
    expect(pick({ a: {} }, ["a", "b", "c"])).toBeUndefined();
  });

  it("returns the input itself for an empty path", () => {
    expect(pick("value", [])).toBe("value");
  });

  it("does not traverse into arrays", () => {
    expect(pick({ a: ["x"] }, ["a", "0"])).toBeUndefined();
  });
});

describe("traverse", () => {
  it("applies the function to every key, depth first", () => {
    const seen: string[] = [];
    traverse({ a: 1, b: { c: 2 } }, (key, value) => {
      seen.push(key);
      return value;
    });
    expect(seen).toEqual(["a", "b", "c"]);
  });

  it("passes the full path of each key", () => {
    const paths: string[][] = [];
    traverse({ a: { b: { c: 1 } } }, (_key, value, path) => {
      paths.push(path);
      return value;
    });
    expect(paths).toEqual([["a"], ["a", "b"], ["a", "b", "c"]]);
  });

  it("maps values and recurses into records inside arrays", () => {
    const result = traverse({ list: [{ n: "1" }, { n: "2" }] }, (_key, value) =>
      typeof value === "string" ? Number(value) : value,
    );
    expect(result).toEqual({ list: [{ n: 1 }, { n: 2 }] });
  });

  it("leaves a replaced non-record value alone", () => {
    expect(traverse({ a: "x" }, () => "replaced")).toEqual({ a: "replaced" });
  });
});

describe("filterObject", () => {
  it("keeps only entries matching the predicate", () => {
    expect(filterObject({ a: 1, b: 2, c: 3 }, (value) => value > 1)).toEqual({ b: 2, c: 3 });
  });

  it("passes the key to the predicate", () => {
    expect(filterObject({ keep: 1, drop: 2 }, (_value, key) => key === "keep")).toEqual({ keep: 1 });
  });
});

describe("split", () => {
  it("partitions into [matching, non-matching]", () => {
    expect(split({ a: 1, b: 2 }, (value) => value === 1)).toEqual([{ a: 1 }, { b: 2 }]);
  });

  it("yields two empty records for an empty input", () => {
    expect(split({}, () => true)).toEqual([{}, {}]);
  });
});

describe("flatMap", () => {
  it("concatenates the produced arrays", () => {
    expect(flatMap([1, 2], (n) => [n, n * 10])).toEqual([1, 10, 2, 20]);
  });

  it("drops entries that map to an empty array", () => {
    expect(flatMap([1, 2], (n) => (n === 1 ? [n] : []))).toEqual([1]);
  });
});

describe("capitalize", () => {
  it("upper-cases the first character only", () => {
    expect(capitalize("freemarker")).toBe("Freemarker");
    expect(capitalize("thymeLEAF")).toBe("ThymeLEAF");
  });

  it("handles the empty string", () => {
    expect(capitalize("")).toBe("");
  });
});

describe("endsWith", () => {
  it("detects a suffix", () => {
    expect(endsWith("view.ftl", ".ftl")).toBe(true);
    expect(endsWith("view.ftlh", ".ftl")).toBe(false);
  });

  it("does not match a suffix occurring earlier in the string", () => {
    expect(endsWith("a.ftl.html", ".ftl")).toBe(false);
  });
});
