package menagerie.model.search;

import menagerie.model.menagerie.ImageInfo;

public class MissingRule extends SearchRule {

    public enum Type {
        MD5,
        FILE,
        HISTOGRAM
    }

    private final Type type;


    public MissingRule(Type type) {
        this.type = type;
    }

    @Override
    public boolean accept(ImageInfo img) {
        switch (type) {
            case MD5:
                return img.getMD5() == null;
            case FILE:
                return img.getFile() == null || !img.getFile().exists();
            case HISTOGRAM:
                return img.getHistogram() == null;
        }

        return false;
    }

    @Override
    public String toString() {
        return "Missing Rule: " + type;
    }

}
