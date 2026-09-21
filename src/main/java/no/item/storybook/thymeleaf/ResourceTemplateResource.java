package no.item.storybook.thymeleaf;

import com.enonic.xp.resource.Resource;
import org.thymeleaf.templateresource.ITemplateResource;

import java.io.IOException;
import java.io.Reader;

/**
 * A template read from an installed application's resources rather than from disk, which is what makes
 * {@code xpAppName} work for templates that are only present inside a jar.
 */
final class ResourceTemplateResource implements ITemplateResource {
  private final Resource resource;
  private final StorybookTemplateResolver resolver;

  ResourceTemplateResource(final Resource resource, final StorybookTemplateResolver resolver) {
    this.resource = resource;
    this.resolver = resolver;
  }

  @Override
  public String getDescription() {
    return this.resource.getKey().toString();
  }

  @Override
  public String getBaseName() {
    final String name = this.resource.getKey().getName();
    final int dot = name.lastIndexOf('.');
    return dot == -1 ? name : name.substring(0, dot);
  }

  @Override
  public boolean exists() {
    return this.resource.exists();
  }

  @Override
  public Reader reader() throws IOException {
    return this.resource.openReader();
  }

  @Override
  public ITemplateResource relative(final String relativeLocation) {
    return this.resolver.resolveRelativeToResource(this.resource, relativeLocation);
  }
}
