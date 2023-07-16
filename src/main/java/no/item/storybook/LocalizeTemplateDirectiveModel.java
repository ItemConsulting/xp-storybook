package no.item.storybook;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public class LocalizeTemplateDirectiveModel implements TemplateDirectiveModel {
  private final String baseDir;

  public LocalizeTemplateDirectiveModel(String baseDir) {
    this.baseDir = baseDir;
  }

  @Override
  public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body) throws TemplateException, IOException {
    try (Writer out = env.getOut()) {
      ResourceBundle bundle = getResourceBundle(params);
      String key = params.get("key").toString();

      if (bundle.keySet().contains(key)) {
        out.append(bundle.getString(key));
      } else {
        out.append("NOT_TRANSLATED");
      }
    }
  }

  private ResourceBundle getResourceBundle(Map params) throws MalformedURLException {
    Locale locale = params.containsKey("locale") ? Locale.forLanguageTag(params.get("locale").toString()) : Locale.ROOT;
    File file = new File(baseDir + File.separator + "i18n");
    URL[] urls = {file.toURI().toURL()};
    ClassLoader loader = new URLClassLoader(urls);

    return ResourceBundle.getBundle("phrases", locale, loader);
  }
}
