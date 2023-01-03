package menagerie.model.search.rules;

import menagerie.model.menagerie.GroupItem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TitleRuleTest {

  private GroupItem createGroupItem(String title) {
    return new GroupItem(null, 1, 1, title);
  }

  @Test
  void testAccept() {
    GroupItem item1 = createGroupItem("Title with search word");
    GroupItem item2 = createGroupItem("Title without looked-for terms");
    GroupItem item3 = createGroupItem("Title with Search WORD (different case)");
    GroupItem item4 = createGroupItem("Title with search word and extra words");

    TitleRule rule1 = new TitleRule("search word", false);
    TitleRule rule2 = new TitleRule("search word", true);
    TitleRule rule3 = new TitleRule("Search WORD", false);
    TitleRule rule4 = new TitleRule("extra words", false);

    assertTrue(rule1.accept(item1));
    assertFalse(rule1.accept(item2));
    assertTrue(rule1.accept(item3));
    assertTrue(rule1.accept(item4));

    assertFalse(rule2.accept(item1));
    assertTrue(rule2.accept(item2));
    assertFalse(rule2.accept(item3));
    assertFalse(rule2.accept(item4));

    assertTrue(rule3.accept(item1));
    assertFalse(rule3.accept(item2));
    assertTrue(rule3.accept(item3));
    assertTrue(rule3.accept(item4));

    assertFalse(rule4.accept(item1));
    assertFalse(rule4.accept(item2));
    assertFalse(rule4.accept(item3));
    assertTrue(rule4.accept(item4));
  }
}
