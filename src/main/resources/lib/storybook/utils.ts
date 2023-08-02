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

export function traverse(
  obj: Record<string, unknown>,
  f: (key: string, value: unknown, path: string[]) => unknown,
  path: string[] = []
): Record<string, unknown> {
  const res: Record<string, unknown> = {};

  for (const key in obj) {
    if (obj.hasOwnProperty(key)) {
      const value = f(key, obj[key], path.concat(key));
      res[key] = isRecord(value)
        ? traverse(value, f, path.concat(key))
        : Array.isArray(value)
        ? value.map((val) => {
            return typeof val === "string" || val instanceof String
              ? f(key, val, path)
              : traverse(val, f, path.concat(key));
          })
        : value;
    }
  }

  return res;
}

export function filterObject<T>(
  rec: Record<string, T>,
  predicate: (value: T, key: string) => boolean
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
    [{}, {}]
  );
}

export function flatMap<A, B>(arr: Array<A>, f: (as: A) => Array<B>): Array<B> {
  return arr.reduce<B[]>((res, val) => res.concat(f(val)), []);
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return value != null && value.constructor?.name === "Object" && !isJavaClass(value);
}

/**
 * Note that strings are also Java classes (java.lang.String)
 */
function isJavaClass(value: unknown): boolean {
  const cls = (value as { class?: string })?.class;

  return cls !== undefined && String(cls).substring(0, 6) === "class ";
}
