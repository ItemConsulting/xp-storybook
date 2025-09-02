package no.item.storybook.freemarker.directive;

import com.enonic.xp.portal.PortalRequestAccessor;
import com.enonic.xp.portal.view.ViewFunctionParams;
import com.enonic.xp.portal.view.ViewFunctionService;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateModel;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class LegacyDirectiveModel implements TemplateDirectiveModel {
  private final ViewFunctionService viewFunctionService;
  private final String name;
  private final List<String> allowedParams;

  public LegacyDirectiveModel(ViewFunctionService viewFunctionService, String name, String... allowedParams) {
    this.name = name;
    this.allowedParams = Arrays.asList(allowedParams);
    this.viewFunctionService = viewFunctionService;
  }

  @SuppressWarnings("unchecked")
  public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body) throws IOException {
    final ViewFunctionParams viewFunctionParams = new ViewFunctionParams()
      .name(name)
      .args(toMultimap(params))
      .portalRequest(PortalRequestAccessor.get());

    Writer out = env.getOut();
    out.append(this.viewFunctionService.execute(viewFunctionParams).toString());
    out.close();
  }

  private Multimap<String, String> toMultimap(Map<String, Object> params) {
    Multimap<String, String> args = HashMultimap.create();

    params.keySet().stream()
      .filter(allowedParams::contains)
      .forEach(key -> args.put("_" + key, params.get(key).toString()));

    return args;
  }
}
