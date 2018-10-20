package menagerie.model.search;

import menagerie.model.ImageInfo;

import java.util.Date;

public class DateAddedRule extends SearchRule {

    public enum Type {
        LESS_THAN,
        GREATER_THAN,
        EQUAL_TO
    }

    private final long time;
    private final Type type;


    public DateAddedRule(Type type, long time) {
        priorty = 10;

        this.time = time;
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public long getTime() {
        return time;
    }

    @Override
    public boolean accept(ImageInfo img) {
        if (type == DateAddedRule.Type.LESS_THAN) {
            return img.getDateAdded() < time;
        } else if (type == DateAddedRule.Type.GREATER_THAN) {
            return img.getDateAdded() > time;
        } else if (type == DateAddedRule.Type.EQUAL_TO) {
            return img.getDateAdded() == time;
        }

        return false;
    }

    @Override
    public String toString() {
        return "Added Date Rule: " + type + " " + time + " (" + new Date(time) + ")";
    }

}
