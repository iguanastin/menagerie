package menagerie.model.search;

import menagerie.model.search.rules.*;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Class to parse search rule string to rule instances.
 */
public abstract class SearchRuleParser {

  private static final Logger LOGGER = Logger.getLogger(Search.class.getName());

  public static List<SearchRule> parseRules(String search) {
    // this would be a test str"ing that doesn't tokenize the "quotes
    // This would be a test "string that DOES tokenize the quotes"
    // "This   " too
    List<String> tokens = tokenize(search);

    List<SearchRule> rules = new ArrayList<>();

    for (String arg : tokens) {
      if (arg == null || arg.isEmpty()) continue;

      boolean inverted = false;
      if (arg.charAt(0) == '-') {
        inverted = true;
        arg = arg.substring(1);
      }

      if (arg.startsWith("id:")) {
        parseIDRule(arg, rules, inverted);
      } else if (arg.startsWith("date:") || arg.startsWith("time:")) {
        parseDateRule(arg, rules, inverted);
      } else if (arg.startsWith("path:") || arg.startsWith("file:")) {
        parseFileRule(arg, rules, inverted);
      } else if (arg.startsWith("missing:")) {
        parseMissingRule(arg, rules, inverted);
      } else if (arg.startsWith("type:") || arg.startsWith("is:")) {
        parseTypeRule(arg, rules, inverted);
      } else if (arg.startsWith("tags:")) {
        parseTagsRule(arg, rules, inverted);
      } else if (arg.startsWith("title:")) {
        parseTitleRule(arg, rules, inverted);
      } else {
        rules.add(new TagRule(arg, inverted));
      }
    }

    return rules;
  }

  private static void parseIDRule(String arg, List<SearchRule> rules, boolean inverted) {
    String temp = arg.substring(arg.indexOf(':') + 1);
    IDRule.Type type = IDRule.Type.EQUAL_TO;
    if (temp.startsWith("<")) {
      type = IDRule.Type.LESS_THAN;
      temp = temp.substring(1);
    } else if (temp.startsWith(">")) {
      type = IDRule.Type.GREATER_THAN;
      temp = temp.substring(1);
    }
    try {
      rules.add(new IDRule(type, Integer.parseInt(temp), inverted));
    } catch (NumberFormatException e) {
      LOGGER.warning("Failed to convert parameter to integer: " + temp);
    }
  }

  private static void parseDateRule(String arg, List<SearchRule> rules, boolean inverted) {
    String temp = arg.substring(arg.indexOf(':') + 1);
    DateAddedRule.Type type = DateAddedRule.Type.EQUAL_TO;
    if (temp.startsWith("<")) {
      type = DateAddedRule.Type.LESS_THAN;
      temp = temp.substring(1);
    } else if (temp.startsWith(">")) {
      type = DateAddedRule.Type.GREATER_THAN;
      temp = temp.substring(1);
    }
    try {
      rules.add(new DateAddedRule(type, Long.parseLong(temp), inverted));
    } catch (NumberFormatException e) {
      LOGGER.warning("Failed to convert parameter to long: " + temp);
    }
  }

  private static void parseFileRule(String arg, List<SearchRule> rules, boolean inverted) {
    rules.add(new FilePathRule(arg.substring(arg.indexOf(':') + 1), inverted));
  }

  private static void parseMissingRule(String arg, List<SearchRule> rules, boolean inverted) {
    String type = arg.substring(arg.indexOf(':') + 1);
    switch (type.toLowerCase()) {
      case "md5" -> rules.add(new MissingRule(MissingRule.Type.MD5, inverted));
      case "file" -> rules.add(new MissingRule(MissingRule.Type.FILE, inverted));
      case "histogram", "hist" -> rules.add(new MissingRule(MissingRule.Type.HISTOGRAM, inverted));
      default -> LOGGER.warning("Unknown type for missing type: " + type);
    }
  }

  private static void parseTypeRule(String arg, List<SearchRule> rules, boolean inverted) {
    String type = arg.substring(arg.indexOf(':') + 1);

    switch(type.toLowerCase()) {
      case "group" -> rules.add(new TypeRule(TypeRule.Type.GROUP, inverted));
      case "media" -> rules.add(new TypeRule(TypeRule.Type.MEDIA, inverted));
      case "image" -> rules.add(new TypeRule(TypeRule.Type.IMAGE, inverted));
      case "video" -> rules.add(new TypeRule(TypeRule.Type.VIDEO, inverted));
      default -> LOGGER.warning("Unknown type for type: " + type);
    }
  }

  private static void parseTagsRule(String arg, List<SearchRule> rules, boolean inverted) {
    String temp = arg.substring(arg.indexOf(':') + 1);
    TagCountRule.Type type = TagCountRule.Type.EQUAL_TO;
    if (temp.startsWith("<")) {
      type = TagCountRule.Type.LESS_THAN;
      temp = temp.substring(1);
    } else if (temp.startsWith(">")) {
      type = TagCountRule.Type.GREATER_THAN;
      temp = temp.substring(1);
    }
    try {
      rules.add(new TagCountRule(type, Integer.parseInt(temp), inverted));
    } catch (NumberFormatException e) {
      LOGGER.warning("Failed to convert parameter to integer: " + temp);
    }
  }

  private static void parseTitleRule(String arg, List<SearchRule> rules, boolean inverted) {
    String temp = arg.substring(arg.indexOf(':') + 1);
    if (temp.charAt(0) == '"') temp = temp.substring(1); // Strip first quote
    if (temp.charAt(temp.length() - 1) == '"') temp = temp.substring(0, temp.length() - 1); // Strip second quote
    rules.add(new TitleRule(temp, inverted));
  }

  private static List<String> tokenize(String search) {
    List<String> tokens = new ArrayList<>();
    int i = 0;
    while (i < search.length()) {
      // Read a word
      int k = i + 1;
      while (k < search.length() && !Character.isWhitespace(search.charAt(k))) {
        if (search.charAt(k - 1) == ':' && search.charAt(k) == '"') {
          k++;
          while (k < search.length() && search.charAt(k) != '"') {
            k++;
          }
        }

        k++;
      }

      tokens.add(search.substring(i, k));
      i = k;
      while (i < search.length() && search.charAt(i) == ' ') {
        i++;
      }
    }

    return tokens;
  }
}
