package no.item.storybook.freemarker;

import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.url.PortalUrlService;
import com.enonic.xp.portal.view.ViewFunctionService;
import com.google.common.collect.Lists;
import freemarker.core.Environment;
import no.item.freemarker.FreemarkerPortalObjectImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Supplier;

public class StorybookPortalObject extends FreemarkerPortalObjectImpl {
  private final static Logger log = LoggerFactory.getLogger(StorybookPortalObject.class);
  private final String baseDirPath;

  /**
   * This object provides portal-related functionality for Freemarker templates,
   * including URL generation, localization, and HTML processing capabilities.
   */
  public StorybookPortalObject(PortalUrlService urlService, ViewFunctionService viewFunctionService, Supplier<PortalRequest> requestSupplier, String baseDirPath) {
    super(urlService, viewFunctionService, requestSupplier);
    this.baseDirPath = baseDirPath;
  }

  /**
   * This function generates a URL pointing to a static file.
   *
   * @param path Path to the asset.
   * @return The generated URL.
   */
  @Override
  public String assetUrl(String path) {
    return path;
  }

  /**
   * This function generates a URL pointing to a static file.
   *
   * @param path        Path to the asset.
   * @param type        URL type. Either `server` (server-relative URL) or `absolute`.
   * @param application Other application to reference to. Defaults to current application.
   * @param params      Custom parameters to append to the url.
   * @return The generated URL.
   */
  @Override
  public String assetUrl(String path, String type, String application, Map<String, String> params) {
    return path;
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
   * @return The localized string.
   */
  @Override
  public String localize(String key, String locale, List<String> values, List<String> bundles, String application) {
    if (baseDirPath == null) {
      log.warn("No base directory set for localization, returning 'NOT_TRANSLATED'");
      return "NOT_TRANSLATED";
    }

    ResourceBundle bundle = getResourceBundle(bundles.get(0));

    if (bundle.keySet().contains(key)) {
      return bundle.getString(key);
    } else {
      return "NOT_TRANSLATED";
    }
  }

  private ResourceBundle getResourceBundle(String languageTag) {
    Locale locale = languageTag != null ? Locale.forLanguageTag(languageTag) : Locale.ROOT;

    File dir = new File(baseDirPath + File.separator + "i18n");

    try {
      URL url = dir.toURI().toURL();
      ClassLoader loader = new URLClassLoader(new URL[]{url});
      return ResourceBundle.getBundle("phrases", locale, loader); // TODO: Can be another bundle then phrases
    } catch (MalformedURLException e) {
      log.error("Could not load resource bundle", e);
      return ResourceBundle.getBundle("phrases", locale);
    }
  }
}

