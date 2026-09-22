package no.item.storybook.render;

import java.util.List;
import java.util.Locale;

/**
 * What a template is allowed to pull in.
 *
 * <p>The endpoint checks the extension of the template it was asked for, but a template can include others, and an
 * inline {@code template} parameter puts the caller in full control of what it includes. Without a check here, an
 * inline template could read anything in the application's resources — which in development mode is
 * {@code src/main/resources} on disk, TypeScript sources, {@code .properties} files and all.
 *
 * <p>The list is deliberately generous: it covers every fragment type either engine can meaningfully render, so that
 * a template which works in a deployed application also works in the preview. It excludes what is source or
 * configuration rather than content.
 */
public final class TemplateExtensions {
  private static final List<String> ALLOWED =
    List.of(".ftl", ".ftlh", ".ftlx", ".html", ".htm", ".xhtml", ".xml", ".txt", ".js", ".json", ".css");

  private TemplateExtensions() {
  }

  public static boolean isTemplate(final String name) {
    if (name == null) {
      return false;
    }

    final String lowerCased = name.toLowerCase(Locale.ROOT);

    return ALLOWED.stream().anyMatch(lowerCased::endsWith);
  }
}
