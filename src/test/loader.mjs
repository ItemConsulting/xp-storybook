// Module resolver for `node --test`. XP resolves absolute specifiers like "/lib/storybook/x"
// against the app's own resources; the test runner needs the same mapping. "/lib/time" is
// Java-backed with no Node implementation, so it maps to a stub. Registered from register.mjs.
import { resolve as resolvePath } from "node:path";
import { pathToFileURL } from "node:url";

const STORYBOOK = resolvePath(process.cwd(), "src/main/resources/lib/storybook");
const LIB_TIME_STUB = resolvePath(process.cwd(), "src/test/stubs/lib-time.mts");

export function resolve(specifier, context, next) {
  if (specifier.startsWith("/lib/storybook/")) {
    const file = `${STORYBOOK}/${specifier.slice("/lib/storybook/".length)}.ts`;
    return { url: pathToFileURL(file).href, shortCircuit: true };
  }
  if (specifier === "/lib/time") {
    return { url: pathToFileURL(LIB_TIME_STUB).href, shortCircuit: true };
  }
  return next(specifier, context);
}
