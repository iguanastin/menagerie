package menagerie.model.search;

import menagerie.model.menagerie.ImageInfo;
import menagerie.model.menagerie.Tag;

public class TagRule extends SearchRule {

    private final boolean exclude;
    private final Tag tag;


    public TagRule(Tag tag, boolean exclude) {
        priority = 25;

        this.tag = tag;
        this.exclude = exclude;
    }

    @Override
    public boolean accept(ImageInfo img) {
        if (exclude) {
            return !img.hasTag(tag);
        } else {
            return img.hasTag(tag);
        }
    }

    @Override
    public String toString() {
        if (exclude) {
            return "Tag Rule: exclude \"" + tag.getName() + "\"";
        } else {
            return "Tag Rule: \"" + tag.getName() + "\"";
        }
    }

}
