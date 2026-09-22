package no.item.storybook.thymeleaf;

import com.enonic.lib.thymeleaf.view.ViewFunctionService;
import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.portal.PortalRequestAccessor;
import com.enonic.xp.resource.ResourceService;
import com.enonic.xp.script.bean.BeanContext;
import com.enonic.xp.script.bean.ScriptBean;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.dialect.IDialect;
import org.thymeleaf.standard.StandardDialect;

import java.util.Set;
import java.util.function.Supplier;

public final class ThymeleafService implements ScriptBean {
  private BeanContext context;
  private Supplier<ResourceService> resourceServiceSupplier;

  /**
   * @param appName the application whose resources hold the template, and whose phrases {@code portal.localize}
   *     resolves against.
   */
  public ThymeleafFileProcessor newFileProcessor(final String appName) {
    final ApplicationKey applicationKey = ApplicationKey.from(appName);

    return new ThymeleafFileProcessor(newEngine(applicationKey), createViewFunctions(applicationKey));
  }

  public ThymeleafInlineProcessor newInlineTemplateProcessor(final String appName) {
    final ApplicationKey applicationKey = ApplicationKey.from(appName);

    return new ThymeleafInlineProcessor(newEngine(applicationKey), createViewFunctions(applicationKey));
  }

  @Override
  public void initialize(final BeanContext context) {
    this.context = context;
    this.resourceServiceSupplier = context.getService(ResourceService.class);
  }

  /**
   * A Thymeleaf {@link TemplateEngine} refuses to have its resolver replaced once it has processed a template, so the
   * engine is built per render instead of being reconfigured. That also matches how the FreeMarker flavor sets its
   * template loader on every call, and keeps two requests with different resource directories independent.
   */
  private TemplateEngine newEngine(final ApplicationKey applicationKey) {
    final TemplateEngine engine = new TemplateEngine();

    final Set<IDialect> dialects = Set.of(new ExtensionDialectImpl(), new StandardDialect());
    engine.setDialects(dialects);

    final StorybookTemplateResolver resolver = new StorybookTemplateResolver(applicationKey, this.resourceServiceSupplier);
    resolver.setSuffix(".html");
    engine.setTemplateResolver(resolver);

    return engine;
  }

  private ThymeleafViewFunctions createViewFunctions(final ApplicationKey applicationKey) {
    final ThymeleafViewFunctions functions = new ThymeleafViewFunctions(applicationKey, this.resourceServiceSupplier);
    functions.viewFunctionService = this.context.getService(ViewFunctionService.class).get();
    functions.portalRequest = PortalRequestAccessor.get();
    return functions;
  }
}
