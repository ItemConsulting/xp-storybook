// Loaded via `node --import` before any test. Registers the module resolver and installs the
// `log` global, which XP passes into every module rather than putting on the global object.
// Delegates to console lazily so a test that mocks console.warn intercepts log.warning.
import { register } from "node:module";

register("./loader.mjs", import.meta.url);

globalThis.log = {
  debug: (...args) => console.debug(...args),
  info: (...args) => console.info(...args),
  error: (...args) => console.error(...args),
  warning: (...args) => console.warn(...args),
};
