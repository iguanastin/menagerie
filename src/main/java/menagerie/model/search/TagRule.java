package menagerie.model.search;

import menagerie.model.menagerie.ImageInfo;
import menagerie.model.menagerie.Tag;

public class TagRule extends SearchRule {

    private final Tag tag;


    public TagRule(Tag tag, boolean exclude) {
        super(exclude);
        priority = 25;

        this.tag = tag;
    }

    @Override
    public boolean accept(ImageInfo img) {
        if (isInverted()) {
            return !img.hasTag(tag);
        } else {
            return img.hasTag(tag);
        }
    }

    @Override
    public String toString() {
        String result = "Tag Rule: \"" + tag.getName() + "\"";
        if (isInverted()) result += " [inverted]";
        return result;
    }

}
