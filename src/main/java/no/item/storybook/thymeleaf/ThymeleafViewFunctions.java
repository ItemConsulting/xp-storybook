package no.item.storybook.thymeleaf;

import com.enonic.xp.portal.PortalRequest;
import com.enonic.lib.thymeleaf.view.ViewFunctionParams;
import com.enonic.lib.thymeleaf.view.ViewFunctionService;
import no.item.storybook.i18n.Phrases;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

final class ThymeleafViewFunctions {
  ViewFunctionService viewFunctionService;
  PortalRequest portalRequest;
  private final String baseDirPath;

  public ThymeleafViewFunctions(String baseDirPath) {
    this.baseDirPath = baseDirPath;
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

  public String localize(final List<String> params) {
    return getParamValue(params, "_key")
      .map(key -> Phrases.localize(baseDirPath, getLocaleFromParams(params), key, valuesFrom(params)))
      .orElse(Phrases.NOT_TRANSLATED);
  }

  private List<String> valuesFrom(final List<String> params) {
    return getParamValue(params, "_values")
      .map(valuesStr -> Arrays.asList(valuesStr.split(",")))
      .orElse(List.of());
  }


  private Object execute(final String name, final List<String> args) {
    final ViewFunctionParams params = new ViewFunctionParams().name(name).args(args).portalRequest(this.portalRequest);
    return this.viewFunctionService.execute(params);
  }


  private Locale getLocaleFromParams(final List<String> params) {
    return getParamValue(params, "_locale").map(Locale::forLanguageTag).orElse(Locale.ROOT);
  }

  private Optional<String> getParamValue(final List<String> params, String key) {
    return params.stream()
      .filter(p -> p.startsWith(key + "="))
      .map(p -> p.substring(key.length() + 1))
      .findFirst();
  }
}
