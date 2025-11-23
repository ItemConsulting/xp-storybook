package no.item.storybook.freemarker;

import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.resource.Resource;
import com.enonic.xp.resource.ResourceKeys;
import com.enonic.xp.resource.ResourceService;
import freemarker.cache.TemplateLoader;
import no.item.freemarker.ResourceTemplateSource;

import java.io.IOException;
import java.io.Reader;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A {@link TemplateLoader} that loads templates from XP resources.
 */
public class ResourceTemplateLoader implements TemplateLoader {
  private final Supplier<ResourceService> resourceServiceSupplier;
  private final String appName;

  /**
   * Create a new {@link ResourceTemplateLoader} with the given {@link ResourceService}.
   * @param resourceServiceSupplier to use for finding resources.
   */
  public ResourceTemplateLoader(Supplier<ResourceService> resourceServiceSupplier, String appName) {
    this.resourceServiceSupplier = resourceServiceSupplier;
    this.appName = appName;
  }

  @Override
  public Object findTemplateSource(String name) {
    return findResource(name)
      .map(ResourceTemplateSource::new)
      .orElse(null);
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

  private Optional<Resource> findResource(String name) {
    ResourceService service = resourceServiceSupplier.get();
    ApplicationKey applicationKey = ApplicationKey.from(appName);
    ResourceKeys keys = service.findFiles(applicationKey, name);

    return Optional.ofNullable(keys.get(0)).map(service::getResource);
  }
}
