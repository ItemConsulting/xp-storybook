import { filterObject, flatMap } from "/lib/storybook/utils";
import type { Region } from "@enonic-types/core";
import type { LayoutComponent, PageComponent, PartComponent, ComponentDescriptor } from "@enonic-types/core";

export { ComponentDescriptor } from "@enonic-types/core";
export type Component = PageComponent | PartComponent | LayoutComponent;

export function isRegion(value: unknown): value is Region {
  const region = value as Region;
  return region?.components !== undefined;
}

export function getRegionComponents(regions: Region[]): Component[] {
  return flatMap(regions, (region) => region.components as Component[]);
}

export function findRegions(rec: Record<string, unknown>, region: RegExp): Array<Region> {
  return Object.values(filterObject(rec, (val, key) => region.test(key))).filter(isRegion);
}

export function isComponentDescriptor(value: string): value is ComponentDescriptor {
  return value.indexOf(":") !== -1;
}
