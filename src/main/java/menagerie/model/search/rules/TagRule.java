package menagerie.model.search.rules;

import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.Tag;

/**
 * Rule that checks if an item has a tag.
 */
public class TagRule extends SearchRule {

    private final String tag;


    /**
     * @param tag     Tag to find.
     * @param exclude Negate this rule.
     */
    public TagRule(String tag, boolean exclude) {
        super(exclude);
        priority = 25;

        this.tag = tag;
    }

    @Override
    public boolean accept(Item item) {
        boolean result = false;

        for (Tag t : item.getTags()) {
            if (t.getName().equalsIgnoreCase(tag)) {
                result = true;
                break;
            }
        }

        if (isInverted()) result = !result;
        return result;
    }

    @Override
    public String toString() {
        String result = "Tag Rule: \"" + tag + "\"";
        if (isInverted()) result += " [inverted]";
        return result;
    }

}
