package menagerie.model.search.rules;

import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.Tag;

/**
 * Rule that checks if an item has a tag.
 */
public class TagRule extends SearchRule {

    private final Tag tag;


    /**
     * @param tag     Tag to find.
     * @param exclude Negate this rule.
     */
    public TagRule(Tag tag, boolean exclude) {
        super(exclude);
        priority = 25;

        this.tag = tag;
    }

    @Override
    public boolean accept(Item item) {
        if (isInverted()) {
            return !item.hasTag(tag);
        } else {
            return item.hasTag(tag);
        }
    }

    @Override
    public String toString() {
        String result = "Tag Rule: \"" + tag.getName() + "\"";
        if (isInverted()) result += " [inverted]";
        return result;
    }

}
