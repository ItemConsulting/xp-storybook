package no.item.storybook.i18n;

import com.enonic.xp.app.ApplicationKey;
import no.item.storybook.FakeResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PhrasesTest {
  private static final ApplicationKey APP = ApplicationKey.from("com.example.app");

  private FakeResourceService resources;

  @BeforeEach
  void setUp() {
    this.resources = new FakeResourceService();
  }

  private String localize(final Locale locale, final String key, final List<String> values) {
    return Phrases.localize(this.resources, APP, locale, key, values, List.of());
  }

  @Test
  void resolves_a_key_from_the_default_bundle() {
    this.resources.put(APP, "/i18n/phrases.properties", "greeting=Hei");

    assertEquals("Hei", localize(Locale.ROOT, "greeting", List.of()));
  }

  @Test
  void answers_not_translated_when_the_application_has_no_bundle() {
    assertEquals(Phrases.NOT_TRANSLATED, localize(Locale.ROOT, "greeting", List.of()));
  }

  @Test
  void answers_not_translated_for_a_key_the_bundle_does_not_have() {
    this.resources.put(APP, "/i18n/phrases.properties", "greeting=Hei");

    assertEquals(Phrases.NOT_TRANSLATED, localize(Locale.ROOT, "absent", List.of()));
  }

  @Test
  void answers_not_translated_for_a_missing_key() {
    assertEquals(Phrases.NOT_TRANSLATED, localize(Locale.ROOT, null, List.of()));
  }

  @Test
  void substitutes_positional_values() {
    this.resources.put(APP, "/i18n/phrases.properties", "greeting=Hei {0} og {1}!");

    assertEquals("Hei Tom og Ada!", localize(Locale.ROOT, "greeting", List.of("Tom", "Ada")));
  }

  /** XP leaves the phrase untouched when there is nothing to substitute, so a placeholder survives verbatim. */
  @Test
  void leaves_placeholders_alone_when_no_values_are_given() {
    this.resources.put(APP, "/i18n/phrases.properties", "greeting=Hei {0}");

    assertEquals("Hei {0}", localize(Locale.ROOT, "greeting", List.of()));
  }

  @Test
  void picks_the_bundle_matching_the_locale() {
    this.resources.put(APP, "/i18n/phrases.properties", "greeting=Hello");
    this.resources.put(APP, "/i18n/phrases_no.properties", "greeting=Hei");

    assertEquals("Hei", localize(Locale.forLanguageTag("no"), "greeting", List.of()));
  }

  /** A key the specific bundle does not have falls back to the less specific one, as ResourceBundle would. */
  @Test
  void falls_back_to_the_less_specific_bundle_for_a_key_the_specific_one_lacks() {
    this.resources.put(APP, "/i18n/phrases.properties", "greeting=Hello\nfarewell=Bye");
    this.resources.put(APP, "/i18n/phrases_no.properties", "greeting=Hei");

    assertEquals("Bye", localize(Locale.forLanguageTag("no"), "farewell", List.of()));
  }

  @Test
  void prefers_the_country_specific_bundle() {
    this.resources.put(APP, "/i18n/phrases.properties", "greeting=Hello");
    this.resources.put(APP, "/i18n/phrases_no.properties", "greeting=Hei");
    this.resources.put(APP, "/i18n/phrases_no_NO.properties", "greeting=Hei fra Norge");

    assertEquals("Hei fra Norge", localize(Locale.forLanguageTag("no-NO"), "greeting", List.of()));
  }

  @Test
  void reads_an_explicitly_named_bundle() {
    this.resources.put(APP, "/i18n/other.properties", "greeting=Fra en annen bundle");

    assertEquals("Fra en annen bundle",
      Phrases.localize(this.resources, APP, Locale.ROOT, "greeting", List.of(), List.of("/i18n/other")));
  }

  @Test
  void accepts_a_bundle_name_without_a_leading_slash() {
    this.resources.put(APP, "/i18n/other.properties", "greeting=Fra en annen bundle");

    assertEquals("Fra en annen bundle",
      Phrases.localize(this.resources, APP, Locale.ROOT, "greeting", List.of(), List.of("i18n/other")));
  }

  /**
   * The reason this does not go through XP's LocaleService: a preview has to show what the file says now, and
   * LocaleService caches a bundle per application and locale.
   */
  @Test
  void reads_the_bundle_again_on_every_call() {
    this.resources.put(APP, "/i18n/phrases.properties", "greeting=Before");
    assertEquals("Before", localize(Locale.ROOT, "greeting", List.of()));

    this.resources.put(APP, "/i18n/phrases.properties", "greeting=After");
    assertEquals("After", localize(Locale.ROOT, "greeting", List.of()));
  }
}
