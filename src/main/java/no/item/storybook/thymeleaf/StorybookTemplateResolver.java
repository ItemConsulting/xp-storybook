package no.item.storybook.thymeleaf;

import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.cache.ICacheEntryValidity;
import org.thymeleaf.cache.NonCacheableCacheEntryValidity;
import org.thymeleaf.exceptions.ConfigurationException;
import org.thymeleaf.templateresolver.AbstractConfigurableTemplateResolver;
import org.thymeleaf.templateresource.FileTemplateResource;
import org.thymeleaf.templateresource.ITemplateResource;
import org.thymeleaf.templateresource.StringTemplateResource;

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
    if (templateResolutionAttributes != null && templateResolutionAttributes.get("sbType") == "inline") {
      return new StringTemplateResource(template);  // TODO Set basePath to be used to find referenced
    } else {
      return new FileTemplateResource(resourceName, characterEncoding);
    }
  }

  @Override
  protected ICacheEntryValidity computeValidity(final IEngineConfiguration configuration, final String ownerTemplate, final String template, final Map<String, Object> templateResolutionAttributes) {
    return NonCacheableCacheEntryValidity.INSTANCE;
  }
}
