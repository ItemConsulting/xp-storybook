package no.item.storybook.freemarker;

import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.i18n.LocaleService;
import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.url.PortalUrlService;
import com.enonic.xp.resource.ResourceService;
import com.enonic.xp.script.bean.BeanContext;
import com.enonic.xp.script.bean.ScriptBean;
import freemarker.cache.TemplateLoader;
import no.item.freemarker.FreemarkerPortalObject;

import java.util.function.Supplier;

public class StorybookScriptBean implements ScriptBean {
  private Supplier<PortalUrlService> portalUrlServiceSupplier;
  private Supplier<PortalRequest> portalRequestSupplier;
  private Supplier<ResourceService> resourceServiceSupplier;
  private Supplier<LocaleService> localeServiceSupplier;

  @Override
  public void initialize(BeanContext context) {
    this.portalUrlServiceSupplier = context.getService(PortalUrlService.class);
    this.portalRequestSupplier = context.getBinding(PortalRequest.class);
    this.resourceServiceSupplier = context.getService(ResourceService.class);
    this.localeServiceSupplier = context.getService(LocaleService.class);
  }

  /**
   * The {@code portal} object for templates of {@code appName}.
   *
   * <p>Deliberately keyed on the application being previewed rather than on this one
   * ({@code context.getApplicationKey()}), so that {@code portal.localize} resolves the phrases of the app whose
   * template is rendering — the same ones it would resolve in production.
   */
  public FreemarkerPortalObject getPortalObject(String appName) {
    return new StorybookPortalObject(
      portalUrlServiceSupplier,
      localeServiceSupplier,
      portalRequestSupplier,
      resourceServiceSupplier,
      ApplicationKey.from(appName)
    );
  }

  public TemplateLoader getResourceTemplateLoader(String appName) {
    return new ResourceTemplateLoader(this.resourceServiceSupplier, ApplicationKey.from(appName));
  }
}
