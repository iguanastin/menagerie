package menagerie.gui;

/**
 * Current JavaFX applications require a plain boot class which does NOT inherit from javafx.application.Application.
 * Otherwise, "Error: JavaFX runtime components are missing, and are required to run this application."
 */
public class GUIStarter {

    public static void main(final String[] args) {
        Main.main(args);
    }
}