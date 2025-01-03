import { filterObject, flatMap } from "/lib/storybook/utils";
import type { LayoutComponent, PageComponent, PartComponent, ComponentDescriptor, Region } from "@enonic-types/core";
import type { ViewMap } from "/lib/storybook/params";

export type { ComponentDescriptor } from "@enonic-types/core";
export type RenderParams = string | { template: string };
export type Component = PageComponent | PartComponent | LayoutComponent;

export function insertChildComponents(
  body: string,
  views: ViewMap,
  component: Component,
  model: Record<string, unknown>,
  renderFn: <T = unknown>(params: RenderParams, model: T) => string,
  locale?: unknown,
): string {
  const view = views[component.descriptor];

  if (view === undefined) {
    log.warning(`You have not specified a template for the descriptor: "${component.descriptor}"`);
    return body;
  }

  const renderedBody = body.replace(
    `<!--# COMPONENT ${component.path} -->`,
    renderFn(view, {
      locale,
      ...component.config,
    }),
  );

  // is is layout-component
  if ("regions" in component) {
    return getRegionComponents(objectValues(component.config).filter(isRegion)).reduce(
      (str, comp) => insertChildComponents(str, views, comp, model, renderFn, locale),
      renderedBody,
    );
  } else {
    return renderedBody;
  }
}

function objectValues<T>(obj: Record<string, T>): T[] {
  return Object.keys(obj).map((key) => obj[key]);
}

function isRegion(value: unknown): value is Region {
  const region = value as Region;
  return region?.components !== undefined;
}

export function getRegionComponents(regions: Region[]): Component[] {
  return flatMap(regions, (region) => region.components as Component[]);
}

export function findRegions(rec: Record<string, unknown>, region: RegExp): Array<Region> {
  return objectValues(filterObject(rec, (val, key) => region.test(key))).filter(isRegion);
}

export function isComponentDescriptor(value: string): value is ComponentDescriptor {
  return value.indexOf(":") !== -1;
}

export function isRenderParams(value: unknown): value is RenderParams {
  const params = value as RenderParams;

  return typeof params === "string" || params.template !== undefined;
}
