package no.item.storybook;

import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.resource.Resource;
import com.enonic.xp.resource.ResourceBase;
import com.enonic.xp.resource.ResourceKey;
import com.enonic.xp.resource.ResourceKeys;
import com.enonic.xp.resource.ResourceProcessor;
import com.enonic.xp.resource.ResourceService;
import com.enonic.xp.resource.UrlResource;
import com.enonic.xp.vfs.VirtualFile;
import com.google.common.io.ByteSource;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An in-memory stand-in for XP's resource service, so the resolvers can be tested without a running server.
 *
 * <p>{@link ResourceBase} implements everything on top of {@code getBytes()}, so a fake resource is a handful of
 * lines and no mocking library is needed.
 */
public final class FakeResourceService implements ResourceService {
  /** Keyed on the resource URI, e.g. "com.example.app:/parts/card.html", so several applications can be held. */
  private final Map<String, String> resources = new LinkedHashMap<>();

  private String resolverName = "file";

  public FakeResourceService put(final ApplicationKey applicationKey, final String path, final String content) {
    this.resources.put(ResourceKey.from(applicationKey, path).toString(), content);
    return this;
  }

  /** "file" is what XP tags a resource read from a source directory; "bundle" one read from the installed jar. */
  public FakeResourceService withResolverName(final String resolverName) {
    this.resolverName = resolverName;
    return this;
  }

  @Override
  public Resource getResource(final ResourceKey key) {
    final String content = this.resources.get(key.toString());

    return content == null ? new UrlResource(key, null) : new FakeResource(key, content, this.resolverName);
  }

  @Override
  public ResourceKeys findFiles(final ApplicationKey applicationKey, final String pattern) {
    throw new UnsupportedOperationException("findFiles is deliberately unused: a template path is a literal, not a regex");
  }

  @Override
  public <K, V> V processResource(final ResourceProcessor<K, V> processor) {
    throw new UnsupportedOperationException();
  }

  @Override
  public VirtualFile getVirtualFile(final ResourceKey key) {
    throw new UnsupportedOperationException();
  }

  private static final class FakeResource extends ResourceBase {
    private final byte[] bytes;
    private final String resolverName;

    private FakeResource(final ResourceKey key, final String content, final String resolverName) {
      super(key);
      this.bytes = content.getBytes(StandardCharsets.UTF_8);
      this.resolverName = resolverName;
    }

    @Override
    public boolean exists() {
      return true;
    }

    @Override
    public long getSize() {
      return this.bytes.length;
    }

    @Override
    public long getTimestamp() {
      return 0;
    }

    @Override
    public ByteSource getBytes() {
      return ByteSource.wrap(this.bytes);
    }

    @Override
    public String getResolverName() {
      return this.resolverName;
    }
  }
}
