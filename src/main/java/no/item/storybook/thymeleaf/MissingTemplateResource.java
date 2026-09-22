package no.item.storybook.thymeleaf;

import com.enonic.xp.app.ApplicationKey;
import org.thymeleaf.templateresource.ITemplateResource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;

/**
 * Stands in for a template the application does not hold. Thymeleaf turns this into a
 * {@code TemplateInputException} naming what it looked for.
 */
final class MissingTemplateResource implements ITemplateResource {
  private final String name;
  private final ApplicationKey applicationKey;

  MissingTemplateResource(final String name, final ApplicationKey applicationKey) {
    this.name = name;
    this.applicationKey = applicationKey;
  }

  @Override
  public String getDescription() {
    return this.name + " (not found in application " + this.applicationKey + ")";
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
    return new MissingTemplateResource(relativeLocation, this.applicationKey);
  }
}
