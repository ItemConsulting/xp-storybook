package no.item.storybook.thymeleaf;

import com.enonic.lib.thymeleaf.view.ViewFunctionService;
import com.enonic.xp.portal.PortalRequestAccessor;
import com.enonic.xp.resource.ResourceService;
import com.enonic.xp.script.bean.BeanContext;
import com.enonic.xp.script.bean.ScriptBean;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.dialect.IDialect;
import org.thymeleaf.standard.StandardDialect;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public final class ThymeleafService implements ScriptBean {
  private BeanContext context;
  private Supplier<ResourceService> resourceServiceSupplier;

  /**
   * @param dirPaths          the directories to resolve templates from, in order.
   * @param appName           application to resolve templates from when they are not on disk, or {@code null}.
   * @param i18nBaseDirPath   the directory holding the phrases bundle, which is the one the template was found in.
   */
  public ThymeleafFileProcessor newFileProcessor(final List<String> dirPaths, final String appName, final String i18nBaseDirPath) {
    return new ThymeleafFileProcessor(newEngine(dirPaths, appName), createViewFunctions(i18nBaseDirPath));
  }

  public ThymeleafInlineProcessor newInlineTemplateProcessor(final List<String> dirPaths, final String appName, final String i18nBaseDirPath) {
    return new ThymeleafInlineProcessor(newEngine(dirPaths, appName), createViewFunctions(i18nBaseDirPath));
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
  private TemplateEngine newEngine(final List<String> dirPaths, final String appName) {
    final TemplateEngine engine = new TemplateEngine();

    final Set<IDialect> dialects = Set.of(new ExtensionDialectImpl(), new StandardDialect());
    engine.setDialects(dialects);

    final StorybookTemplateResolver resolver = new StorybookTemplateResolver(dirPaths, appName, this.resourceServiceSupplier);
    resolver.setSuffix(".html");
    engine.setTemplateResolver(resolver);

    return engine;
  }

  private ThymeleafViewFunctions createViewFunctions(final String i18nBaseDirPath) {
    final ThymeleafViewFunctions functions = new ThymeleafViewFunctions(i18nBaseDirPath);
    functions.viewFunctionService = this.context.getService(ViewFunctionService.class).get();
    functions.portalRequest = PortalRequestAccessor.get();
    return functions;
  }
}
