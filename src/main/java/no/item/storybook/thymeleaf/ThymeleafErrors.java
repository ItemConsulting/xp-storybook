package no.item.storybook.thymeleaf;

import com.google.common.base.Throwables;
import com.google.common.collect.Streams;
import org.thymeleaf.exceptions.TemplateProcessingException;

import java.util.Optional;

final class ThymeleafErrors {
  private ThymeleafErrors() {
  }

  /**
   * Thymeleaf nests a {@link TemplateProcessingException} per template it was rendering, outermost first, and
   * {@code getCausalChain} lists them in that order. The innermost one names the template, line and column that
   * actually failed, which is what makes the reported error useful.
   */
  static RuntimeException unwrap(final RuntimeException e) {
    final Optional<Throwable> innermost = Streams.findLast(
      Throwables.getCausalChain(e)
        .stream()
        .filter(throwable -> throwable instanceof TemplateProcessingException)
    );

    return innermost.map(RuntimeException.class::cast).orElse(e);
  }
}
