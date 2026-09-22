package no.item.storybook.freemarker;

import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.resource.Resource;
import com.enonic.xp.resource.ResourceKey;
import com.enonic.xp.resource.ResourceService;
import freemarker.cache.TemplateLoader;
import no.item.freemarker.ResourceTemplateSource;
import no.item.storybook.render.TemplateExtensions;

import java.io.IOException;
import java.io.Reader;
import java.util.function.Supplier;

/**
 * A {@link TemplateLoader} that loads templates from the resources of one XP application.
 *
 * <p>When Enonic XP runs in development mode, an application built with dev source paths resolves its resources from
 * the source directory on disk before the installed jar, so a template edited there is picked up without a rebuild.
 */
public class ResourceTemplateLoader implements TemplateLoader {
  private final Supplier<ResourceService> resourceServiceSupplier;
  private final ApplicationKey applicationKey;

  /**
   * @param resourceServiceSupplier to use for finding resources.
   * @param applicationKey the application whose resources may be loaded. Pinning the loader to one application is
   *     what keeps a template from reaching into another one.
   */
  public ResourceTemplateLoader(Supplier<ResourceService> resourceServiceSupplier, ApplicationKey applicationKey) {
    this.resourceServiceSupplier = resourceServiceSupplier;
    this.applicationKey = applicationKey;
  }

  @Override
  public Object findTemplateSource(String name) {
    if (!TemplateExtensions.isTemplate(name)) {
      return null;
    }

    Resource resource = findResource(name);

    return resource.exists() ? new ResourceTemplateSource(resource) : null;
  }

  @Override
  public long getLastModified(Object templateSource) {
    return ((ResourceTemplateSource) templateSource).getLastModified();
  }

  @Override
  public Reader getReader(Object templateSource, String encoding) {
    return ((ResourceTemplateSource) templateSource).getReader();
  }

  @Override
  public void closeTemplateSource(Object templateSource) throws IOException {
    ((ResourceTemplateSource) templateSource).close();
  }

  /**
   * Deliberately a direct lookup rather than {@code ResourceService.findFiles}, whose second argument is a regular
   * expression: a template path is a literal, so a path containing "+", "(" or "[" would fail to compile, a "." would
   * match any character, and every file in the application would be enumerated on each template load.
   */
  private Resource findResource(String name) {
    ResourceKey key = ResourceKey.from(applicationKey, name.startsWith("/") ? name : "/" + name);

    return resourceServiceSupplier.get().getResource(key);
  }
}
