package no.item.storybook.freemarker;

import com.enonic.xp.app.ApplicationKey;
import com.enonic.xp.i18n.LocaleService;
import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.url.PortalUrlService;
import com.enonic.xp.resource.ResourceService;
import com.enonic.xp.script.bean.BeanContext;
import com.enonic.xp.script.bean.ScriptBean;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import no.item.freemarker.FreemarkerPortalObject;

public class StorybookScriptBean implements ScriptBean {
  private Supplier<PortalUrlService> portalUrlServiceSupplier;
  private Supplier<PortalRequest> portalRequestSupplier;
  private Supplier<ResourceService> resourceServiceSupplier;
  private Supplier<LocaleService> localeServiceSupplier;
  private ApplicationKey applicationKey;

  @Override
  public void initialize(BeanContext context) {
    this.portalUrlServiceSupplier = context.getService(PortalUrlService.class);
    this.portalRequestSupplier = context.getBinding(PortalRequest.class);
    this.resourceServiceSupplier = context.getService(ResourceService.class);
    this.localeServiceSupplier = context.getService(LocaleService.class);
    this.applicationKey = context.getApplicationKey();
  }

  public FreemarkerPortalObject getPortalObject(String baseDirPath) {
    return new StorybookPortalObject(portalUrlServiceSupplier, localeServiceSupplier, portalRequestSupplier, applicationKey, baseDirPath);
  }

  public TemplateErrorCollector newTemplateErrorCollector() {
    return new TemplateErrorCollector();
  }

  public MultiTemplateLoader getFileAndResourceTemplateLoader(List<String> dirPaths, String appName) {
    List<TemplateLoader> loaders = createFileTemplateLoaders(dirPaths);

    if (appName != null) {
      ResourceTemplateLoader loader = new ResourceTemplateLoader(this.resourceServiceSupplier, appName);
      loaders.add(loader);
    }

    return new MultiTemplateLoader(loaders.toArray((TemplateLoader[]::new)));
  }


  private static List<TemplateLoader> createFileTemplateLoaders(List<String> baseDirPaths) {
    return baseDirPaths.stream()
      .map(baseDir -> {
        try {
          return new FileTemplateLoader(new File(baseDir));
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      })
      .collect(Collectors.toList());
  }
}
