package no.item.storybook.freemarker;

import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.i18n.LocaleService;
import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.url.PortalUrlService;
import com.enonic.xp.resource.ResourceService;
import freemarker.core.Environment;
import no.item.freemarker.FreemarkerPortalObjectImpl;
import no.item.storybook.i18n.Phrases;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * The {@code portal} object, with {@code localize} reading the phrases of the application being previewed on every
 * call rather than through XP's {@code LocaleService}.
 *
 * <p>{@code LocaleService} caches a message bundle per application and locale, so an edited
 * {@code phrases.properties} would not appear until the application was deployed again. See {@link Phrases}.
 */
public class StorybookPortalObject extends FreemarkerPortalObjectImpl {
  private final Supplier<ResourceService> resourceServiceSupplier;
  private final ApplicationKey applicationKey;

  public StorybookPortalObject(
    Supplier<PortalUrlService> urlServiceSupplier,
    Supplier<LocaleService> localeServiceSupplier,
    Supplier<PortalRequest> portalRequestSupplier,
    Supplier<ResourceService> resourceServiceSupplier,
    ApplicationKey applicationKey
  ) {
    super(urlServiceSupplier, localeServiceSupplier, portalRequestSupplier, applicationKey);
    this.resourceServiceSupplier = resourceServiceSupplier;
    this.applicationKey = applicationKey;
  }

  @Override
  public String localize(String key) {
    return localize(key, List.of());
  }

  @Override
  public String localize(String key, List<String> values) {
    // The locale FreeMarker is rendering with. Outside a portal request that is the configuration default, which is
    // the same one lib-xp-freemarker would hand to LocaleService.
    Environment environment = Environment.getCurrentEnvironment();
    Locale locale = environment != null ? environment.getLocale() : null;

    return localize(key, locale != null ? locale.toLanguageTag() : null, values);
  }

  @Override
  public String localize(String key, String locale, List<String> values) {
    return localize(key, locale, values, List.of(), null);
  }

  @Override
  public String localize(String key, String locale, List<String> values, List<String> bundles, String application) {
    ApplicationKey resolved = application != null && !application.isBlank()
      ? ApplicationKey.from(application)
      : this.applicationKey;

    return Phrases.localize(
      resourceServiceSupplier.get(),
      resolved,
      locale != null ? Locale.forLanguageTag(locale) : Locale.ROOT,
      key,
      values,
      bundles
    );
  }
}
