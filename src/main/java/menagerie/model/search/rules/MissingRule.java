package menagerie.model.search.rules;

import menagerie.model.menagerie.MediaItem;
import menagerie.model.menagerie.Item;

public class MissingRule extends SearchRule {

    public enum Type {
        MD5,
        FILE,
        HISTOGRAM
    }

    private final Type type;


    public MissingRule(Type type, boolean inverted) {
        super(inverted);
        this.type = type;
    }

    @Override
    public boolean accept(Item item) {
        boolean result = false;
        if (item instanceof MediaItem) {
            switch (type) {
                case MD5:
                    result = ((MediaItem) item).getMD5() == null;
                    break;
                case FILE:
                    result = ((MediaItem) item).getFile() == null || !((MediaItem) item).getFile().exists();
                    break;
                case HISTOGRAM:
                    result = ((MediaItem) item).getHistogram() == null;
                    break;
            }
        }

        if (isInverted()) result = !result;

        return result;
    }

    @Override
    public String toString() {
        String result = "Missing Rule: " + type;
        if (isInverted()) result += " [inverted]";
        return result;
    }

}
