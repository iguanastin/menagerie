package menagerie.model.search;

import menagerie.model.menagerie.ImageInfo;

public class FilePathRule extends SearchRule {

    private final String text;


    public FilePathRule(String text) {
        this.text = text;
    }

    @Override
    public boolean accept(ImageInfo img) {
        return img.getFile().getAbsolutePath().contains(text);
    }

    @Override
    public String toString() {
        return "File Path Rule: \"" + text + "\"";
    }

}
