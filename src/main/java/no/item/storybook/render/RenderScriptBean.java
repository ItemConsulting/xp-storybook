package no.item.storybook.render;

import com.enonic.xp.script.bean.BeanContext;
import com.enonic.xp.script.bean.ScriptBean;

/**
 * Flavor-independent rendering helpers. Both the FreeMarker and the Thymeleaf renderer collect their errors here, so
 * the factory does not belong to either of them.
 */
public final class RenderScriptBean implements ScriptBean {
  /**
   * Creates a collector for the errors of all templates rendered while serving one request.
   */
  public TemplateErrors newTemplateErrors() {
    return new TemplateErrors();
  }

  @Override
  public void initialize(final BeanContext context) {
    // Nothing to bind: the collector needs no XP services.
  }
}
