// Module resolver for `node --test`. XP resolves absolute specifiers like "/lib/storybook/x"
// against the app's own resources; the test runner needs the same mapping. "/lib/time" and
// "/lib/storybook/java" are Java-backed with no Node implementation, so they map to stubs.
// Registered from register.mjs.
import { resolve as resolvePath } from "node:path";
import { pathToFileURL } from "node:url";

const STORYBOOK = resolvePath(process.cwd(), "src/main/resources/lib/storybook");
const LIB_TIME_STUB = resolvePath(process.cwd(), "src/test/stubs/lib-time.mts");
const STORYBOOK_JAVA_STUB = resolvePath(process.cwd(), "src/test/stubs/storybook-java.mts");

export function resolve(specifier, context, next) {
  if (specifier === "/lib/storybook/java") {
    return { url: pathToFileURL(STORYBOOK_JAVA_STUB).href, shortCircuit: true };
  }
  if (specifier.startsWith("/lib/storybook/")) {
    const file = `${STORYBOOK}/${specifier.slice("/lib/storybook/".length)}.ts`;
    return { url: pathToFileURL(file).href, shortCircuit: true };
  }
  if (specifier === "/lib/time") {
    return { url: pathToFileURL(LIB_TIME_STUB).href, shortCircuit: true };
  }
  return next(specifier, context);
}
