package no.item.storybook.thymeleaf;

import org.thymeleaf.templateresource.ITemplateResource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

/**
 * Stands in for a template that no resource directory holds, so the resolver never has to name a path it would not
 * have been allowed to read. Thymeleaf turns this into a {@code TemplateInputException} naming where it looked.
 */
final class MissingTemplateResource implements ITemplateResource {
  private final String name;
  private final List<String> dirPaths;

  MissingTemplateResource(final String name, final List<String> dirPaths) {
    this.name = name;
    this.dirPaths = dirPaths;
  }

  @Override
  public String getDescription() {
    return this.dirPaths.isEmpty()
      ? this.name + " (no resource directories to resolve it from)"
      : this.name + " (not found in " + String.join(", ", this.dirPaths) + ")";
  }

  @Override
  public String getBaseName() {
    return this.name;
  }

  @Override
  public boolean exists() {
    return false;
  }

  @Override
  public Reader reader() throws IOException {
    throw new FileNotFoundException(getDescription());
  }

  @Override
  public ITemplateResource relative(final String relativeLocation) {
    return new MissingTemplateResource(relativeLocation, this.dirPaths);
  }
}
