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
      res[key] = isRecord(value) ? traverse(value, f, path.concat(key)) : value;
    }
  }

  return res;
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
