import { describe, it } from "node:test";
import type { Region } from "@enonic-types/core";
import type { RenderFn, RenderParams, ViewMap } from "/lib/storybook/params";
import {
  type Component,
  findRegions,
  getRegionComponents,
  insertChildComponents,
  isComponentDescriptor,
} from "/lib/storybook/regions";
import { expect } from "./expect.mts";

const VIEW: RenderParams = {
  type: "file",
  filePath: "part.ftl",
  xpAppName: "com.example.app",
};

function part(descriptor: string, path: string, config: Record<string, unknown> = {}): Component {
  return { type: "part", descriptor, path, config } as unknown as Component;
}

function region(name: string, components: Component[]): Region {
  return { name, components } as unknown as Region;
}

describe("isComponentDescriptor", () => {
  it("requires an app-qualified name", () => {
    expect(isComponentDescriptor("com.example.app:my-part")).toBe(true);
    expect(isComponentDescriptor("my-part")).toBe(false);
  });
});

describe("findRegions", () => {
  it("returns values whose key matches and which look like a region", () => {
    const model = {
      myRegion: region("main", []),
      otherRegion: region("side", []),
      title: "not a region",
    };
    expect(findRegions(model, /Region$/)).toHaveLength(2);
  });

  it("skips matching keys that are not regions", () => {
    expect(findRegions({ fakeRegion: { notComponents: [] } }, /Region$/)).toEqual([]);
  });

  it("returns nothing when no key matches", () => {
    expect(findRegions({ myRegion: region("main", []) }, /nope/)).toEqual([]);
  });
});

describe("getRegionComponents", () => {
  it("flattens the components of every region", () => {
    const a = part("app:a", "/main/0");
    const b = part("app:b", "/side/0");
    expect(getRegionComponents([region("main", [a]), region("side", [b])])).toEqual([a, b]);
  });

  it("returns an empty array for no regions", () => {
    expect(getRegionComponents([])).toEqual([]);
  });
});

describe("insertChildComponents", () => {
  const renderFn: RenderFn = () => "<p>rendered</p>";

  it("replaces the component placeholder with the rendered output", () => {
    const views: ViewMap = { "app:a": VIEW } as ViewMap;
    const body = "<div><!--# COMPONENT /main/0 --></div>";

    expect(insertChildComponents(body, views, part("app:a", "/main/0"), {}, renderFn)).toBe(
      "<div><p>rendered</p></div>",
    );
  });

  it("passes the component config and locale into the render function", () => {
    const seen: Array<Record<string, unknown>> = [];
    const capturing: RenderFn = (_params, model) => {
      seen.push(model);
      return "";
    };
    const views: ViewMap = { "app:a": VIEW } as ViewMap;

    insertChildComponents(
      "<!--# COMPONENT /main/0 -->",
      views,
      part("app:a", "/main/0", { heading: "Hi" }),
      {},
      capturing,
      "no",
    );

    expect(seen[0]).toEqual({ locale: "no", heading: "Hi" });
  });

  it("warns and returns the body untouched when the descriptor has no view", (t) => {
    const warn = t.mock.method(console, "warn", () => undefined);
    const body = "<div><!--# COMPONENT /main/0 --></div>";

    expect(insertChildComponents(body, {} as ViewMap, part("app:missing", "/main/0"), {}, renderFn)).toBe(body);
    expect(warn).toHaveBeenCalled();
  });

  it("leaves the body untouched when no placeholder matches the component path", () => {
    const views: ViewMap = { "app:a": VIEW } as ViewMap;
    const body = "<div><!--# COMPONENT /other/9 --></div>";

    expect(insertChildComponents(body, views, part("app:a", "/main/0"), {}, renderFn)).toBe(body);
  });

  it("recurses into a layout's regions", () => {
    const child = part("app:child", "/main/0/inner/0");
    const layout = {
      type: "layout",
      descriptor: "app:layout",
      path: "/main/0",
      regions: {},
      config: { inner: region("inner", [child]) },
    } as unknown as Component;

    const childView: RenderParams = { ...VIEW, filePath: "child.ftl" };
    const views: ViewMap = { "app:layout": VIEW, "app:child": childView } as ViewMap;
    const renderLayout: RenderFn = (params) =>
      params === childView ? "<p>child</p>" : "<section><!--# COMPONENT /main/0/inner/0 --></section>";

    const result = insertChildComponents("<!--# COMPONENT /main/0 -->", views, layout, {}, renderLayout);

    expect(result).toBe("<section><p>child</p></section>");
  });
});
