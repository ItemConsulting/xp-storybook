package no.item.storybook.freemarker;

import freemarker.core.Environment;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A {@link TemplateExceptionHandler} that writes errors into the output the same way
 * {@link TemplateExceptionHandler#HTML_DEBUG_HANDLER} does, while also keeping a record of them so the controller
 * can respond with a 500 status.
 *
 * <p>{@code HTML_DEBUG_HANDLER} rethrows once it has written the error to the output, and lib-xp-freemarker swallows
 * that exception for that particular handler. The controller is therefore never told that rendering failed. Catching
 * the rethrow here keeps the rest of the template rendering, and {@link #hasErrors()} reports the failure instead.
 */
public class TemplateErrorCollector implements TemplateExceptionHandler {
  private final List<String> messages = Collections.synchronizedList(new ArrayList<>());

  @Override
  public void handleTemplateException(TemplateException te, Environment env, Writer out) throws TemplateException {
    // Inside <#attempt> the exception belongs to the matching <#recover> block, so pass it on untouched.
    if (env.isInAttemptBlock()) {
      throw te;
    }

    // Read the message before delegating, as printing the stack trace replaces it with a "see it above" placeholder.
    this.messages.add(te.getMessage());

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
