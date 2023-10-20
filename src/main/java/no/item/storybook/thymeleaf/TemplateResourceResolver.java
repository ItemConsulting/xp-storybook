package no.item.storybook.thymeleaf;

import com.enonic.xp.resource.ResourceKey;
import org.thymeleaf.templateresource.ITemplateResource;

interface TemplateResourceResolver
{
    ITemplateResource resolve( ResourceKey base, String location );
}
