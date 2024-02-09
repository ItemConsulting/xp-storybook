package no.item.storybook.freemarker;

import com.enonic.xp.resource.ResourceKey;
import com.enonic.xp.resource.ResourceProblemException;
import com.enonic.xp.script.ScriptValue;
import com.google.common.collect.Maps;
import freemarker.template.*;
import no.api.freemarker.java8.Java8ObjectWrapper;
import no.tine.xp.lib.freemarker.ComponentDirective;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

public final class FreemarkerInlineTemplateProcessor {
  private final static Logger log = LoggerFactory.getLogger(FreemarkerInlineTemplateProcessor.class);
  private static final Configuration CONFIGURATION = new Configuration(Configuration.VERSION_2_3_31);
  private final Map<String, TemplateDirectiveModel> viewFunctions;
  private String baseDirPath;
  private ScriptValue model;
  private String textTemplate;

  public FreemarkerInlineTemplateProcessor(Map<String, TemplateDirectiveModel> viewFunctions) {
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

  public void setTemplate(final String template) {
    this.textTemplate = template;
  }

  public void setBaseDirPath(final String baseDirPath) {
    this.baseDirPath = baseDirPath;
  }

  public void setModel(final ScriptValue model) {
    this.model = model;
  }

  public String process() {
    try {
      return doProcess();
    } catch (final TemplateException e) {
      throw handleError(e);
    } catch (final IOException e) {
      throw handleError(e);
    } catch (final RuntimeException e) {
      throw handleError(e);
    }
  }

  private String doProcess() throws IOException, TemplateException {
    final Map<String, Object> map = this.model != null ? this.model.getMap() : Maps.newHashMap();
    map.putAll(this.viewFunctions);
    map.put("localize", new LocalizeTemplateDirectiveModel(baseDirPath));

    if (baseDirPath != null) {
      CONFIGURATION.setDirectoryForTemplateLoading(new File(baseDirPath));
    }

    Template template = new Template(null, this.textTemplate, CONFIGURATION);

    StringWriter sw = new StringWriter();
    template.process(map, sw);

    return sw.toString();
  }

  private RuntimeException handleError(final TemplateException e) {
    // TODO This probably doesn't work
    final ResourceKey resource = e.getTemplateSourceName() != null ? ResourceKey.from(e.getTemplateSourceName()) : null;

    return ResourceProblemException.create()
      .lineNumber(e.getLineNumber())
      .resource(resource)
      .cause(e)
      .message(e.getMessageWithoutStackTop())
      .build();
  }

  private RuntimeException handleError(final IOException e) {
    String error = "IO with the script. " + e.getMessage();
    log.error(error, e);

    return new RuntimeException(error, e);
  }

  private RuntimeException handleError(final RuntimeException e) {
    return e;
  }
}
