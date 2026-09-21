package no.item.storybook.freemarker;

import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.i18n.LocaleService;
import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.url.PortalUrlService;
import com.google.common.collect.Lists;
import freemarker.core.Environment;
import no.item.freemarker.FreemarkerPortalObjectImpl;
import no.item.storybook.i18n.Phrases;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public class StorybookPortalObject extends FreemarkerPortalObjectImpl {
  private final String baseDirPath;

  /**
   * This object provides portal-related functionality for Freemarker templates,
   * including URL generation, localization, and HTML processing capabilities.
   */
  public StorybookPortalObject(
    Supplier<PortalUrlService> urlServiceSupplier,
    Supplier<LocaleService> localeServiceSupplier,
    Supplier<PortalRequest> portalRequestSupplier,
    ApplicationKey applicationKey,
    String baseDirPath
  ) {
    super(urlServiceSupplier, localeServiceSupplier, portalRequestSupplier, applicationKey);
    this.baseDirPath = baseDirPath;
  }

  /**
   * This function localizes a phrase.
   *
   * @param key The property key.
   * @return The localized string.
   */
  @Override
  public String localize(String key) {
    return localize(key, Lists.newArrayList());
  }

  /**
   * This function localizes a phrase.
   *
   * @param key    The property key.
   * @param values Placeholder values.
   * @return The localized string.
   */
  @Override
  public String localize(String key, List<String> values) {
    Environment environment = Environment.getCurrentEnvironment();
    return localize(key, environment.getLocale().toLanguageTag(), values);
  }

  /**
   * This function localizes a phrase.
   *
   * @param key    The property key.
   * @param locale A string-representation of a locale. If the locale is not set, the content language is used.
   * @param values Placeholder values.
   * @return The localized string.
   */
  @Override
  public String localize(String key, String locale, List<String> values) {
    return localize(key, locale, values, Lists.newArrayList(), null);
  }

  /**
   * This function localizes a phrase.
   *
   * @param key    The property key.
   * @param locale A string-representation of a locale. If the locale is not set, the content language is used.
   * @param values Placeholder values.
   * @param application The name of the application
   * @return The localized string.
   */
  @Override
  public String localize(String key, String locale, List<String> values, List<String> bundles, String application) {
    Locale resolved = locale != null ? Locale.forLanguageTag(locale) : Locale.ROOT;

    return Phrases.localize(baseDirPath, resolved, key, values);
  }
}

