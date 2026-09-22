package no.item.storybook.app;

import com.enonic.xp.app.Application;
import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.app.ApplicationService;
import com.enonic.xp.resource.Resource;
import com.enonic.xp.resource.ResourceKey;
import com.enonic.xp.resource.ResourceService;
import com.enonic.xp.script.bean.BeanContext;
import com.enonic.xp.script.bean.ScriptBean;

import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Answers what the endpoint needs to know about the application it was asked to render from, so that the common
 * mistakes — a misspelled application key, an application that was never deployed — are reported as such rather than
 * as a template that could not be found.
 */
public final class ApplicationScriptBean implements ScriptBean {
  public static final String MISSING = "MISSING";
  public static final String STOPPED = "STOPPED";
  public static final String STARTED = "STARTED";

  /**
   * The resolver that served a resource. XP tags a resource read from an application's source directory "file" and
   * one read from the installed jar "bundle". There is no exported constant for either, so this is treated as an
   * advisory signal: only an exact "bundle" match means anything, and only to warn that edits will not show up.
   */
  public static final String BUNDLE_RESOLVER = "bundle";

  private Supplier<ApplicationService> applicationServiceSupplier;
  private Supplier<ResourceService> resourceServiceSupplier;

  @Override
  public void initialize(final BeanContext context) {
    this.applicationServiceSupplier = context.getService(ApplicationService.class);
    this.resourceServiceSupplier = context.getService(ResourceService.class);
  }

  /**
   * @return whether {@code appName} can be an application key at all. {@link ApplicationKey#from(String)} throws for
   *     anything else, and that would otherwise surface as a 500.
   */
  public boolean isValidApplicationKey(final String appName) {
    return toApplicationKey(appName) != null;
  }

  /**
   * @return {@link #MISSING}, {@link #STOPPED} or {@link #STARTED}.
   */
  public String getApplicationState(final String appName) {
    final ApplicationKey key = toApplicationKey(appName);

    if (key == null) {
      return MISSING;
    }

    final Application application = this.applicationServiceSupplier.get().get(key);

    if (application == null) {
      return MISSING;
    }

    return application.isStarted() ? STARTED : STOPPED;
  }

  public boolean resourceExists(final String appName, final String path) {
    final Resource resource = getResource(appName, path);

    return resource != null && resource.exists();
  }

  /**
   * @return the name of the resolver that served the resource, or {@code null} if there is none.
   */
  public String getResolverName(final String appName, final String path) {
    final Resource resource = getResource(appName, path);

    return resource == null || !resource.exists() ? null : resource.getResolverName();
  }

  /**
   * @return every installed application, comma separated, to help whoever misspelled one.
   */
  public String getInstalledApplicationNames() {
    return StreamSupport.stream(this.applicationServiceSupplier.get().getInstalledApplications().spliterator(), false)
      .map(application -> application.getKey().toString())
      .sorted()
      .collect(Collectors.joining(", "));
  }

  private Resource getResource(final String appName, final String path) {
    final ApplicationKey key = toApplicationKey(appName);

    return key == null ? null : this.resourceServiceSupplier.get().getResource(ResourceKey.from(key, withLeadingSlash(path)));
  }

  private static ApplicationKey toApplicationKey(final String appName) {
    if (appName == null || appName.isBlank()) {
      return null;
    }

    try {
      return ApplicationKey.from(appName);
    } catch (final RuntimeException e) {
      return null;
    }
  }

  private static String withLeadingSlash(final String path) {
    return path.startsWith("/") ? path : "/" + path;
  }
}
