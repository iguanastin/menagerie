package menagerie.model.search.rules;

import menagerie.model.menagerie.Item;

public class IDRule extends SearchRule {

    public enum Type {
        LESS_THAN,
        GREATER_THAN,
        EQUAL_TO
    }

    private final int id;
    private final Type type;


    public IDRule(Type type, int value, boolean inverted) {
        super(inverted);
        priority = 1;

        this.type = type;
        this.id = value;
    }

    @Override
    public boolean accept(Item item) {
        boolean result = false;
        switch (type) {
            case LESS_THAN:
                result = item.getId() < id;
                break;
            case GREATER_THAN:
                result = item.getId() > id;
                break;
            case EQUAL_TO:
                result = item.getId() == id;
                break;
        }

        if (isInverted()) result = !result;

        return result;
    }

    @Override
    public String toString() {
        String result = "ID Rule: " + type + " " + id;
        if (isInverted()) result += " [inverted]";
        return result;
    }

}
