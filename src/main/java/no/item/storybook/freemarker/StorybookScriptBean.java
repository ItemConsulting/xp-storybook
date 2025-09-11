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
  private Supplier<ViewFunctionService> viewFunctionServiceSupplier;
  private Supplier<PortalUrlService> portalUrlServiceSupplier;
  private Supplier<PortalRequest> portalRequestSupplier;

  @Override
  public void initialize(BeanContext context) {
    this.viewFunctionServiceSupplier = context.getService(ViewFunctionService.class);
    this.portalUrlServiceSupplier = context.getService(PortalUrlService.class);
    this.portalRequestSupplier = context.getBinding(PortalRequest.class);
  }

  public FreemarkerPortalObject getPortalObject(String baseDirPath) {
    return new StorybookPortalObject(portalUrlServiceSupplier, viewFunctionServiceSupplier, portalRequestSupplier, baseDirPath);
  }

  public Map<String, TemplateDirectiveModel> createLegacyDirectives(String baseDirPath) {
    Map<String, TemplateDirectiveModel> directives = new HashMap<>();

    directives.put("pageUrl", new LegacyDirectiveModel(viewFunctionServiceSupplier, "pageUrl", "id", "path", "type"));
    directives.put("imageUrl", new LegacyDirectiveModel(viewFunctionServiceSupplier, "imageUrl", "id", "path", "format", "scale", "quality", "background", "filter", "type"));
    directives.put("attachmentUrl", new LegacyDirectiveModel(viewFunctionServiceSupplier, "attachmentUrl", "id", "path", "name", "label", "download", "type"));
    directives.put("componentUrl", new LegacyDirectiveModel(viewFunctionServiceSupplier, "componentUrl", "id", "path", "component", "type"));
    directives.put("serviceUrl", new LegacyDirectiveModel(viewFunctionServiceSupplier, "serviceUrl", "service", "application", "type"));
    directives.put("processHtml", new LegacyDirectiveModel(viewFunctionServiceSupplier, "processHtml", "value", "type"));
    directives.put("imagePlaceholder", new LegacyDirectiveModel(viewFunctionServiceSupplier, "imagePlaceholder"));
    directives.put("assetUrl", new AssetUrlTemplateDirectiveModel());
    directives.put("localize", baseDirPath != null
      ? new LocalizeTemplateDirectiveModel(baseDirPath)
      : new LegacyDirectiveModel(viewFunctionServiceSupplier, "i18n.localize", "key", "locale"));

    return directives;
  }
}
