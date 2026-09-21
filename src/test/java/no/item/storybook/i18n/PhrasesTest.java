package no.item.storybook.i18n;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhrasesTest {
  @TempDir
  Path baseDir;

  private void writePhrases(final Path dir, final String content) throws IOException {
    Files.createDirectories(dir);
    Files.writeString(dir.resolve("phrases.properties"), content);
  }

  @Test
  void returns_not_translated_when_there_is_no_i18n_directory() {
    assertEquals(Phrases.NOT_TRANSLATED,
      Phrases.localize(baseDir.toString(), Locale.ROOT, "greeting", List.of()));
  }

  /**
   * The reported case: an i18n directory that exists but holds no phrases.properties — which is
   * what a scaffolded project with only a .gitkeep looks like. This used to escape as
   * MissingResourceException and replace the rendered component with a stack trace.
   */
  @Test
  void returns_not_translated_when_the_i18n_directory_has_no_bundle() throws IOException {
    Files.createDirectories(baseDir.resolve("i18n"));

    assertEquals(Phrases.NOT_TRANSLATED,
      Phrases.localize(baseDir.toString(), Locale.forLanguageTag("en-US"), "greeting", List.of()));
  }

  @Test
  void returns_not_translated_for_a_null_base_directory() {
    assertEquals(Phrases.NOT_TRANSLATED, Phrases.localize(null, Locale.ROOT, "greeting", List.of()));
  }

  @Test
  void resolves_a_key_from_the_i18n_directory() throws IOException {
    writePhrases(baseDir.resolve("i18n"), "greeting=Hei");

    assertEquals("Hei", Phrases.localize(baseDir.toString(), Locale.ROOT, "greeting", List.of()));
  }

  @Test
  void returns_not_translated_for_a_key_the_bundle_does_not_have() throws IOException {
    writePhrases(baseDir.resolve("i18n"), "greeting=Hei");

    assertEquals(Phrases.NOT_TRANSLATED,
      Phrases.localize(baseDir.toString(), Locale.ROOT, "absent", List.of()));
  }

  @Test
  void substitutes_positional_values() throws IOException {
    writePhrases(baseDir.resolve("i18n"), "greeting=Hei {0} og {1}!");

    assertEquals("Hei Tom og Ada!",
      Phrases.localize(baseDir.toString(), Locale.ROOT, "greeting", List.of("Tom", "Ada")));
  }

  @Test
  void leaves_placeholders_alone_when_no_values_are_given() throws IOException {
    writePhrases(baseDir.resolve("i18n"), "greeting=Hei {0}");

    assertEquals("Hei {0}", Phrases.localize(baseDir.toString(), Locale.ROOT, "greeting", List.of()));
  }

  @Test
  void falls_back_to_the_site_i18n_directory() throws IOException {
    writePhrases(baseDir.resolve("site").resolve("i18n"), "greeting=Fra site-mappa");

    assertEquals("Fra site-mappa", Phrases.localize(baseDir.toString(), Locale.ROOT, "greeting", List.of()));
  }

  @Test
  void prefers_the_top_level_i18n_directory_over_the_site_one() throws IOException {
    writePhrases(baseDir.resolve("i18n"), "greeting=Toppnivaa");
    writePhrases(baseDir.resolve("site").resolve("i18n"), "greeting=Fra site-mappa");

    assertEquals("Toppnivaa", Phrases.localize(baseDir.toString(), Locale.ROOT, "greeting", List.of()));
  }

  @Test
  void picks_the_bundle_matching_the_locale() throws IOException {
    writePhrases(baseDir.resolve("i18n"), "greeting=Hello");
    Files.writeString(baseDir.resolve("i18n").resolve("phrases_no.properties"), "greeting=Hei");

    assertEquals("Hei",
      Phrases.localize(baseDir.toString(), Locale.forLanguageTag("no"), "greeting", List.of()));
  }

  @Test
  void find_returns_empty_when_there_is_no_bundle() {
    assertTrue(Phrases.find(baseDir.toString(), Locale.ROOT).isEmpty());
  }
}
