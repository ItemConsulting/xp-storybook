package no.item.storybook.freemarker;

import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.url.PortalUrlService;
import com.enonic.xp.portal.view.ViewFunctionService;
import com.enonic.xp.script.bean.BeanContext;
import com.enonic.xp.script.bean.ScriptBean;
import freemarker.template.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import no.item.freemarker.FreemarkerPortalObject;
import no.item.storybook.freemarker.directive.AssetUrlTemplateDirectiveModel;
import no.item.storybook.freemarker.directive.LegacyDirectiveModel;
import no.item.storybook.freemarker.directive.LocalizeTemplateDirectiveModel;

public class StorybookScriptBean implements ScriptBean {
  private ViewFunctionService service;
  private PortalUrlService urlService;
  private Supplier< PortalRequest > requestSupplier;

  @Override
  public void initialize(BeanContext context) {
    this.service = context.getService(ViewFunctionService.class).get();
    this.urlService = context.getService(PortalUrlService.class).get();
    this.requestSupplier = context.getService(PortalRequest.class);
  }

  public FreemarkerPortalObject getPortalObject(String baseDirPath) {
    return new StorybookPortalObject(urlService, service, requestSupplier, baseDirPath);
  }

  public Map<String, TemplateDirectiveModel> createLegacyDirectives(String baseDirPath) {
    Map<String, TemplateDirectiveModel> directives = new HashMap<>();

    directives.put("pageUrl", new LegacyDirectiveModel(service, "pageUrl", "id", "path", "type"));
    directives.put("imageUrl", new LegacyDirectiveModel(service, "imageUrl", "id", "path", "format", "scale", "quality", "background", "filter", "type"));
    directives.put("attachmentUrl", new LegacyDirectiveModel(service, "attachmentUrl", "id", "path", "name", "label", "download", "type"));
    directives.put("componentUrl", new LegacyDirectiveModel(service, "componentUrl", "id", "path", "component", "type"));
    directives.put("serviceUrl", new LegacyDirectiveModel(service, "serviceUrl", "service", "application", "type"));
    directives.put("processHtml", new LegacyDirectiveModel(service, "processHtml", "value", "type"));
    directives.put("imagePlaceholder", new LegacyDirectiveModel(service, "imagePlaceholder"));
    directives.put("assetUrl", new AssetUrlTemplateDirectiveModel());
    directives.put("localize", baseDirPath != null
      ? new LocalizeTemplateDirectiveModel(baseDirPath)
      : new LegacyDirectiveModel(service, "i18n.localize", "key", "locale"));

    return directives;
  }
}
