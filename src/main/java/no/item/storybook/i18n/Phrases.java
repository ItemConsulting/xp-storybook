package no.item.storybook.i18n;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Loads the "phrases" bundle from a directory on disk, the way Storybook previews supply i18n.
 *
 * <p>Both lookups are best-effort on purpose. XP's own {@code localize} answers with a short marker
 * when a phrase cannot be resolved, and a preview should do the same: a project with no phrases
 * file at all — or a key that has not been translated yet — is an ordinary state while authoring,
 * not an error worth replacing the rendered component with a stack trace.
 */
public final class Phrases {
  private static final Logger log = LoggerFactory.getLogger(Phrases.class);

  public static final String NOT_TRANSLATED = "NOT_TRANSLATED";

  private static final String BUNDLE_NAME = "phrases";

  private Phrases() {
  }

  /**
   * Resolves {@code key} against the phrases bundle found under {@code baseDirPath}.
   *
   * @return the phrase with {@code values} substituted for {0}, {1}, ..., or {@link #NOT_TRANSLATED}
   *     if there is no bundle or the bundle has no such key.
   */
  public static String localize(final String baseDirPath, final Locale locale, final String key,
                                final List<String> values) {
    if (baseDirPath == null) {
      log.warn("No base directory set for localization, returning '{}'", NOT_TRANSLATED);
      return NOT_TRANSLATED;
    }

    return find(baseDirPath, locale)
      .filter(bundle -> bundle.containsKey(key))
      .map(bundle -> applyValues(bundle.getString(key), values))
      .orElse(NOT_TRANSLATED);
  }

  /**
   * Loads the phrases bundle from {@code <baseDirPath>/i18n}, falling back to
   * {@code <baseDirPath>/site/i18n} for projects that keep their phrases under the site directory.
   *
   * @return empty when neither directory holds a matching bundle.
   */
  public static Optional<ResourceBundle> find(final String baseDirPath, final Locale locale) {
    if (baseDirPath == null) {
      return Optional.empty();
    }

    return load(new File(baseDirPath, "i18n"), locale)
      .or(() -> load(new File(baseDirPath + File.separator + "site", "i18n"), locale));
  }

  private static Optional<ResourceBundle> load(final File dir, final Locale locale) {
    if (!dir.isDirectory()) {
      return Optional.empty();
    }

    // Deliberately a fresh loader per call, not a cached one: ResourceBundle keys its cache on the
    // class loader, so reusing one would also reuse the parsed bundle and stop picking up edits to
    // phrases.properties — which is the whole point of a live preview. Closing it releases the file
    // handle; the bundle is fully read into memory by then.
    try (URLClassLoader loader = new URLClassLoader(new URL[]{dir.toURI().toURL()})) {
      return Optional.of(ResourceBundle.getBundle(BUNDLE_NAME, locale, loader));
    } catch (final MissingResourceException e) {
      // No phrases.properties in this directory. Normal while authoring; the caller falls back to
      // NOT_TRANSLATED rather than failing the render.
      log.debug("No '{}' bundle in {}", BUNDLE_NAME, dir, e);
      return Optional.empty();
    } catch (final IOException e) {
      log.error("Could not load resource bundle from {}", dir, e);
      return Optional.empty();
    }
  }

  private static String applyValues(final String phrase, final List<String> values) {
    if (values == null || values.isEmpty()) {
      return phrase;
    }

    String result = phrase;

    for (int i = 0; i < values.size(); i++) {
      result = result.replace("{" + i + "}", values.get(i));
    }

    return result;
  }
}
