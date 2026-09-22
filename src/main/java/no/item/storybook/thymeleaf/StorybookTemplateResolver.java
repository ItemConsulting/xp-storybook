package no.item.storybook.thymeleaf;

import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.resource.Resource;
import com.enonic.xp.resource.ResourceKey;
import com.enonic.xp.resource.ResourceService;
import no.item.storybook.render.TemplateExtensions;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.cache.ICacheEntryValidity;
import org.thymeleaf.cache.NonCacheableCacheEntryValidity;
import org.thymeleaf.exceptions.ConfigurationException;
import org.thymeleaf.templateresolver.AbstractConfigurableTemplateResolver;
import org.thymeleaf.templateresource.ITemplateResource;
import org.thymeleaf.templateresource.StringTemplateResource;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Resolves Thymeleaf templates from the resources of one XP application.
 *
 * <p>Reads cannot leave that application: {@link ResourceKey} normalises its path with
 * {@code Files.simplifyPath("/" + path)}, which clamps a leading ".." at the root, and the application key is fixed
 * when this resolver is built rather than taken from the template name. In development mode XP serves an
 * application's resources from its source directory before the installed jar, so an edited template is picked up
 * without a rebuild.
 */
public class StorybookTemplateResolver extends AbstractConfigurableTemplateResolver {
  /**
   * Marks the root template of an inline render. The template string is passed where a name normally goes, so
   * without this the resolver would have to guess whether it was handed a template or a path.
   */
  static final String INLINE_ATTRIBUTE = "no.item.storybook.inline";

  private final ApplicationKey applicationKey;
  private final Supplier<ResourceService> resourceServiceSupplier;

  /**
   * Where each template resolved to, keyed by the name it was requested under, which is the name Thymeleaf passes
   * back as {@code ownerTemplate} for the templates it includes. Thymeleaf hands the resolver a name and not the
   * resource the owner resolved to, so a relative include can only be resolved against the template its owner was
   * really found at if that is remembered here. Two includes reaching different templates under one relative name
   * within a single render would collide, which is why this is per-render state: the engine, and with it this
   * resolver, is rebuilt for every render.
   */
  private final Map<String, ResourceKey> resolvedKeys = new ConcurrentHashMap<>();

  public StorybookTemplateResolver(final ApplicationKey applicationKey,
                                   final Supplier<ResourceService> resourceServiceSupplier) {
    this.applicationKey = applicationKey;
    this.resourceServiceSupplier = resourceServiceSupplier;
  }

  @Override
  public void setUseDecoupledLogic(final boolean useDecoupledLogic) {
    if (useDecoupledLogic) {
      throw new ConfigurationException("The 'useDecoupledLogic' flag is not allowed for String template resolution");
    }
    super.setUseDecoupledLogic(useDecoupledLogic);
  }

  @Override
  protected ITemplateResource computeTemplateResource(final IEngineConfiguration configuration, final String ownerTemplate, final String template, final String resourceName, final String characterEncoding, final Map<String, Object> templateResolutionAttributes) {
    if (ownerTemplate == null && isInline(templateResolutionAttributes)) {
      return new StringTemplateResource(template);
    }

    // A template referring to "./other.html" means the directory of the template that included it. Anything else is
    // resolved from the application root, which is how this resolver has always treated it.
    if (ownerTemplate != null && template.startsWith(".")) {
      final ResourceKey ownerKey = this.resolvedKeys.get(ownerTemplate);

      if (ownerKey != null) {
        return resourceOrMissing(resolveAgainst(ownerKey, resourceName), template, resourceName);
      }
      // The owner is an inline template, which has no key of its own, so "./x" means the same as "x".
    }

    return resourceOrMissing(ResourceKey.from(this.applicationKey, withLeadingSlash(resourceName)), template, resourceName);
  }

  @Override
  protected ICacheEntryValidity computeValidity(final IEngineConfiguration configuration, final String ownerTemplate, final String template, final Map<String, Object> templateResolutionAttributes) {
    return NonCacheableCacheEntryValidity.INSTANCE;
  }

  /**
   * Resolves a template referenced from one that was itself loaded out of the application's resources.
   */
  ITemplateResource resolveRelativeToResource(final Resource owner, final String relativeLocation) {
    return resourceOrMissing(resolveAgainst(owner.getKey(), relativeLocation), relativeLocation, relativeLocation);
  }

  private ITemplateResource resourceOrMissing(final ResourceKey key, final String template, final String resourceName) {
    final ITemplateResource resource = applicationResource(key, template);

    return resource != null ? resource : new MissingTemplateResource(resourceName, this.applicationKey);
  }

  /**
   * @return the template held by the application, or {@code null} when the key is not a template or no such resource
   *     exists.
   */
  private ITemplateResource applicationResource(final ResourceKey key, final String template) {
    if (!TemplateExtensions.isTemplate(key.getName())) {
      return null;
    }

    final Resource resource = this.resourceServiceSupplier.get().getResource(key);

    if (!resource.exists()) {
      return null;
    }

    this.resolvedKeys.put(template, key);

    return new ResourceTemplateResource(resource, this);
  }

  /**
   * A {@link ResourceKey} names a file, so a sibling of it is one directory up.
   */
  private static ResourceKey resolveAgainst(final ResourceKey owner, final String location) {
    final String relative = stripLeadingSlashes(location);

    return withHtmlSuffix(location.startsWith("/") ? owner.resolve(relative) : owner.resolve("../" + relative));
  }

  private static ResourceKey withHtmlSuffix(final ResourceKey key) {
    return key.getName().indexOf('.') == -1 ? ResourceKey.from(key.getUri() + ".html") : key;
  }

  private static String withLeadingSlash(final String path) {
    return path.startsWith("/") ? path : "/" + path;
  }

  private static String stripLeadingSlashes(final String path) {
    int i = 0;
    while (i < path.length() && path.charAt(i) == '/') {
      i++;
    }
    return path.substring(i);
  }

  private static boolean isInline(final Map<String, Object> templateResolutionAttributes) {
    return templateResolutionAttributes != null && Boolean.TRUE.equals(templateResolutionAttributes.get(INLINE_ATTRIBUTE));
  }
}
