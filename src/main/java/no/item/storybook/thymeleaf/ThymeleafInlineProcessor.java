package no.item.storybook.thymeleaf;

import com.enonic.xp.resource.ResourceProblemException;
import com.enonic.xp.script.ScriptValue;
import com.google.common.collect.Maps;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.TemplateSpec;
import org.thymeleaf.context.Context;
import org.thymeleaf.exceptions.TemplateProcessingException;
import org.thymeleaf.templatemode.TemplateMode;

import java.util.HashMap;
import java.util.Map;

public final class ThymeleafInlineProcessor {
  private final TemplateEngine engine;
  private final Map<String, Object> parameters;
  private String template;
  private TemplateMode mode;

  public ThymeleafInlineProcessor(final TemplateEngine engine, final ThymeleafViewFunctions viewFunctions) {
    this.engine = engine;
    this.parameters = Maps.newHashMap();

    this.parameters.put("portal", viewFunctions);
  }

  public void setTemplate(final String template) {
    this.template = template;
  }

  public void setModel(final ScriptValue model) {
    if (model != null) {
      this.parameters.putAll(model.getMap());
    }
  }

  public void setMode(final String mode) {
    try {
      this.mode = TemplateMode.valueOf(mode.toUpperCase());
    } catch (final Exception e) {
      this.mode = TemplateMode.HTML;
    }
  }

  public String process() {
    return doProcess();
  }

  private String doProcess() {
    try {
      final Context context = new Context();
      context.setVariables(this.parameters);

      Map<String, Object> resolutionAttributes = new HashMap<>() {{
        put("sbType", "inline");
      }};

      final TemplateSpec spec = new TemplateSpec(template, null, this.mode, resolutionAttributes);
      return this.engine.process(spec, context);
    } catch (final RuntimeException e) {
      throw handleException(e);
    }
  }

  private RuntimeException handleException(final RuntimeException e) {
    if (e instanceof TemplateProcessingException) {
      return handleException((TemplateProcessingException) e);
    }

    return e;
  }

  private RuntimeException handleException(final TemplateProcessingException e) {
    final int lineNumber = e.getLine() != null ? e.getLine() : 0;
    String message = e.getMessage();

    return ResourceProblemException.create().
      lineNumber(lineNumber).
      cause(e).
      message(message).
      build();
  }
}