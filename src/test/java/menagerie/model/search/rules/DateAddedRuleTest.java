package menagerie.model.search.rules;

import menagerie.model.menagerie.Item;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DateAddedRuleTest {

    @Test
    public void testLessThanRule() {
        long time = new Date().getTime();
        Item item1 = new TestItemBuilder().dateAdded(time - 1000).build();
        Item item2 = new TestItemBuilder().dateAdded(time + 1000).build();

        DateAddedRule rule = new DateAddedRule(DateAddedRule.Type.LESS_THAN, time, false);
        assertTrue(rule.accept(item1));
        assertFalse(rule.accept(item2));

        rule = new DateAddedRule(DateAddedRule.Type.LESS_THAN, time, true);
        assertFalse(rule.accept(item1));
        assertTrue(rule.accept(item2));
    }

    @Test
    public void testGreaterThanRule() {
        long time = new Date().getTime();
        Item item1 = new TestItemBuilder().dateAdded(time - 1000).build();
        Item item2 = new TestItemBuilder().dateAdded(time + 1000).build();

        DateAddedRule rule = new DateAddedRule(DateAddedRule.Type.GREATER_THAN, time, false);
        assertFalse(rule.accept(item1));
        assertTrue(rule.accept(item2));

        rule = new DateAddedRule(DateAddedRule.Type.GREATER_THAN, time, true);
        assertTrue(rule.accept(item1));
        assertFalse(rule.accept(item2));
    }

    @Test
    public void testEqualToRule() {
        long time = new Date().getTime();
        Item item1 = new TestItemBuilder().dateAdded(time - 1000).build();
        Item item2 = new TestItemBuilder().dateAdded(time).build();
        Item item3 = new TestItemBuilder().dateAdded(time + 1000).build();

        DateAddedRule rule = new DateAddedRule(DateAddedRule.Type.EQUAL_TO, time, false);
        assertFalse(rule.accept(item1));
        assertTrue(rule.accept(item2));
        assertFalse(rule.accept(item3));

        rule = new DateAddedRule(DateAddedRule.Type.EQUAL_TO, time, true);
        assertTrue(rule.accept(item1));
        assertFalse(rule.accept(item2));
        assertTrue(rule.accept(item3));
    }

}
