package no.item.storybook.freemarker;

import com.enonic.xp.app.ApplicationKey;
import no.item.storybook.FakeResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ResourceTemplateLoaderTest {
  private static final ApplicationKey APP = ApplicationKey.from("com.example.app");
  private static final ApplicationKey OTHER_APP = ApplicationKey.from("com.example.other");

  private FakeResourceService resources;
  private ResourceTemplateLoader loader;

  @BeforeEach
  void setUp() {
    this.resources = new FakeResourceService();
    this.loader = new ResourceTemplateLoader(() -> this.resources, APP);
  }

  @Test
  void finds_a_template_in_the_application() {
    this.resources.put(APP, "/parts/card.ftlh", "<p>hello</p>");

    assertNotNull(this.loader.findTemplateSource("/parts/card.ftlh"));
  }

  @Test
  void accepts_a_name_without_a_leading_slash() {
    this.resources.put(APP, "/parts/card.ftlh", "<p>hello</p>");

    assertNotNull(this.loader.findTemplateSource("parts/card.ftlh"));
  }

  @Test
  void returns_null_for_a_template_the_application_does_not_have() {
    assertNull(this.loader.findTemplateSource("/parts/absent.ftlh"));
  }

  /**
   * The loader used to hand the path to ResourceService.findFiles, whose argument is a regular expression. A path
   * like this one failed to compile as a pattern.
   */
  @Test
  void finds_a_template_whose_path_contains_regex_metacharacters() {
    this.resources.put(APP, "/parts/a+b(c)/card.ftlh", "<p>metacharacters</p>");

    assertNotNull(this.loader.findTemplateSource("/parts/a+b(c)/card.ftlh"));
  }

  /**
   * "." matches any character in a regular expression, so a findFiles-based lookup could answer with a different
   * file whose name merely resembled the one asked for.
   */
  @Test
  void does_not_match_a_template_whose_name_merely_resembles_the_one_asked_for() {
    this.resources.put(APP, "/parts/cardXftlh", "<p>wrong file</p>");

    assertNull(this.loader.findTemplateSource("/parts/card.ftlh"));
  }

  @Test
  void never_reaches_another_application() {
    this.resources.put(OTHER_APP, "/parts/card.ftlh", "<p>another application</p>");

    assertNull(this.loader.findTemplateSource("/parts/card.ftlh"));
  }

  /**
   * An inline template is caller-supplied and can include whatever it likes, so the loader has to refuse anything
   * that is not a template. In development mode the application's resources are its source directory on disk.
   */
  @Test
  void refuses_a_properties_file() {
    this.resources.put(APP, "/i18n/phrases.properties", "secret=TOPSECRET");

    assertNull(this.loader.findTemplateSource("/i18n/phrases.properties"));
  }

  @Test
  void refuses_a_typescript_source() {
    this.resources.put(APP, "/lib/creds.ts", "const TOKEN = \"TOPSECRET\";");

    assertNull(this.loader.findTemplateSource("/lib/creds.ts"));
  }

  @Test
  void refuses_a_file_with_no_extension() {
    this.resources.put(APP, "/Dockerfile", "FROM scratch");

    assertNull(this.loader.findTemplateSource("/Dockerfile"));
  }

  /** A FreeMarker template can legitimately include an HTML partial, so the check is by type, not by engine. */
  @Test
  void allows_a_fragment_type_a_deployed_application_could_include() {
    this.resources.put(APP, "/site/partials/header.html", "<header>hi</header>");

    assertNotNull(this.loader.findTemplateSource("/site/partials/header.html"));
  }

  @Test
  void reads_the_template_contents() throws IOException {
    this.resources.put(APP, "/parts/card.ftlh", "<p>hello</p>");

    final Object source = this.loader.findTemplateSource("/parts/card.ftlh");

    try (var reader = this.loader.getReader(source, "UTF-8")) {
      assertEquals("<p>hello</p>", new java.io.BufferedReader(reader).readLine());
    }
  }
}
