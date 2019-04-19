package menagerie.model.search.rules;

import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;

/**
 * Rule that checks if the item's file path contains a string.
 */
public class FilePathRule extends SearchRule {

    private final String text;


    /**
     * @param text     String to find in item's file path. Case sensitive.
     * @param inverted Negate the rule.
     */
    public FilePathRule(String text, boolean inverted) {
        super(inverted);
        this.text = text;
    }

    @Override
    public boolean accept(Item item) {
        boolean result = item instanceof MediaItem && ((MediaItem) item).getFile().getAbsolutePath().contains(text);
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
