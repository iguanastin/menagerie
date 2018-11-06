package menagerie.model.search;

import menagerie.model.menagerie.ImageInfo;

public class TagCountRule extends SearchRule {

    public enum Type {
        EQUAL_TO,
        GREATER_THAN,
        LESS_THAN,
    }

    private final Type type;
    private final int value;


    public TagCountRule(Type type, int value) {
        this.type = type;
        this.value = value;
    }

    @Override
    public boolean accept(ImageInfo img) {
        switch (type) {
            case EQUAL_TO:
                return img.getTags().size() == value;
            case LESS_THAN:
                return img.getTags().size() < value;
            case GREATER_THAN:
                return img.getTags().size() > value;
        }

        return false;
    }

    @Override
    public String toString() {
        return "Tag Count Rule: " + type + " " + value;
    }

}
