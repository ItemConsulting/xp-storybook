import assert from "node:assert/strict";

type NodeMock = { mock: { callCount(): number } };

/**
 * A minimal `expect()` backed by `node:assert`, covering exactly the matchers these tests use.
 * It keeps the suite off a test-framework dependency (we run on `node:test`) while leaving the
 * assertions readable. `toEqual` is deep + strict, so an expected `{ k: undefined }` must match an
 * own `k` set to `undefined` on the actual value.
 */
export function expect(actual: unknown) {
  return {
    toBe: (expected: unknown): void => assert.strictEqual(actual, expected),
    toEqual: (expected: unknown): void => assert.deepStrictEqual(actual, expected),
    toBeUndefined: (): void => assert.strictEqual(actual, undefined),
    toBeInstanceOf: (ctor: new (...args: never[]) => unknown): void => assert.ok(actual instanceof ctor),
    toHaveLength: (length: number): void => assert.strictEqual((actual as { length: number }).length, length),
    toHaveBeenCalled: (): void => assert.ok((actual as NodeMock).mock.callCount() > 0),
    not: {
      toHaveProperty: (key: string): void => assert.ok(!(key in (actual as object))),
    },
  };
}
