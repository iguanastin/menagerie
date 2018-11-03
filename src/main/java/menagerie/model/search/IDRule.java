package menagerie.model.search;

import menagerie.model.menagerie.ImageInfo;

public class IDRule extends SearchRule {

    public enum Type {
        LESS_THAN,
        GREATER_THAN,
        EQUAL_TO
    }

    private final int id;
    private final Type type;


    public IDRule(Type type, int value) {
        priorty = 1;

        this.type = type;
        this.id = value;
    }

    public int getId() {
        return id;
    }

    public Type getType() {
        return type;
    }

    @Override
    public boolean accept(ImageInfo img) {
        if (type == Type.LESS_THAN) {
            return img.getId() < id;
        } else if (type == Type.GREATER_THAN) {
            return img.getId() > id;
        } else if (type == Type.EQUAL_TO) {
            return img.getId() == id;
        }

        return false;
    }

    @Override
    public String toString() {
        return "ID Rule: " + type + " " + id;
    }

}
