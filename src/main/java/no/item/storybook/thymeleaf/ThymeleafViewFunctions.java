package no.item.storybook.thymeleaf;

import com.enonic.lib.thymeleaf.view.ViewFunctionParams;
import com.enonic.lib.thymeleaf.view.ViewFunctionService;
import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.resource.ResourceService;
import no.item.storybook.i18n.Phrases;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

final class ThymeleafViewFunctions {
  ViewFunctionService viewFunctionService;
  PortalRequest portalRequest;
  /** lib-thymeleaf's own form for several values in one argument: "_values={a,b}". */
  private static final Pattern BRACED_VALUES = Pattern.compile("^\\{.*\\}$");

  private final ApplicationKey applicationKey;
  private final Supplier<ResourceService> resourceServiceSupplier;

  public ThymeleafViewFunctions(ApplicationKey applicationKey, Supplier<ResourceService> resourceServiceSupplier) {
    this.applicationKey = applicationKey;
    this.resourceServiceSupplier = resourceServiceSupplier;
  }

  public String assetUrl(final List<String> params) {
    return getParamValue(params, "_path").orElse("");
  }

  public String pageUrl(final List<String> params) {
    return execute("pageUrl", params).toString();
  }

  public String attachmentUrl(final List<String> params) {
    return execute("attachmentUrl", params).toString();
  }

  public String componentUrl(final List<String> params) {
    return execute("componentUrl", params).toString();
  }

  public String imageUrl(final List<String> params) {
    return execute("imageUrl", params).toString();
  }

  public String serviceUrl(final List<String> params) {
    return execute("serviceUrl", params).toString();
  }

  public String idProviderUrl(final List<String> params) {
    return execute("idProviderUrl", params).toString();
  }

  public String loginUrl(final List<String> params) {
    return execute("loginUrl", params).toString();
  }

  public String logoutUrl(final List<String> params) {
    return execute("logoutUrl", params).toString();
  }

  public String imagePlaceholder(final List<String> params) {
    return execute("imagePlaceholder", params).toString();
  }

  public String processHtml(final List<String> params) {
    return execute("processHtml", params).toString();
  }

  /**
   * Reads the phrases of the application being previewed on every call, rather than going through XP's
   * {@code i18n.localize} view function, which caches a bundle per application and locale and so would not show an
   * edited {@code phrases.properties} until the application was deployed again. See {@link Phrases}.
   */
  public String localize(final List<String> params) {
    final ApplicationKey application = getParamValue(params, "_application")
      .map(ApplicationKey::from)
      .orElse(this.applicationKey);

    return Phrases.localize(
      this.resourceServiceSupplier.get(),
      application,
      getParamValue(params, "_locale").map(Locale::forLanguageTag).orElse(Locale.ROOT),
      getParamValue(params, "_key").orElse(null),
      valuesFrom(params),
      getParamValues(params, "_bundles")
    );
  }

  /**
   * Takes values the way lib-thymeleaf does: either one braced argument, "_values={a,b}", or a "_values" argument per
   * value. The previous implementation read only the first argument and split it on commas, which dropped the rest.
   */
  private List<String> valuesFrom(final List<String> params) {
    final List<String> values = getParamValues(params, "_values");

    if (values.size() == 1 && BRACED_VALUES.matcher(values.get(0)).find()) {
      final String inner = values.get(0).substring(1, values.get(0).length() - 1);

      return inner.isEmpty() ? List.of() : Arrays.asList(inner.split(","));
    }

    return values;
  }

  private Object execute(final String name, final List<String> args) {
    final ViewFunctionParams params = new ViewFunctionParams().name(name).args(args).portalRequest(this.portalRequest);
    return this.viewFunctionService.execute(params);
  }


  private List<String> getParamValues(final List<String> params, String key) {
    return params.stream()
      .filter(p -> p.startsWith(key + "="))
      .map(p -> p.substring(key.length() + 1))
      .collect(Collectors.toList());
  }

  private Optional<String> getParamValue(final List<String> params, String key) {
    return params.stream()
      .filter(p -> p.startsWith(key + "="))
      .map(p -> p.substring(key.length() + 1))
      .findFirst();
  }
}
