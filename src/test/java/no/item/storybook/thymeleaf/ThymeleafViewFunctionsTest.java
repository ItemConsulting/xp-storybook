package no.item.storybook.thymeleaf;

import com.enonic.lib.thymeleaf.view.ViewFunctionParams;
import com.enonic.lib.thymeleaf.view.ViewFunctionService;
import com.enonic.xp.app.ApplicationKey;
import no.item.storybook.FakeResourceService;
import no.item.storybook.i18n.Phrases;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ThymeleafViewFunctionsTest {
  private static final ApplicationKey APP = ApplicationKey.from("com.example.app");
  private static final ApplicationKey OTHER_APP = ApplicationKey.from("com.example.other");

  private FakeResourceService resources;
  private ThymeleafViewFunctions functions;

  @BeforeEach
  void setUp() {
    this.resources = new FakeResourceService();
    this.functions = new ThymeleafViewFunctions(APP, () -> this.resources);
    this.functions.viewFunctionService = new FailingViewFunctionService();
    // A Universal API has no portal request, which is why localize cannot go through XP's own view function.
    this.functions.portalRequest = null;
  }

  @Test
  void localizes_from_the_application_being_previewed() {
    this.resources.put(APP, "/i18n/phrases.properties", "greeting=Hei");

    assertEquals("Hei", this.functions.localize(List.of("_key=greeting")));
  }

  @Test
  void answers_not_translated_for_a_key_the_application_does_not_have() {
    this.resources.put(APP, "/i18n/phrases.properties", "greeting=Hei");

    assertEquals(Phrases.NOT_TRANSLATED, this.functions.localize(List.of("_key=absent")));
  }

  @Test
  void localizes_from_the_application_the_template_named() {
    this.resources.put(APP, "/i18n/phrases.properties", "greeting=Hei");
    this.resources.put(OTHER_APP, "/i18n/phrases.properties", "greeting=Fra en annen app");

    assertEquals("Fra en annen app",
      this.functions.localize(List.of("_key=greeting", "_application=com.example.other")));
  }

  @Test
  void picks_the_bundle_matching_the_locale() {
    this.resources.put(APP, "/i18n/phrases.properties", "greeting=Hello");
    this.resources.put(APP, "/i18n/phrases_no.properties", "greeting=Hei");

    assertEquals("Hei", this.functions.localize(List.of("_key=greeting", "_locale=no")));
  }

  /** The previous implementation read only the first "_values" argument and split it on commas. */
  @Test
  void takes_a_value_per_argument() {
    this.resources.put(APP, "/i18n/phrases.properties", "greeting=Hei {0} og {1}!");

    assertEquals("Hei Tom og Ada!",
      this.functions.localize(List.of("_key=greeting", "_values=Tom", "_values=Ada")));
  }

  /** lib-thymeleaf's other accepted form, so a template written for production behaves the same here. */
  @Test
  void takes_several_values_in_one_braced_argument() {
    this.resources.put(APP, "/i18n/phrases.properties", "greeting=Hei {0} og {1}!");

    assertEquals("Hei Tom og Ada!", this.functions.localize(List.of("_key=greeting", "_values={Tom,Ada}")));
  }

  @Test
  void reads_an_explicitly_named_bundle() {
    this.resources.put(APP, "/i18n/other.properties", "greeting=Fra en annen bundle");

    assertEquals("Fra en annen bundle",
      this.functions.localize(List.of("_key=greeting", "_bundles=/i18n/other")));
  }

  /** Unlike the other view functions, this one answers from the parameters rather than from a portal request. */
  @Test
  void asset_url_answers_with_the_path_it_was_given() {
    assertEquals("/assets/app.css", this.functions.assetUrl(List.of("_path=/assets/app.css")));
  }

  /** localize must not reach XP's view function service, which would cache the bundle. */
  private static final class FailingViewFunctionService implements ViewFunctionService {
    @Override
    public Object execute(final ViewFunctionParams params) {
      throw new AssertionError("localize must resolve phrases itself, but called " + params.getName());
    }
  }
}
