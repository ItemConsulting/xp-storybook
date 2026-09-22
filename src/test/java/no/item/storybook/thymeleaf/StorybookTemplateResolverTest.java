package no.item.storybook.thymeleaf;

import com.enonic.xp.app.ApplicationKey;
import no.item.storybook.FakeResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.TemplateSpec;
import org.thymeleaf.context.Context;
import org.thymeleaf.dialect.IDialect;
import org.thymeleaf.exceptions.TemplateInputException;
import org.thymeleaf.standard.StandardDialect;
import org.thymeleaf.templatemode.TemplateMode;

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
  private static final ApplicationKey APP = ApplicationKey.from("com.example.app");
  private static final ApplicationKey OTHER_APP = ApplicationKey.from("com.example.other");

  private FakeResourceService resources;

  @BeforeEach
  void setUp() {
    this.resources = new FakeResourceService();
  }

  private TemplateEngine engine() {
    return engineFor(APP);
  }

  private TemplateEngine engineFor(final ApplicationKey applicationKey) {
    final TemplateEngine engine = new TemplateEngine();

    final Set<IDialect> dialects = Set.of(new ExtensionDialectImpl(), new StandardDialect());
    engine.setDialects(dialects);

    final StorybookTemplateResolver resolver = new StorybookTemplateResolver(applicationKey, () -> this.resources);
    resolver.setSuffix(".html");
    engine.setTemplateResolver(resolver);

    return engine;
  }

  private void write(final String path, final String content) {
    this.resources.put(APP, path, content);
  }

  private String render(final TemplateEngine engine, final String filePath) {
    return engine.process(new TemplateSpec(filePath, (TemplateMode) null), new Context());
  }

  private TemplateSpec inline(final String template) {
    return new TemplateSpec(template, null, (TemplateMode) null, Map.of(StorybookTemplateResolver.INLINE_ATTRIBUTE, Boolean.TRUE));
  }

  @Test
  void resolves_a_template_from_the_application() {
    write("/parts/card.html", "<p>from the application</p>");

    assertEquals("<p>from the application</p>", render(engine(), "/parts/card.html"));
  }

  @Test
  void resolves_a_relative_include_against_the_including_template() {
    write("/parts/card.html", "<div th:replace=\"~{./body :: body}\"></div>");
    write("/parts/body.html", "<span th:fragment=\"body\">included</span>");

    assertEquals("<span>included</span>", render(engine(), "/parts/card.html"));
  }

  @Test
  void resolves_a_root_relative_include_from_the_application_root() {
    write("/parts/card.html", "<div th:replace=\"~{fragments/body :: body}\"></div>");
    write("/fragments/body.html", "<span th:fragment=\"body\">included</span>");

    assertEquals("<span>included</span>", render(engine(), "/parts/card.html"));
  }

  /**
   * Thymeleaf reports the owner by the name it was requested under ("./widgets/box"), not by where it was found, so
   * resolving against that name would lose the ancestor directory.
   */
  @Test
  void resolves_a_relative_include_nested_inside_another_one() {
    write("/parts/card.html", "<div th:replace=\"~{./widgets/box :: box}\"></div>");
    write("/parts/widgets/box.html", "<div th:fragment=\"box\" th:replace=\"~{./inner :: inner}\"></div>");
    write("/parts/widgets/inner.html", "<span th:fragment=\"inner\">innermost</span>");

    assertEquals("<span>innermost</span>", render(engine(), "/parts/card.html"));
  }

  /**
   * An inline template has no resource of its own, and Thymeleaf reports it as owner by its entire source text, so
   * "./x" has to mean the same as "x" there.
   */
  @Test
  void resolves_a_relative_include_from_an_inline_template() {
    write("/fragments/body.html", "<span th:fragment=\"body\">included</span>");

    assertEquals(
      "<span>included</span>",
      engine().process(inline("<div th:replace=\"~{./fragments/body :: body}\"></div>"), new Context()));
  }

  @Test
  void resolves_an_include_from_an_inline_template() {
    write("/fragments/body.html", "<span th:fragment=\"body\">included</span>");

    assertEquals(
      "<span>included</span>",
      engine().process(inline("<div th:replace=\"~{fragments/body :: body}\"></div>"), new Context()));
  }

  /**
   * An inline template used to be recognised by looking for angle brackets in it, which made a template without any
   * resolve as a resource path instead.
   */
  @Test
  void renders_an_inline_template_that_has_no_angle_brackets() {
    final Context context = new Context();
    context.setVariable("title", "Hi");

    assertEquals("Hi", engine().process(inline("[[${title}]]"), context));
  }

  @Test
  void includes_a_fragment_under_any_html_extension() {
    write("/fragments/body.htm", "<span th:fragment=\"body\">included</span>");
    write("/parts/card.html", "<div th:replace=\"~{fragments/body.htm :: body}\"></div>");

    assertEquals("<span>included</span>", render(engine(), "/parts/card.html"));
  }

  /**
   * The marker that lib/storybook/regions.ts replaces with the rendered child component. It has to come out byte for
   * byte the same as the one FreeMarker's component directive writes.
   */
  @Test
  void writes_the_component_placeholder_the_region_renderer_looks_for() {
    write("/pages/page.html", "<div portal:component=\"${path}\">placeholder</div>");

    final Context context = new Context();
    context.setVariable("path", "/main/0");

    final String rendered = engine().process(new TemplateSpec("/pages/page.html", (TemplateMode) null), context);

    assertTrue(rendered.contains("<!--# COMPONENT /main/0 -->"), rendered);
  }

  /**
   * ResourceKey normalises its path with Files.simplifyPath, which clamps a leading "..", so an include cannot climb
   * out of the application it belongs to.
   */
  @Test
  void refuses_to_leave_the_application() {
    this.resources.put(APP, "/outside-secret.html", "<span th:fragment=\"content\">secret</span>");
    write("/parts/widgets/card.html", "<div th:replace=\"~{./../../../../outside-secret :: content}\"></div>");

    // The climb is clamped at the application root, so it lands back inside the application rather than outside it.
    assertEquals("<span>secret</span>", render(engine(), "/parts/widgets/card.html"));
  }

  @Test
  void never_reaches_another_application() {
    this.resources.put(OTHER_APP, "/parts/card.html", "<p>another application</p>");

    final TemplateInputException e =
      assertThrows(TemplateInputException.class, () -> render(engine(), "/parts/card.html"));

    assertTrue(!rootMessage(e).contains("another application"), "must not read another application's resources");
  }

  @Test
  void names_the_application_when_a_template_is_missing() {
    final TemplateInputException e =
      assertThrows(TemplateInputException.class, () -> render(engine(), "/parts/absent.html"));

    assertTrue(rootMessage(e).contains("com.example.app"), rootMessage(e));
  }

  /**
   * A template path is a literal. It used to be handed to ResourceService.findFiles, whose argument is a regular
   * expression, so a path like this failed to compile as one.
   */
  @Test
  void resolves_a_template_whose_path_contains_regex_metacharacters() {
    write("/parts/a+b(c)/card.html", "<p>metacharacters</p>");

    assertEquals("<p>metacharacters</p>", render(engine(), "/parts/a+b(c)/card.html"));
  }

  /**
   * In development mode an application's resources are its source directory on disk, so they hold things that are
   * not meant to be served — TypeScript sources among them. An inline template is caller-supplied, so the extension
   * check is what keeps it from reading them.
   */
  @Test
  void refuses_to_include_a_typescript_source() {
    write("/parts/card.ts", "const SECRET = \"TOPSECRET\";");

    final TemplateInputException e = assertThrows(
      TemplateInputException.class,
      () -> engine().process(inline("<div th:replace=\"~{/parts/card.ts}\"></div>"), new Context()));

    assertTrue(!rootMessage(e).contains("TOPSECRET"), "the refused file must not be read");
  }

  @Test
  void refuses_to_include_a_properties_file() {
    write("/i18n/phrases.properties", "secret=TOPSECRET");

    final TemplateInputException e = assertThrows(
      TemplateInputException.class,
      () -> engine().process(inline("<div th:replace=\"~{/i18n/phrases.properties}\"></div>"), new Context()));

    assertTrue(!rootMessage(e).contains("TOPSECRET"), "the refused file must not be read");
  }

  /** A deployed application can include these, so the preview has to be able to as well. */
  @Test
  void includes_a_fragment_type_thymeleaf_recognises() {
    write("/fragments/icons.xml", "<span th:fragment=\"icon\">icon</span>");
    write("/parts/card.html", "<div th:replace=\"~{/fragments/icons.xml :: icon}\"></div>");

    assertEquals("<span>icon</span>", render(engine(), "/parts/card.html"));
  }

  private static String rootMessage(final Throwable e) {
    final StringBuilder sb = new StringBuilder();
    for (Throwable t = e; t != null; t = t.getCause()) {
      sb.append(t.getMessage()).append('\n');
    }
    return sb.toString();
  }
}
