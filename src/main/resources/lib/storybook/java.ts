export type MultiProjectTemplateLoader<Source = unknown> = {
  findTemplateSource(): Source;
  getLastModified(source: Source): number;
  getReader(source: Source, encoding: string): unknown;
  closeTemplateSource(source: Source): void;
};

type MultiProjectTemplateLoaderConstructor = {
  new (dirPaths: string[]): MultiProjectTemplateLoader;
};

export const MultiProjectTemplateLoader = Java.type<MultiProjectTemplateLoaderConstructor>(
  "no.item.storybook.freemarker.MultiProjectTemplateLoader",
);

export const TemplateExceptionHandler = Java.type<{
  HTML_DEBUG_HANDLER: unknown;
}>("freemarker.template.TemplateExceptionHandler");

export type Path = {
  compareTo(other: Path): number;
  endsWith(other: string): boolean;
  endsWith(other: Path): boolean;
  resolve(other: string): boolean;
  resolve(other: Path): boolean;
  startsWith(other: string): boolean;
  startsWith(other: Path): boolean;
  normalize(): Path;
};

export const Paths = Java.type<{
  get(path: string, ...more: string[]): Path;
}>("java.nio.file.Paths");

export const Files = Java.type<{
  exists(path: Path): boolean;
}>("java.nio.file.Files");
