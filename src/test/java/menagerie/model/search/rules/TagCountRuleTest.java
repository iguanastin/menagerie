package menagerie.model.search.rules;

import menagerie.model.menagerie.Item;
import org.junit.jupiter.api.Test;

import static menagerie.model.search.rules.TagCountRule.Type.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TagCountRuleTest {

  Item item1 = new TestItemBuilder().build();
  Item item2 = new TestItemBuilder().tags("tag1", "tag2").build();
  Item item3 = new TestItemBuilder().tags("tag1", "tag2", "tag3").build();
  Item item4 = new TestItemBuilder().tags("tag1", "tag2", "tag3", "tag4").build();

  @Test
  void testEqualTo() {
    TagCountRule rule = new TagCountRule(EQUAL_TO, 2, false);

    assertFalse(rule.accept(item1));
    assertTrue(rule.accept(item2));
    assertFalse(rule.accept(item3));
  }

  @Test
  void testLessThan() {
    TagCountRule rule = new TagCountRule(LESS_THAN, 3, false);

    assertTrue(rule.accept(item1));
    assertTrue(rule.accept(item2));
    assertFalse(rule.accept(item3));
    assertFalse(rule.accept(item4));
  }

  @Test
  void testGreaterThan() {
    TagCountRule rule = new TagCountRule(GREATER_THAN, 3, false);

    assertFalse(rule.accept(item1));
    assertFalse(rule.accept(item2));
    assertFalse(rule.accept(item3));
    assertTrue(rule.accept(item4));
  }
}
