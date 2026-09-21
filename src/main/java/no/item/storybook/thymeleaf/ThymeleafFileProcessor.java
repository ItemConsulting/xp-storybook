package no.item.storybook.thymeleaf;

import com.enonic.xp.script.ScriptValue;
import com.google.common.collect.Maps;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.TemplateSpec;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;

import java.util.Map;

public final class ThymeleafFileProcessor {
  private final TemplateEngine engine;
  private final Map<String, Object> parameters;
  private String filePath;
  private TemplateMode mode;

  public ThymeleafFileProcessor(final TemplateEngine engine, final ThymeleafViewFunctions viewFunctions) {
    this.engine = engine;
    this.parameters = Maps.newHashMap();

    this.parameters.put("portal", viewFunctions);
  }

  public void setFilePath(final String filePath) {
    this.filePath = filePath;
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

      final TemplateSpec spec = new TemplateSpec(this.filePath, this.mode);
      return this.engine.process(spec, context);
    } catch (final RuntimeException e) {
      throw ThymeleafErrors.unwrap(e);
    }
  }
}
