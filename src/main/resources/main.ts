import { RunMode } from "/lib/storybook/java";

// Runs once when the app is loaded, landing in server.log next to the app being installed.
export function init(): void {
  // Fires in every mode, including dev, because dev mode is when the endpoint is live and
  // exploitable. Rendering a caller-supplied template is equivalent to running caller-supplied
  // code, so whoever installed this — possibly on a shared or networked dev box — needs to see
  // what it exposes, not just be told after they have put it in production.
  log.warning(
    `[${app.name}] SECURITY: this app renders caller-supplied FreeMarker and Thymeleaf templates, ` +
      `which is equivalent to remote code execution for anyone who can reach it. It is a local ` +
      `development tool only — never install it on a shared, networked, or production server.`,
  );

  // The preview API refuses every request outside development mode, but it can only say so on a
  // request that has already been made — by which point the caller is usually Storybook, which
  // reports it as an opaque failure. Say it at startup instead, where whoever installed this on a
  // production server can see it.
  if (!RunMode.isDev()) {
    log.warning(
      `[${app.name}] Enonic XP is running in production mode, so every request is answered with ` +
        `403 — uninstall this app from this server.`,
    );
  }
}

init();
