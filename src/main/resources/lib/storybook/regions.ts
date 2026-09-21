import type { ComponentDescriptor, LayoutComponent, PageComponent, PartComponent, Region } from "@enonic-types/core";
import type { RenderFn, ViewMap } from "/lib/storybook/params";
import { filterObject, flatMap } from "/lib/storybook/utils";

export type { ComponentDescriptor } from "@enonic-types/core";
export type Component = PageComponent | PartComponent | LayoutComponent;

export function insertChildComponents(
  body: string,
  views: ViewMap,
  component: Component,
  model: Record<string, unknown>,
  renderFn: RenderFn,
  locale?: unknown,
): string {
  const renderParams = views[component.descriptor];

  if (renderParams === undefined) {
    log.warning(`You have not specified a template for the descriptor: "${component.descriptor}"`);
    return body;
  }

  const renderedComponent = renderFn(renderParams, {
    locale,
    ...component.config,
  });

  const renderedBody = body.replace(`<!--# COMPONENT ${component.path} -->`, renderedComponent);

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
  return objectValues(filterObject(rec, (_val, key) => region.test(key))).filter(isRegion);
}

export function isComponentDescriptor(value: string): value is ComponentDescriptor {
  return value.indexOf(":") !== -1;
}
