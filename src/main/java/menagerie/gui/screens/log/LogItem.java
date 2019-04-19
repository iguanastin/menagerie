package menagerie.gui.screens.log;

/**
 * A class representing a single log item.
 */
public class LogItem {

    private String css = null;
    private String text;


    /**
     * Creates a log item with default CSS.
     *
     * @param text Log message.
     */
    public LogItem(String text) {
        this.text = text;
    }

    /**
     * Creates a log item with specified CSS
     *
     * @param text Log message.
     * @param css  CSS styling.
     */
    public LogItem(String text, String css) {
        this(text);
        this.css = css;
    }

    /**
     * @return The CSS styling.
     */
    public String getCSS() {
        return css;
    }

    /**
     * @return The log message.
     */
    public String getText() {
        return text;
    }

}
