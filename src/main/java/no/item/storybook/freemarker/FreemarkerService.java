package no.item.storybook.freemarker;

import com.enonic.xp.portal.PortalRequest;
import com.enonic.xp.portal.PortalRequestAccessor;
import com.enonic.xp.portal.view.ViewFunctionService;
import com.enonic.xp.script.bean.BeanContext;
import com.enonic.xp.script.bean.ScriptBean;
import freemarker.template.TemplateDirectiveModel;
import no.tine.xp.lib.freemarker.PortalViewFunction;
import no.tine.xp.lib.freemarker.ViewFunctionSpec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FreemarkerService implements ScriptBean {
  private BeanContext context;

  private final List<ViewFunctionSpec> viewFunctions;

  public FreemarkerService() {
    viewFunctions = new ArrayList<>();

    // Make these portal functions available as user-defined functions. Should now be possible to do: <@imageUrl id="11" scale="width(200)" customParam="123" />
    viewFunctions.add(new ViewFunctionSpec("pageUrl", "id", "path", "type"));
    viewFunctions.add(new ViewFunctionSpec("imageUrl", "id", "path", "format", "scale", "quality", "background", "filter", "type"));
    viewFunctions.add(new ViewFunctionSpec("attachmentUrl", "id", "path", "name", "label", "download", "type"));
    viewFunctions.add(new ViewFunctionSpec("componentUrl", "id", "path", "component", "type"));
    viewFunctions.add(new ViewFunctionSpec("serviceUrl", "service", "application", "type"));
    viewFunctions.add(new ViewFunctionSpec("processHtml", "value", "type"));
    viewFunctions.add(new ViewFunctionSpec("imagePlaceholder"));
  }

  @Override
  public void initialize(final BeanContext context) {
    this.context = context;
    FreemarkerFileProcessor.setupFreemarker();
    FreemarkerInlineTemplateProcessor.setupFreemarker();
  }

  // This is called from JavaScript
  public Object newFileProcessor() {
    return new FreemarkerFileProcessor(createViewFunctions());
  }

  // This is called from JavaScript
  public Object newInlineTemplateProcessor() {
    return new FreemarkerInlineTemplateProcessor(createViewFunctions());
  }


  private Map<String, TemplateDirectiveModel> createViewFunctions() {
    ViewFunctionService service = this.context.getService(ViewFunctionService.class).get();
    PortalRequest portalRequest = PortalRequestAccessor.get();

    Map<String, TemplateDirectiveModel> functions = new HashMap<>();

    viewFunctions.forEach(fnSpec ->
      functions.put(directiveName(fnSpec.getName()), new PortalViewFunction(fnSpec, service, portalRequest))
    );

    return functions;
  }

  private String directiveName(String functionName) {
    if (functionName.indexOf(".") > 0) {
      String[] names = functionName.split("\\.");
      return names[names.length - 1];
    }
    return functionName;
  }
}
