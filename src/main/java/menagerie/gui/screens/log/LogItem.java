package menagerie.gui.screens.log;

public class LogItem {

    private String css = null;
    private String text;


    public LogItem(String text) {
        this.text = text;
    }

    public LogItem(String text, String css) {
        this(text);
        this.css = css;
    }

    public String getCSS() {
        return css;
    }

    public String getText() {
        return text;
    }

}
