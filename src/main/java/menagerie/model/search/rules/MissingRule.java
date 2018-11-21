package menagerie.model.search.rules;

import menagerie.model.menagerie.ImageInfo;

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
    public boolean accept(ImageInfo img) {
        boolean result = false;
        switch (type) {
            case MD5:
                result = img.getMD5() == null;
                break;
            case FILE:
                result = img.getFile() == null || !img.getFile().exists();
                break;
            case HISTOGRAM:
                result = img.getHistogram() == null;
                break;
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
