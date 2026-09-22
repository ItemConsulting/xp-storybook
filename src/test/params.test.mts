import { describe, it } from "node:test";
import { isTemplatePath, parseParams } from "/lib/storybook/params";
import { expect } from "./expect.mts";

describe("parseParams", () => {
  it("separates component views from ordinary model values", () => {
    const parsed = parseParams({
      "app:my-part": "parts/my-part.ftl",
      heading: "Hello",
      xpAppName: "com.example.app",
    });

    expect(parsed.views["app:my-part"]).toEqual({
      type: "file",
      filePath: "parts/my-part.ftl",
      xpAppName: "com.example.app",
    });
    // The reserved parameters configure the renderer, so none of them reaches the template.
    expect(parsed.model).toEqual({ heading: "Hello" });
  });

  it("recognises an inline template supplied as JSON", () => {
    const parsed = parseParams({
      "app:my-part": JSON.stringify({ template: "<p>inline</p>" }),
      xpAppName: "com.example.app",
    });

    expect(parsed.views["app:my-part"]).toEqual({
      type: "inline",
      template: "<p>inline</p>",
      name: "app:my-part",
      xpAppName: "com.example.app",
    });
  });

  it("ignores a view whose value is neither an inline template nor a template file", (t) => {
    const warn = t.mock.method(console, "warn", () => undefined);

    // Valid JSON, but no `template` key — and not a template path either, so there is nothing to do with it.
    const parsed = parseParams({ "app:my-part": '{"other":1}', xpAppName: "com.example.app" });

    expect(parsed.views["app:my-part"]).toBeUndefined();
    expect(warn).toHaveBeenCalled();
  });

  it("ignores a view pointing at a non-template file", (t) => {
    t.mock.method(console, "warn", () => undefined);

    const parsed = parseParams({ "app:my-part": "../../../etc/passwd", xpAppName: "com.example.app" });

    expect(parsed.views["app:my-part"]).toBeUndefined();
  });

  it("takes xpAppName from the query parameters", () => {
    // There is no server-side fallback: the controller rejects a request without it.
    expect(parseParams({ xpAppName: "com.example.app" }).xpAppName).toBe("com.example.app");
  });

  it("keeps template, javaTypes and matchers out of the model", () => {
    const parsed = parseParams({
      template: "<p>t</p>",
      javaTypes: "{}",
      matchers: "{}",
      heading: "Hello",
      xpAppName: "com.example.app",
    });

    expect(parsed.template).toBe("<p>t</p>");
    expect(parsed.model).not.toHaveProperty("template");
    expect(parsed.model).not.toHaveProperty("javaTypes");
    expect(parsed.model).not.toHaveProperty("matchers");
  });

  it("applies javaTypes to the model", () => {
    const parsed = parseParams({
      count: "42",
      javaTypes: JSON.stringify({ count: "number" }),
      xpAppName: "com.example.app",
    });

    expect(parsed.model.count).toBe(42);
  });

  it("collects components from the regions named by the region matcher", () => {
    const parsed = parseParams({
      main: JSON.stringify({ components: [{ type: "part", descriptor: "app:a", path: "/main/0", config: {} }] }),
      matchers: JSON.stringify({ region: "/^main$/" }),
      xpAppName: "com.example.app",
    });

    expect(parsed.components).toHaveLength(1);
    expect(parsed.components[0].descriptor).toBe("app:a");
  });

  it("returns no components when no region matcher is given", () => {
    const parsed = parseParams({ main: '{"components":[]}', xpAppName: "com.example.app" });
    expect(parsed.components).toEqual([]);
  });
});

describe("isTemplatePath", () => {
  it("accepts every supported template extension", () => {
    expect(isTemplatePath("parts/a.ftl")).toBe(true);
    expect(isTemplatePath("parts/a.ftlh")).toBe(true);
    expect(isTemplatePath("parts/a.ftlx")).toBe(true);
    expect(isTemplatePath("parts/a.html")).toBe(true);
  });

  it("ignores extension case", () => {
    expect(isTemplatePath("parts/A.FTLH")).toBe(true);
  });

  it("rejects anything else", () => {
    expect(isTemplatePath("passwd")).toBe(false);
    expect(isTemplatePath("../../../etc/passwd")).toBe(false);
    expect(isTemplatePath("id_rsa")).toBe(false);
    expect(isTemplatePath(".env")).toBe(false);
    expect(isTemplatePath("app.properties")).toBe(false);
    expect(isTemplatePath("")).toBe(false);
  });

  it("requires the extension at the end, not merely somewhere in the path", () => {
    expect(isTemplatePath("a.html/../../etc/passwd")).toBe(false);
    expect(isTemplatePath("templates.ftl.bak")).toBe(false);
  });
});
