package no.item.storybook.freemarker;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateModel;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public class LocalizeTemplateDirectiveModel implements TemplateDirectiveModel {
  private final List<String> baseDirPaths;

  public LocalizeTemplateDirectiveModel(List<String> baseDirPaths) {
    this.baseDirPaths = baseDirPaths;
  }

  @Override
  public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body) throws IOException {
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

  private ResourceBundle getResourceBundle(Map params) {
    Locale locale = params.containsKey("locale") ? Locale.forLanguageTag(params.get("locale").toString()) : Locale.ROOT;

    URL[] urls = baseDirPaths.stream()
      .map(baseDirPath -> new File(baseDirPath + File.separator + "i18n"))
      .map(file -> {
        try {
          return file.toURI().toURL();
        } catch (MalformedURLException e) {
          throw new RuntimeException(e);
        }
      }).toArray(URL[]::new);

    ClassLoader loader = new URLClassLoader(urls);

    return ResourceBundle.getBundle("phrases", locale, loader);
  }
}
