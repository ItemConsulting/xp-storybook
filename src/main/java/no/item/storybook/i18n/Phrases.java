package no.item.storybook.i18n;

import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.resource.Resource;
import com.enonic.xp.resource.ResourceKey;
import com.enonic.xp.resource.ResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.TimeZone;

/**
 * Resolves phrases from an application's {@code /i18n} resources, the way XP's own {@code LocaleService} does, but
 * reading them again on every call.
 *
 * <p>Deliberately not {@code LocaleService}: that caches a message bundle per application and locale, so an edited
 * {@code phrases.properties} would not show up until the application was deployed again. A preview exists to show
 * what the files on disk currently say, and {@link ResourceService#getResource} is uncached — in development mode it
 * reads straight from the application's source directory — so resolving a phrase per call is what keeps phrases as
 * live as the templates that use them.
 *
 * <p>Everything else mirrors {@code LocaleServiceImpl} and {@code MessageBundleImpl}: the same default base name, the
 * same candidate-locale chain, and {@link MessageFormat} for the values, so a phrase resolves here exactly as it
 * would in production.
 */
public final class Phrases {
  private static final Logger LOG = LoggerFactory.getLogger(Phrases.class);

  /** What XP answers with when a phrase cannot be resolved. */
  public static final String NOT_TRANSLATED = "NOT_TRANSLATED";

  private static final List<String> DEFAULT_BASE_NAMES = List.of("/i18n/phrases");

  private Phrases() {
  }

  /**
   * @param locale the locale to resolve for, or {@code null} for {@link Locale#ROOT}.
   * @param values substituted into the phrase by {@link MessageFormat}. An empty list leaves the phrase as it is,
   *     placeholders and all, which is also what XP does.
   * @param bundleNames base names to read, or empty for {@code /i18n/phrases}.
   * @return the phrase, or {@link #NOT_TRANSLATED} if there is no such key.
   */
  public static String localize(final ResourceService resourceService, final ApplicationKey applicationKey,
                                final Locale locale, final String key, final List<String> values,
                                final List<String> bundleNames) {
    if (key == null) {
      return NOT_TRANSLATED;
    }

    final Locale nonNullLocale = locale != null ? locale : Locale.ROOT;
    final Properties properties = load(resourceService, applicationKey, nonNullLocale, bundleNames);
    final String message = properties.getProperty(key, "");

    return message.isEmpty() ? NOT_TRANSLATED : format(message, nonNullLocale, values);
  }

  /**
   * Reads every candidate bundle, least specific first, so that a more specific one overrides it — the parent chain
   * {@link ResourceBundle} would otherwise give us.
   */
  private static Properties load(final ResourceService resourceService, final ApplicationKey applicationKey,
                                 final Locale locale, final List<String> bundleNames) {
    final Properties properties = new Properties();

    for (final ResourceKey resourceKey : candidateKeys(applicationKey, locale, bundleNames)) {
      final Resource resource = resourceService.getResource(resourceKey);

      if (resource.exists()) {
        properties.putAll(read(resource));
      }
    }

    return properties;
  }

  private static List<ResourceKey> candidateKeys(final ApplicationKey applicationKey, final Locale locale,
                                                 final List<String> bundleNames) {
    final ResourceBundle.Control control = ResourceBundle.Control.getControl(ResourceBundle.Control.FORMAT_PROPERTIES);
    final List<String> baseNames = bundleNames == null || bundleNames.isEmpty() ? DEFAULT_BASE_NAMES : bundleNames;
    final List<ResourceKey> keys = new ArrayList<>();

    for (final String baseName : baseNames) {
      final String normalized = baseName.startsWith("/") ? baseName : "/" + baseName;
      final List<Locale> candidateLocales = new ArrayList<>(control.getCandidateLocales(normalized, locale));
      Collections.reverse(candidateLocales);

      for (final Locale candidateLocale : candidateLocales) {
        keys.add(ResourceKey.from(applicationKey, control.toBundleName(normalized, candidateLocale) + ".properties"));
      }
    }

    return keys;
  }

  private static Properties read(final Resource resource) {
    final Properties properties = new Properties();

    try (Reader reader = resource.openReader()) {
      properties.load(reader);
    } catch (final IOException e) {
      LOG.warn("Could not read phrases from {}", resource.getKey(), e);
    }

    return properties;
  }

  private static String format(final String message, final Locale locale, final List<String> values) {
    if (values == null || values.isEmpty()) {
      return message;
    }

    final MessageFormat messageFormat = new MessageFormat(message, locale);

    for (final Object format : messageFormat.getFormats()) {
      if (format instanceof DateFormat dateFormat) {
        dateFormat.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC));
      }
    }

    return messageFormat.format(values.toArray());
  }
}
