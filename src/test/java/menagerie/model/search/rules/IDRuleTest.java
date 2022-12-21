package menagerie.model.search.rules;

import menagerie.model.menagerie.Item;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IDRuleTest {

    private final Item item1 = new TestItemBuilder().id(1).build();
    private final Item item2 = new TestItemBuilder().id(5).build();
    private final Item item3 = new TestItemBuilder().id(10).build();

    @Test
    void testLessThanRule() {
        IDRule rule = new IDRule(IDRule.Type.LESS_THAN, 5, false);

        assertTrue(rule.accept(item1));
        assertFalse(rule.accept(item2));
        assertFalse(rule.accept(item3));
    }

    @Test
    void testGreaterThanRule() {
        IDRule rule = new IDRule(IDRule.Type.GREATER_THAN, 5, false);

        assertFalse(rule.accept(item1));
        assertFalse(rule.accept(item2));
        assertTrue(rule.accept(item3));
    }

    @Test
    void testEqualToRule() {
        IDRule rule = new IDRule(IDRule.Type.EQUAL_TO, 5, false);

        assertFalse(rule.accept(item1));
        assertTrue(rule.accept(item2));
        assertFalse(rule.accept(item3));
    }
}
