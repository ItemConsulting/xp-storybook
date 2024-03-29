package no.item.storybook.freemarker;

import com.enonic.xp.resource.ResourceProblemException;
import com.enonic.xp.script.ScriptValue;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import freemarker.template.*;
import no.api.freemarker.java8.Java8ObjectWrapper;
import no.tine.xp.lib.freemarker.ComponentDirective;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.Optional;

public final class FreemarkerFileProcessor {
  private final static Logger log = LoggerFactory.getLogger(FreemarkerFileProcessor.class);
  private static final Configuration CONFIGURATION = new Configuration(Configuration.VERSION_2_3_31);
  private final Map<String, TemplateDirectiveModel> viewFunctions;
  private String baseDirPath;
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

  public void setBaseDirPath(final String baseDirPath) {
    this.baseDirPath = baseDirPath;
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
    final Map<String, Object> map = model != null ? model.getMap() : Maps.newHashMap();
    map.putAll(viewFunctions);
    map.put("localize", new LocalizeTemplateDirectiveModel(baseDirPath));
    map.put("assetUrl", new AssetUrlTemplateDirectiveModel());

    CONFIGURATION.setDirectoryForTemplateLoading(new File(baseDirPath));

    String name = filePath.replace(baseDirPath, "");
    Template template = new Template(name, new FileReader(filePath), CONFIGURATION);

    StringWriter sw = new StringWriter();
    template.process(map, sw);

    return sw.toString();
  }

  private RuntimeException handleError(final Exception e) throws Throwable {

    Optional<Throwable> templateProcessingException = Streams.findLast(
      Throwables.getCausalChain(e)
        .stream()
        .filter(((throwable) -> throwable instanceof freemarker.template.TemplateException))
    );

    System.out.println("--------------------" + e.getClass().getName() + " " + e.getMessage());

    throw templateProcessingException.orElse(e);
  }
}
