package no.item.storybook.thymeleaf;

import org.apache.commons.lang.StringUtils;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.cache.ICacheEntryValidity;
import org.thymeleaf.cache.NonCacheableCacheEntryValidity;
import org.thymeleaf.exceptions.ConfigurationException;
import org.thymeleaf.templateresolver.AbstractConfigurableTemplateResolver;
import org.thymeleaf.templateresource.FileTemplateResource;
import org.thymeleaf.templateresource.ITemplateResource;
import org.thymeleaf.templateresource.StringTemplateResource;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;


public class StorybookTemplateResolver extends AbstractConfigurableTemplateResolver {
  public StorybookTemplateResolver() {
    super();
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
    if (isHtmlContent(template)) {
      return new StringTemplateResource(template);  // TODO Set basePath to be used to find referenced
    } else if (ownerTemplate != null && template.startsWith(".")) { // Resolves relative filepath
      try {
        String path = Paths.get(this.getPrefix(), StringUtils.substringBeforeLast(ownerTemplate, "/"), template).toFile().getCanonicalPath();
        return new FileTemplateResource(path, characterEncoding);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      return new FileTemplateResource(resourceName, characterEncoding);
    }
  }

  @Override
  protected ICacheEntryValidity computeValidity(final IEngineConfiguration configuration, final String ownerTemplate, final String template, final Map<String, Object> templateResolutionAttributes) {
    return NonCacheableCacheEntryValidity.INSTANCE;
  }

  private boolean isHtmlContent(String template) {
    return template.contains("<") && template.contains(">");
  }
}

