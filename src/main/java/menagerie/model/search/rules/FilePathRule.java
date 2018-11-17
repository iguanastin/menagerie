package menagerie.model.search.rules;

import menagerie.model.menagerie.ImageInfo;

public class FilePathRule extends SearchRule {

    private final String text;


    public FilePathRule(String text, boolean inverted) {
        super(inverted);
        this.text = text;
    }

    @Override
    public boolean accept(ImageInfo img) {
        boolean result = img.getFile().getAbsolutePath().contains(text);
        if (isInverted()) result = !result;
        return result;
    }

    @Override
    public String toString() {
        String result = "File Path Rule: \"" + text + "\"";
        if (isInverted()) result += " [inverted]";
        return result;
    }

}
