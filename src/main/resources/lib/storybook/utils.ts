export function pick(value: unknown, path: string[]): undefined | string {
  const res = path.reduce((res: unknown, key: string) => {
    if (isRecord(res)) {
      return res[key];
    } else {
      return undefined;
    }
  }, value);

  return res as string | undefined;
}

export function substringAfter(str: string, delimiter: string): string {
  return str.substring(str.indexOf(delimiter) + delimiter.length);
}

export function traverse(
  obj: Record<string, unknown>,
  f: (key: string, value: unknown, path: string[]) => unknown,
  path: string[] = [],
): Record<string, unknown> {
  const res: Record<string, unknown> = {};

  for (const key in obj) {
    if (obj.hasOwnProperty(key)) {
      const value = f(key, obj[key], path.concat(key));
      res[key] = isRecord(value)
        ? traverse(value, f, path.concat(key))
        : Array.isArray(value)
          ? value.map((val) => (isRecord(val) ? traverse(val, f, path.concat(key)) : f(key, val, path)))
          : value;
    }
  }

  return res;
}

export function filterObject<T>(
  rec: Record<string, T>,
  predicate: (value: T, key: string) => boolean,
): Record<string, T> {
  return Object.keys(rec).reduce<Record<string, T>>((res, key) => {
    if (predicate(rec[key], key)) {
      res[key] = rec[key];
    }

    return res;
  }, {});
}

export type Splitted<T> = [left: Record<string, T>, right: Record<string, T>];

export function split<T>(record: Record<string, T>, predicate: (value: T, key: string) => boolean): Splitted<T> {
  return Object.keys(record).reduce<Splitted<T>>(
    (tuple, key) => {
      const value = record[key];

      tuple[predicate(value, key) ? 0 : 1][key] = value;

      return tuple;
    },
    [{}, {}],
  );
}

export function flatMap<A, B>(arr: Array<A>, f: (as: A) => Array<B>): Array<B> {
  return arr.reduce<B[]>((res, val) => res.concat(f(val)), []);
}

export function capitalize(str: string): string {
  return str.charAt(0).toUpperCase() + str.slice(1);
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return value != null && isObject(value) && !isJavaClass(value);
}

function isObject(obj: unknown): boolean {
  return obj.constructor?.toString().match(/\w+/g)[1] === "Object";
}

export function endsWith(str: string, suffix: string): boolean {
  return str.indexOf(suffix, str.length - suffix.length) !== -1;
}

/**
 * Note that strings are also Java classes (java.lang.String)
 */
function isJavaClass(value: unknown): boolean {
  const cls = (value as { class?: string })?.class;

  return cls !== undefined && String(cls).substring(0, 6) === "class ";
}
