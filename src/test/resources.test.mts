import { mkdirSync, mkdtempSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { dirname, join } from "node:path";
import { describe, it } from "node:test";
import { getBaseDirIfFileExists, getResourcesDirPaths } from "/lib/storybook/resources";
import { expect } from "./expect.mts";

function withTemplate(relativePath: string): string {
  const dir = mkdtempSync(join(tmpdir(), "xp-storybook-"));
  const file = join(dir, relativePath);

  mkdirSync(dirname(file), { recursive: true });
  writeFileSync(file, "");

  return dir;
}

describe("getResourcesDirPaths", () => {
  it("splits a comma separated list", () => {
    expect(getResourcesDirPaths("/a/src,/b/src")).toEqual(["/a/src", "/b/src"]);
  });

  it("trims the whitespace around each entry", () => {
    expect(getResourcesDirPaths("/a/src , /b/src")).toEqual(["/a/src", "/b/src"]);
  });

  it("returns a single entry for a single directory", () => {
    expect(getResourcesDirPaths("/a/src")).toEqual(["/a/src"]);
  });

  it("returns an empty list when no directory was given", () => {
    expect(getResourcesDirPaths(undefined)).toEqual([]);
  });
});

describe("getBaseDirIfFileExists", () => {
  it("returns the directory holding the file", () => {
    const first = mkdtempSync(join(tmpdir(), "xp-storybook-"));
    const second = withTemplate("parts/card.html");

    expect(getBaseDirIfFileExists([first, second], "parts/card.html")).toBe(second);
  });

  it("prefers the first directory holding the file", () => {
    const first = withTemplate("parts/card.html");
    const second = withTemplate("parts/card.html");

    expect(getBaseDirIfFileExists([first, second], "parts/card.html")).toBe(first);
  });

  // The template then comes from an application's resources instead, and there is no directory on
  // disk to read a phrases bundle from.
  it("returns undefined when no directory holds the file", () => {
    const dir = mkdtempSync(join(tmpdir(), "xp-storybook-"));

    expect(getBaseDirIfFileExists([dir], "parts/absent.html")).toBeUndefined();
  });

  it("returns undefined when there are no directories", () => {
    expect(getBaseDirIfFileExists([], "parts/card.html")).toBeUndefined();
  });
});
