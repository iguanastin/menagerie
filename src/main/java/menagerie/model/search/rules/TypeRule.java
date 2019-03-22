package menagerie.model.search.rules;

import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;

public class TypeRule extends SearchRule {

    public enum Type {
        GROUP,
        MEDIA
    }

    private final Type type;


    public TypeRule(Type type, boolean inverted) {
        super(inverted);
        this.type = type;
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
