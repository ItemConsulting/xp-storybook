package no.item.storybook.thymeleaf;

import com.enonic.xp.script.ScriptValue;
import com.google.common.collect.Maps;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.TemplateSpec;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;

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
    try {
      final Context context = new Context();
      context.setVariables(this.parameters);

      // The template string goes where a template name normally would, so the resolver is told outright that this is
      // an inline template rather than being left to recognise one.
      final TemplateSpec spec = new TemplateSpec(
        this.template,
        null,
        this.mode,
        Map.of(StorybookTemplateResolver.INLINE_ATTRIBUTE, Boolean.TRUE)
      );

      return this.engine.process(spec, context);
    } catch (final RuntimeException e) {
      throw ThymeleafErrors.unwrap(e);
    }
  }
}
