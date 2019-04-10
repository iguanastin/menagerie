package menagerie.model.search.rules;

import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;

/**
 * Rule that checks the type of item.
 */
public class TypeRule extends SearchRule {

    /**
     * @param type     Type of item to accept.
     * @param inverted Negate this rule.
     */
    public TypeRule(Type type, boolean inverted) {
        super(inverted);
        this.type = type;
    }

    private final Type type;


    public enum Type {
        GROUP, MEDIA
    }

    @Override
    public boolean accept(Item item) {
        boolean result = (item instanceof GroupItem && type == Type.GROUP) || (item instanceof MediaItem && type == Type.MEDIA);
        if (isInverted()) result = !result;
        return result;
    }

    @Override
    public String toString() {
        String result = "Type Rule: " + type;
        if (isInverted()) result += " [inverted]";
        return result;
    }

}
