package menagerie.model.search;

public class IntSearchRule {

    public enum Type {
        LESS_THAN,
        GREATER_THAN,
        EQUAL
    }

    private final int value;
    private final Type type;

    public IntSearchRule(Type type, int value) {
        this.type = type;
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public Type getType() {
        return type;
    }

    public boolean accept(int i) {
        if (getType() == Type.LESS_THAN) {
            return i < getValue();
        } else if (getType() == Type.GREATER_THAN) {
            return i > getValue();
        } else if (getType() == Type.EQUAL) {
            return i == getValue();
        }

        return false;
    }

}
