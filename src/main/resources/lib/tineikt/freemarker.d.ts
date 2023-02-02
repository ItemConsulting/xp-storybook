import type { ResourceKey } from "@enonic-types/lib-export";

export interface TextTemplate {
  template: string;
  baseDirPath?: string;
}

export interface File {
  filePath: string;
  baseDirPath?: string;
}

export type FreemarkerView = ResourceKey | TextTemplate | File;

export function render<Model extends object>(view: FreemarkerView, model: Model): string;
