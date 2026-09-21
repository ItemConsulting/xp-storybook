package no.item.storybook.thymeleaf;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.TemplateSpec;
import org.thymeleaf.context.Context;
import org.thymeleaf.dialect.IDialect;
import org.thymeleaf.exceptions.TemplateInputException;
import org.thymeleaf.standard.StandardDialect;
import org.thymeleaf.templatemode.TemplateMode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the resolver through a real {@link TemplateEngine}, which is the only way to cover the suffix handling
 * and the resolution of one template from inside another.
 */
class StorybookTemplateResolverTest {
  @TempDir
  Path first;

  @TempDir
  Path second;

  private TemplateEngine engineFor(final Path... dirPaths) {
    final TemplateEngine engine = new TemplateEngine();

    final Set<IDialect> dialects = Set.of(new ExtensionDialectImpl(), new StandardDialect());
    engine.setDialects(dialects);

    final List<String> paths = List.of(dirPaths).stream().map(Path::toString).toList();
    // No application: these tests cover the disk lookup, and reaching XP's ResourceService needs a running server.
    final StorybookTemplateResolver resolver = new StorybookTemplateResolver(paths, null, () -> null);
    resolver.setSuffix(".html");
    engine.setTemplateResolver(resolver);

    return engine;
  }

  private void write(final Path dir, final String relativePath, final String content) throws IOException {
    final Path file = dir.resolve(relativePath);
    Files.createDirectories(file.getParent());
    Files.writeString(file, content);
  }

  private String render(final TemplateEngine engine, final String filePath) {
    return engine.process(new TemplateSpec(filePath, (TemplateMode) null), new Context());
  }

  /**
   * The reason xpResourcesDirPath takes a comma separated list: a project renders templates that live next to the
   * ones in a shared library. Only the FreeMarker flavor used to honour it.
   */
  @Test
  void resolves_a_template_from_the_second_directory() throws IOException {
    write(second, "parts/card.html", "<p>from the second directory</p>");

    assertEquals("<p>from the second directory</p>", render(engineFor(first, second), "/parts/card.html"));
  }

  @Test
  void prefers_the_first_directory_when_both_hold_the_template() throws IOException {
    write(first, "parts/card.html", "<p>first</p>");
    write(second, "parts/card.html", "<p>second</p>");

    assertEquals("<p>first</p>", render(engineFor(first, second), "/parts/card.html"));
  }

  @Test
  void resolves_a_relative_include_against_the_including_template() throws IOException {
    write(first, "parts/card.html", "<div th:replace=\"~{./body :: body}\"></div>");
    write(first, "parts/body.html", "<span th:fragment=\"body\">included</span>");

    assertEquals("<span>included</span>", render(engineFor(first, second), "/parts/card.html"));
  }

  @Test
  void resolves_a_root_relative_include_from_any_directory() throws IOException {
    write(first, "parts/card.html", "<div th:replace=\"~{fragments/body :: body}\"></div>");
    write(second, "fragments/body.html", "<span th:fragment=\"body\">included</span>");

    assertEquals("<span>included</span>", render(engineFor(first, second), "/parts/card.html"));
  }

  /**
   * The endpoint only checks the extension of the template it was asked for, not of the templates that one includes.
   * FreeMarker's FileTemplateLoader refuses to leave its base directory; this is the same guarantee.
   */
  @Test
  void refuses_an_include_that_escapes_the_resource_directory() throws IOException {
    // A perfectly renderable template, so that only the containment check stands between it and the output.
    final Path outside = first.getParent().resolve("outside-secret.html");
    Files.writeString(outside, "<span th:fragment=\"content\">secret</span>");

    write(first, "parts/card.html", "<div th:replace=\"~{./../../outside-secret :: content}\"></div>");

    final TemplateInputException e =
      assertThrows(TemplateInputException.class, () -> render(engineFor(first), "/parts/card.html"));

    assertTrue(rootMessage(e).contains("outside-secret"), rootMessage(e));
  }

  @Test
  void reports_the_directories_it_searched_when_a_template_is_missing() {
    final TemplateInputException e =
      assertThrows(TemplateInputException.class, () -> render(engineFor(first, second), "/parts/absent.html"));

    final String message = rootMessage(e);
    assertTrue(message.contains(first.toString()), message);
    assertTrue(message.contains(second.toString()), message);
  }

  /**
   * An inline template used to be recognised by looking for angle brackets in it, which made a template without any
   * resolve as a file path instead.
   */
  @Test
  void renders_an_inline_template_that_has_no_angle_brackets() {
    final Context context = new Context();
    context.setVariable("title", "Hi");

    final TemplateSpec spec = new TemplateSpec(
      "[[${title}]]",
      null,
      (TemplateMode) null,
      Map.of(StorybookTemplateResolver.INLINE_ATTRIBUTE, Boolean.TRUE)
    );

    assertEquals("Hi", engineFor(first).process(spec, context));
  }

  @Test
  void resolves_an_include_from_an_inline_template() throws IOException {
    write(first, "fragments/body.html", "<span th:fragment=\"body\">included</span>");

    final TemplateSpec spec = new TemplateSpec(
      "<div th:replace=\"~{fragments/body :: body}\"></div>",
      null,
      (TemplateMode) null,
      Map.of(StorybookTemplateResolver.INLINE_ATTRIBUTE, Boolean.TRUE)
    );

    assertEquals("<span>included</span>", engineFor(first).process(spec, new Context()));
  }

  /**
   * The marker that lib/storybook/regions.ts replaces with the rendered child component. It has to come out byte for
   * byte the same as the one FreeMarker's component directive writes.
   */
  @Test
  void writes_the_component_placeholder_the_region_renderer_looks_for() throws IOException {
    write(first, "pages/page.html", "<div portal:component=\"${path}\">placeholder</div>");

    final Context context = new Context();
    context.setVariable("path", "/main/0");

    final String rendered = engineFor(first).process(new TemplateSpec("/pages/page.html", (TemplateMode) null), context);

    assertTrue(rendered.contains("<!--# COMPONENT /main/0 -->"), rendered);
  }

  /**
   * A relative include inside an already relatively included template. Thymeleaf reports the owner by the name it
   * was requested under ("./widgets/box"), not by where it was found, so resolving against that name loses the
   * ancestor directory and looks for <dir>/widgets/inner.html.
   */
  @Test
  void resolves_a_relative_include_nested_inside_another_one() throws IOException {
    write(first, "parts/card.html", "<div th:replace=\"~{./widgets/box :: box}\"></div>");
    write(first, "parts/widgets/box.html", "<div th:fragment=\"box\" th:replace=\"~{./inner :: inner}\"></div>");
    write(first, "parts/widgets/inner.html", "<span th:fragment=\"inner\">innermost</span>");

    assertEquals("<span>innermost</span>", render(engineFor(first, second), "/parts/card.html"));
  }

  /**
   * An inline template has no directory of its own, and Thymeleaf reports it as owner by its entire source text, so
   * "./x" has to mean the same as "x" there rather than being resolved against that text.
   */
  @Test
  void resolves_a_relative_include_from_an_inline_template() throws IOException {
    write(first, "fragments/body.html", "<span th:fragment=\"body\">included</span>");

    final TemplateSpec spec = new TemplateSpec(
      "<div th:replace=\"~{./fragments/body :: body}\"></div>",
      null,
      (TemplateMode) null,
      Map.of(StorybookTemplateResolver.INLINE_ATTRIBUTE, Boolean.TRUE)
    );

    assertEquals("<span>included</span>", engineFor(first).process(spec, new Context()));
  }

  /**
   * The endpoint applies its extension allow-list to the template it was asked for, not to the ones that template
   * includes. Without the same check here, any readable file under a resource directory could be read back.
   */
  @Test
  void refuses_to_include_a_file_that_is_not_a_template() throws IOException {
    write(first, "secrets.txt", "TOPSECRET");
    write(first, "parts/card.html", "<div th:replace=\"~{/secrets.txt}\"></div>");

    final TemplateInputException e =
      assertThrows(TemplateInputException.class, () -> render(engineFor(first), "/parts/card.html"));

    assertTrue(!rootMessage(e).contains("TOPSECRET"), "the refused file must not be read");
  }

  @Test
  void refuses_to_include_a_non_template_through_a_relative_path() throws IOException {
    write(first, "parts/secrets.txt", "TOPSECRET");
    write(first, "parts/card.html", "<div th:replace=\"~{./secrets.txt}\"></div>");

    final TemplateInputException e =
      assertThrows(TemplateInputException.class, () -> render(engineFor(first), "/parts/card.html"));

    assertTrue(!rootMessage(e).contains("TOPSECRET"), "the refused file must not be read");
  }

  /**
   * Thymeleaf's other recognised extensions stay includable — only its HTML family is, and a name it does not
   * recognise at all gets ".html" appended before it ever reaches the resolver.
   */
  @Test
  void includes_a_fragment_under_any_html_extension() throws IOException {
    write(first, "fragments/body.htm", "<span th:fragment=\"body\">included</span>");
    write(first, "parts/card.html", "<div th:replace=\"~{fragments/body.htm :: body}\"></div>");

    assertEquals("<span>included</span>", render(engineFor(first), "/parts/card.html"));
  }

  @Test
  void refuses_to_include_a_stylesheet() throws IOException {
    write(first, "assets/app.css", "body { color: red }");
    write(first, "parts/card.html", "<div th:replace=\"~{/assets/app.css}\"></div>");

    final TemplateInputException e =
      assertThrows(TemplateInputException.class, () -> render(engineFor(first), "/parts/card.html"));

    assertTrue(!rootMessage(e).contains("color: red"), "the refused file must not be read");
  }

  private static String rootMessage(final Throwable e) {
    final StringBuilder sb = new StringBuilder();
    for (Throwable t = e; t != null; t = t.getCause()) {
      sb.append(t.getMessage()).append('\n');
    }
    return sb.toString();
  }
}
