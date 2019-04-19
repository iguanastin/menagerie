package menagerie.model.search.rules;

import menagerie.model.menagerie.Item;

/**
 * Rule that compares tag counts.
 */
public class TagCountRule extends SearchRule {

    public enum Type {
        EQUAL_TO, GREATER_THAN, LESS_THAN,
    }

    private final Type type;
    private final int value;


    /**
     * @param type   Type of comparison.
     * @param value  Value to compare against.
     * @param invert Negate this rule.
     */
    public TagCountRule(Type type, int value, boolean invert) {
        super(invert);
        this.type = type;
        this.value = value;
    }

    @Override
    public boolean accept(Item item) {
        boolean result = false;
        switch (type) {
            case EQUAL_TO:
                result = item.getTags().size() == value;
                break;
            case LESS_THAN:
                result = item.getTags().size() < value;
                break;
            case GREATER_THAN:
                result = item.getTags().size() > value;
                break;
        }

        if (isInverted()) result = !result;

        return result;
    }

    @Override
    public String toString() {
        String result = "Tag Count Rule: " + type + " " + value;
        if (isInverted()) result += " [inverted]";
        return result;
    }

}
