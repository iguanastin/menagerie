package menagerie.model.search.rules;

import menagerie.model.menagerie.Item;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TagRuleTest {

  private final String TAG_CAT = "cat";
  private final String TAG_DOG = "dog";
  private final String TAG_BIRD = "bird";


  @Test
  void testAcceptWithMatchingTag() {
    Item item = new TestItemBuilder().tags(TAG_CAT, TAG_BIRD).build();
    TagRule rule = new TagRule(TAG_CAT, false);

    assertTrue(rule.accept(item));
  }

  @Test
  void testAcceptWithNonMatchingTag() {
    Item item = new TestItemBuilder().tags(TAG_CAT, TAG_BIRD).build();
    TagRule rule = new TagRule(TAG_DOG, false);

    assertFalse(rule.accept(item));
  }
}
