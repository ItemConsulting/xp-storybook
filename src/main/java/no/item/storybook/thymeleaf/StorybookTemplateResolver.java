package no.item.storybook.thymeleaf;

import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.resource.Resource;
import com.enonic.xp.resource.ResourceKey;
import com.enonic.xp.resource.ResourceService;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.cache.ICacheEntryValidity;
import org.thymeleaf.cache.NonCacheableCacheEntryValidity;
import org.thymeleaf.exceptions.ConfigurationException;
import org.thymeleaf.templateresolver.AbstractConfigurableTemplateResolver;
import org.thymeleaf.templateresource.FileTemplateResource;
import org.thymeleaf.templateresource.ITemplateResource;
import org.thymeleaf.templateresource.StringTemplateResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Resolves Thymeleaf templates the way the FreeMarker flavor resolves its own: across every directory in
 * {@code xpResourcesDirPath}, falling back to the resources of the application named by {@code xpAppName}.
 *
 * <p>No prefix is configured on purpose. {@link AbstractConfigurableTemplateResolver} supports a single prefix, and
 * this resolver needs several roots, so it resolves the name against each of them itself.
 */
public class StorybookTemplateResolver extends AbstractConfigurableTemplateResolver {
  /**
   * Marks the root template of an inline render. The template string is passed where a name normally goes, so
   * without this the resolver would have to guess whether it was handed a template or a path.
   */
  static final String INLINE_ATTRIBUTE = "no.item.storybook.inline";

  /**
   * The extensions a Thymeleaf template may have. The endpoint checks only the template it was asked for
   * (TEMPLATE_EXTENSIONS in lib/storybook/params.ts), so without the same check here any readable file under a
   * resource directory could be disclosed by including it from a template.
   *
   * <p>These are the HTML extensions Thymeleaf itself recognises, and deliberately not the FreeMarker ones from the
   * endpoint's list: a name Thymeleaf does not recognise has ".html" appended before it reaches this resolver, so a
   * template can never be resolved under a ".ftl" name anyway. The other extensions Thymeleaf knows (".xml", ".js",
   * ".json", ".css", ".txt") are left out, since this renders in HTML mode only.
   */
  private static final List<String> TEMPLATE_EXTENSIONS = List.of(".html", ".htm", ".xhtml");

  private final List<String> dirPaths;
  private final String appName;
  private final Supplier<ResourceService> resourceServiceSupplier;

  /**
   * Where each template resolved to, keyed by the name it was requested under, which is the name Thymeleaf passes
   * back as {@code ownerTemplate} for the templates it includes. Thymeleaf hands the resolver a name and not the
   * resource the owner resolved to, so a relative include can only be resolved against the directory its owner was
   * really found in if that is remembered here. Two includes reaching different templates under one relative name
   * within a single render would collide, which is why this is per-render state: the engine, and with it this
   * resolver, is rebuilt for every render.
   */
  private final Map<String, Path> resolvedDirs = new ConcurrentHashMap<>();
  private final Map<String, ResourceKey> resolvedKeys = new ConcurrentHashMap<>();

  public StorybookTemplateResolver(final List<String> dirPaths, final String appName,
                                   final Supplier<ResourceService> resourceServiceSupplier) {
    this.dirPaths = List.copyOf(dirPaths);
    this.appName = appName;
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
    // resolved from the root, which is how this resolver has always treated it.
    if (ownerTemplate != null && template.startsWith(".")) {
      final ITemplateResource relative = resolveRelativeToOwner(ownerTemplate, template, resourceName, characterEncoding);

      if (relative != null) {
        return relative;
      }
      // The owner is an inline template, which has no directory of its own, so "./x" means the same as "x".
    }

    for (final String dirPath : this.dirPaths) {
      final Path base = toBase(dirPath);
      final ITemplateResource resource = fileResource(base, base.resolve(stripLeadingSlashes(resourceName)), template, characterEncoding);

      if (resource != null) {
        return resource;
      }
    }

    final ITemplateResource fromApplication = applicationResource(applicationKeyFor(resourceName), template);

    if (fromApplication != null) {
      return fromApplication;
    }

    // Nothing matched. Deliberately not a FileTemplateResource on a best-guess path: for a name that escaped its
    // directory that would read the very file the check above refused.
    return new MissingTemplateResource(resourceName, this.dirPaths);
  }

  @Override
  protected ICacheEntryValidity computeValidity(final IEngineConfiguration configuration, final String ownerTemplate, final String template, final Map<String, Object> templateResolutionAttributes) {
    return NonCacheableCacheEntryValidity.INSTANCE;
  }

  /**
   * Resolves a template referenced from one that was itself loaded out of an application's resources.
   */
  ITemplateResource resolveRelativeToResource(final Resource owner, final String relativeLocation) {
    final ResourceKey key = resolveAgainst(owner.getKey(), relativeLocation);
    final ITemplateResource resource = applicationResource(key, relativeLocation);

    return resource != null ? resource : new MissingTemplateResource(relativeLocation, this.dirPaths);
  }

  /**
   * @return the template resolved against the directory, or the application resource, its owner was found in, or
   *     {@code null} when the owner has neither — which is the case for an inline template.
   */
  private ITemplateResource resolveRelativeToOwner(final String ownerTemplate, final String template, final String resourceName, final String characterEncoding) {
    final Path ownerDir = this.resolvedDirs.get(ownerTemplate);

    if (ownerDir != null) {
      final Path candidate = ownerDir.resolve(stripLeadingSlashes(resourceName)).normalize();
      final Path base = resourceDirectoryHolding(candidate);
      final ITemplateResource resource = base == null ? null : fileResource(base, candidate, template, characterEncoding);

      return resource != null ? resource : new MissingTemplateResource(resourceName, this.dirPaths);
    }

    final ResourceKey ownerKey = this.resolvedKeys.get(ownerTemplate);

    if (ownerKey != null) {
      final ITemplateResource resource = applicationResource(resolveAgainst(ownerKey, resourceName), template);

      return resource != null ? resource : new MissingTemplateResource(resourceName, this.dirPaths);
    }

    return null;
  }

  /**
   * @return the template at {@code candidate}, or {@code null} when it is not a template file inside {@code base}.
   */
  private ITemplateResource fileResource(final Path base, final Path candidate, final String template, final String characterEncoding) {
    final Path normalized = candidate.normalize();

    // FreeMarker's FileTemplateLoader refuses to leave its base directory, and this makes the same guarantee.
    if (!normalized.startsWith(base) || !isTemplateName(normalized.toString()) || !Files.isRegularFile(normalized)) {
      return null;
    }

    this.resolvedDirs.put(template, normalized.getParent());

    return new FileTemplateResource(normalized.toString(), characterEncoding);
  }

  /**
   * @return the template held by the application named by {@code xpAppName}, or {@code null} when no application was
   *     given, the key is not a template, or the application holds no such resource.
   */
  private ITemplateResource applicationResource(final ResourceKey key, final String template) {
    if (key == null || !isTemplateName(key.getName())) {
      return null;
    }

    final Resource resource = this.resourceServiceSupplier.get().getResource(key);

    if (!resource.exists()) {
      return null;
    }

    this.resolvedKeys.put(template, key);

    return new ResourceTemplateResource(resource, this);
  }

  private ResourceKey applicationKeyFor(final String resourceName) {
    return this.appName == null
      ? null
      : ResourceKey.from(ApplicationKey.from(this.appName), withLeadingSlash(resourceName));
  }

  /**
   * @return the resource directory holding {@code candidate}, or {@code null} when it lies outside all of them.
   */
  private Path resourceDirectoryHolding(final Path candidate) {
    for (final String dirPath : this.dirPaths) {
      final Path base = toBase(dirPath);

      if (candidate.startsWith(base)) {
        return base;
      }
    }

    return null;
  }

  /**
   * A {@link ResourceKey} names a file, so a sibling of it is one directory up.
   */
  private static ResourceKey resolveAgainst(final ResourceKey owner, final String location) {
    final String relative = stripLeadingSlashes(location);

    return withHtmlSuffix(location.startsWith("/") ? owner.resolve(relative) : owner.resolve("../" + relative));
  }

  private static Path toBase(final String dirPath) {
    return Paths.get(dirPath).toAbsolutePath().normalize();
  }

  private static boolean isTemplateName(final String name) {
    final String lowerCased = name.toLowerCase(Locale.ROOT);

    return TEMPLATE_EXTENSIONS.stream().anyMatch(lowerCased::endsWith);
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
