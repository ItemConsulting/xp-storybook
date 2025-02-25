package no.item.storybook.freemarker;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  private final static Logger log = LoggerFactory.getLogger(LocalizeTemplateDirectiveModel.class);
  private final String baseDirPath;

  public LocalizeTemplateDirectiveModel(String baseDirPath) {
    this.baseDirPath = baseDirPath;
  }

  @Override
  public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body) throws IOException {
    try (Writer out = env.getOut()) {
      if(baseDirPath == null) {
        out.append("NOT_TRANSLATED");
        return;
      }

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

    File dir = new File(baseDirPath + File.separator + "i18n");

    try {
      URL url = dir.toURI().toURL();
      ClassLoader loader = new URLClassLoader(new URL[] {url});
      return ResourceBundle.getBundle("phrases", locale, loader);
    } catch (MalformedURLException e) {
      log.error("Could not load resource bundle", e);
      return ResourceBundle.getBundle("phrases", locale);
    }
  }
}
