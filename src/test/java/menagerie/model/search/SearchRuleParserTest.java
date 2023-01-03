package menagerie.model.search;

import menagerie.model.search.rules.DateAddedRule;
import menagerie.model.search.rules.IDRule;
import menagerie.model.search.rules.SearchRule;
import menagerie.model.search.rules.TitleRule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchRuleParserTest {

  @Test
  void testParseIDRule() {
    // Test parsing ID rule with equal to operator
    List<SearchRule> rules = SearchRuleParser.parseRules("id:123");
    assertEquals(1, rules.size());
    assertTrue(rules.get(0) instanceof IDRule);
    assertEquals(123, ((IDRule) rules.get(0)).getId());
    assertEquals(IDRule.Type.EQUAL_TO, ((IDRule) rules.get(0)).getType());

    // Test parsing ID rule with less than operator
    rules = SearchRuleParser.parseRules("id:<123");
    assertEquals(1, rules.size());
    assertTrue(rules.get(0) instanceof IDRule);
    assertEquals(123, ((IDRule) rules.get(0)).getId());
    assertEquals(IDRule.Type.LESS_THAN, ((IDRule) rules.get(0)).getType());

    // Test parsing ID rule with greater than operator
    rules = SearchRuleParser.parseRules("id:>123");
    assertEquals(1, rules.size());
    assertTrue(rules.get(0) instanceof IDRule);
    assertEquals(123, ((IDRule) rules.get(0)).getId());
    assertEquals(IDRule.Type.GREATER_THAN, ((IDRule) rules.get(0)).getType());
  }

  @Test
  void testParseDateRule() {
    // Test parsing date rule with equal to operator
    List<SearchRule> rules = SearchRuleParser.parseRules("date:1671647540865");
    assertEquals(1, rules.size());
    assertTrue(rules.get(0) instanceof DateAddedRule);
    assertEquals(1671647540865L, ((DateAddedRule) rules.get(0)).getTime());
    assertEquals(DateAddedRule.Type.EQUAL_TO, ((DateAddedRule) rules.get(0)).getType());

    // Test parsing date rule with less than operator
    rules = SearchRuleParser.parseRules("date:<1671647540865");
    assertEquals(1, rules.size());
    assertTrue(rules.get(0) instanceof DateAddedRule);
    assertEquals(1671647540865L, ((DateAddedRule) rules.get(0)).getTime());
    assertEquals(DateAddedRule.Type.LESS_THAN, ((DateAddedRule) rules.get(0)).getType());

    // Test parsing date rule with greater than operator
    rules = SearchRuleParser.parseRules("date:>1671647540865");
    assertEquals(1, rules.size());
    assertTrue(rules.get(0) instanceof DateAddedRule);
    assertEquals(1671647540865L, ((DateAddedRule) rules.get(0)).getTime());
    assertEquals(DateAddedRule.Type.GREATER_THAN, ((DateAddedRule) rules.get(0)).getType());
  }

  @Test
  void testParseMultipleRules() {
    // Test parsing two different rules
    List<SearchRule> rules = SearchRuleParser.parseRules("id:123 title:\"My Image\"");
    assertEquals(2, rules.size());
    assertTrue(rules.get(0) instanceof IDRule);
    assertTrue(rules.get(1) instanceof TitleRule);
    assertEquals(123, ((IDRule) rules.get(0)).getId());
  }
}

