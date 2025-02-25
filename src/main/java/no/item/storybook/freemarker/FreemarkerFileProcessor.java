package no.item.storybook.freemarker;

import com.enonic.xp.script.ScriptValue;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.*;
import no.api.freemarker.java8.Java8ObjectWrapper;
import no.tine.xp.lib.freemarker.ComponentDirective;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class FreemarkerFileProcessor {
  private final static Logger log = LoggerFactory.getLogger(FreemarkerFileProcessor.class);
  private static final Configuration CONFIGURATION = new Configuration(Configuration.VERSION_2_3_31);
  private final Map<String, TemplateDirectiveModel> viewFunctions;
  private List<String> baseDirPaths;
  private String filePath;
  private ScriptValue model;

  public FreemarkerFileProcessor(Map<String, TemplateDirectiveModel> viewFunctions) {
    this.viewFunctions = viewFunctions;
  }

  public static void setupFreemarker() {
    CONFIGURATION.setLogTemplateExceptions(false);
    CONFIGURATION.setTagSyntax(Configuration.AUTO_DETECT_TAG_SYNTAX);
    CONFIGURATION.setDefaultEncoding("UTF-8");

    CONFIGURATION.setSharedVariable("component", new ComponentDirective());

    // Remove lookup for localized files (template.ftl => template_en_EN.ftl, template_en.ftl, template.ftl)
    // Should improve performance in dev mode, where some machines have slow file lookup
    CONFIGURATION.setLocalizedLookup(false);

    //CONFIGURATION.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);    // Throws exceptions to log file
    CONFIGURATION.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);      // Shows exceptions on screen
    CONFIGURATION.setObjectWrapper(new Java8ObjectWrapper(Configuration.VERSION_2_3_31));
  }

  public void setFilePath(final String filePath) {
    this.filePath = filePath;
  }

  public void setBaseDirPaths(final List<String> baseDirPaths) {
    this.baseDirPaths = baseDirPaths;
  }

  public void setModel(final ScriptValue model) {
    this.model = model;
  }

  public String process() throws Throwable {
    try {
      return doProcess();
    } catch (final Exception e) {
      throw handleError(e);
    }
  }

  private String doProcess() throws IOException, TemplateException {
    var baseDirPathWithExistingFile = this.baseDirPaths.stream()
      .filter(baseDir -> new File(baseDir + File.separator + filePath).exists())
      .findFirst()
      .orElse(null);

    final Map<String, Object> map = model != null ? model.getMap() : Maps.newHashMap();
    map.putAll(viewFunctions);
    map.put("localize", new LocalizeTemplateDirectiveModel(baseDirPathWithExistingFile));
    map.put("assetUrl", new AssetUrlTemplateDirectiveModel());

    TemplateLoader[] loaders = this.baseDirPaths.stream()
      .map(baseDir -> {
        try {
          return new FileTemplateLoader(new File(baseDir));
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      })
      .toArray(TemplateLoader[]::new);

    MultiTemplateLoader multiTemplateLoader = new MultiTemplateLoader(loaders);

    CONFIGURATION.setTemplateLoader(multiTemplateLoader);

    StringWriter sw = new StringWriter();

    for(int i=0; i < this.baseDirPaths.size(); i++) {
      try {
        String baseDirPath = this.baseDirPaths.get(i);
        Template template = new Template(this.filePath, new FileReader(baseDirPath + File.separator + filePath), CONFIGURATION);
        template.process(map, sw);
        break;
      } catch (IOException e) {
        // If file was not found in last directory, throw the exception
        if (i == this.baseDirPaths.size() - 1) {
          throw e;
        } else {
          log.warn(e.getMessage());
        }
      }
    }

    return sw.toString();
  }

  private RuntimeException handleError(final Exception e) throws Throwable {

    Optional<Throwable> templateProcessingException = Streams.findLast(
      Throwables.getCausalChain(e)
        .stream()
        .filter(((throwable) -> throwable instanceof freemarker.template.TemplateException))
    );

    throw templateProcessingException.orElse(e);
  }
}
