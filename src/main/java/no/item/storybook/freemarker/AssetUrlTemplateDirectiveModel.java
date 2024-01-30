package no.item.storybook.freemarker;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateModel;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

public class AssetUrlTemplateDirectiveModel  implements TemplateDirectiveModel {
  @Override
  public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body) throws IOException {
    try (Writer out = env.getOut()) {
      out.append(String.valueOf(params.get("path")));
    }
  }
}
