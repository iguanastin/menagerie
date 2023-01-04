package menagerie.model.search.rules;

import menagerie.model.menagerie.GroupItem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TitleRuleTest {

  private GroupItem createGroupItem(String title) {
    return new GroupItem(null, 1, 1, title);
  }

  private final GroupItem item1 = createGroupItem("Title with search word");
  private final GroupItem item2 = createGroupItem("Title without looked-for terms");
  private final GroupItem item3 = createGroupItem("Title with Search WORD (different case)");
  private final GroupItem item4 = createGroupItem("Title with search word and extra words");

  private final TitleRule rule1 = new TitleRule("search word", false);
  private final TitleRule rule2 = new TitleRule("search word", true);
  private final TitleRule rule3 = new TitleRule("Search WORD", false);
  private final TitleRule rule4 = new TitleRule("extra words", false);

  @Test
  void testAcceptRule1() {
    assertTrue(rule1.accept(item1));
    assertFalse(rule1.accept(item2));
    assertTrue(rule1.accept(item3));
    assertTrue(rule1.accept(item4));
  }

  @Test
  void testAcceptRule2() {
    assertFalse(rule2.accept(item1));
    assertTrue(rule2.accept(item2));
    assertFalse(rule2.accept(item3));
    assertFalse(rule2.accept(item4));
  }

  @Test
  void testAcceptRule3() {
    assertTrue(rule3.accept(item1));
    assertFalse(rule3.accept(item2));
    assertTrue(rule3.accept(item3));
    assertTrue(rule3.accept(item4));
  }

  @Test
  void testAcceptRule4() {
    assertFalse(rule4.accept(item1));
    assertFalse(rule4.accept(item2));
    assertFalse(rule4.accept(item3));
    assertTrue(rule4.accept(item4));
  }
}
