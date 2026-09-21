package no.item.storybook.render;

import freemarker.core.Environment;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The errors of every template rendered while serving one request, so the controller can respond with a 500 status
 * while still returning the markup that did render.
 *
 * <p>Thymeleaf throws on a failing template, so the Thymeleaf renderer records the message with {@link #add(String)}.
 * FreeMarker instead writes its errors into the output, which is why this doubles as a
 * {@link TemplateExceptionHandler}: {@code HTML_DEBUG_HANDLER} rethrows once it has written the error to the output,
 * and lib-xp-freemarker swallows that exception for that particular handler, so the controller is never told that
 * rendering failed. Catching the rethrow here keeps the rest of the template rendering, and {@link #hasErrors()}
 * reports the failure instead.
 */
public class TemplateErrors implements TemplateExceptionHandler {
  private final List<String> messages = Collections.synchronizedList(new ArrayList<>());

  /**
   * Record an error raised by a template engine that reports failures by throwing.
   *
   * @param message the error message to report alongside the rendered output.
   */
  public void add(final String message) {
    this.messages.add(message);
  }

  @Override
  public void handleTemplateException(TemplateException te, Environment env, Writer out) throws TemplateException {
    // Inside <#attempt> the exception belongs to the matching <#recover> block, so pass it on untouched.
    if (env.isInAttemptBlock()) {
      throw te;
    }

    // Read the message before delegating, as printing the stack trace replaces it with a "see it above" placeholder.
    add(te.getMessage());

    try {
      TemplateExceptionHandler.HTML_DEBUG_HANDLER.handleTemplateException(te, env, out);
    } catch (TemplateException e) {
      // Expected. The error is now part of the output, and is reported by hasErrors() instead.
    }
  }

  public boolean hasErrors() {
    return !this.messages.isEmpty();
  }

  /**
   * @return every collected error message, or {@code null} if nothing failed
   */
  public String getMessage() {
    synchronized (this.messages) {
      return this.messages.isEmpty() ? null : String.join("\n\n", this.messages);
    }
  }
}
