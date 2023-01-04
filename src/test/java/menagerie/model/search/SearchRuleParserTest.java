package menagerie.model.search;

import menagerie.model.search.rules.DateAddedRule;
import menagerie.model.search.rules.IDRule;
import menagerie.model.search.rules.SearchRule;
import menagerie.model.search.rules.TitleRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class SearchRuleParserTest {


  @ParameterizedTest
  @MethodSource("idRuleToRange")
  void testParseIDRule(String ruleString, IDRule.Type type) {
    List<SearchRule> rules = SearchRuleParser.parseRules(ruleString);
    assertEquals(1, rules.size());
    assertTrue(rules.get(0) instanceof IDRule);
    assertEquals(123, ((IDRule) rules.get(0)).getId());
    assertEquals(type, ((IDRule) rules.get(0)).getType());
  }

  private static Stream<Arguments> idRuleToRange() {
    return Stream.of(
        arguments("id:123", IDRule.Type.EQUAL_TO),
        arguments("id:<123", IDRule.Type.LESS_THAN),
        arguments("id:>123", IDRule.Type.GREATER_THAN)
    );
  }

  @ParameterizedTest
  @MethodSource("dateRuleToRange")
  void testParseDateRule(String ruleString, DateAddedRule.Type type) {
    List<SearchRule> rules = SearchRuleParser.parseRules(ruleString);
    assertEquals(1, rules.size());
    assertTrue(rules.get(0) instanceof DateAddedRule);
    assertEquals(1671647540865L, ((DateAddedRule) rules.get(0)).getTime());
    assertEquals(type, ((DateAddedRule) rules.get(0)).getType());
  }

  private static Stream<Arguments> dateRuleToRange() {
    return Stream.of(
        arguments("date:1671647540865", DateAddedRule.Type.EQUAL_TO),
        arguments("date:<1671647540865", DateAddedRule.Type.LESS_THAN),
        arguments("date:>1671647540865", DateAddedRule.Type.GREATER_THAN)
    );
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

