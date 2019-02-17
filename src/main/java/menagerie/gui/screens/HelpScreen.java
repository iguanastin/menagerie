package menagerie.gui.screens;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

public class HelpScreen extends Screen {


    public HelpScreen(Node onShowDisable) {
        super(onShowDisable);

        setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                hide();
            }
        });

        setPadding(new Insets(25));
        BorderPane root = new BorderPane();
        root.setPrefSize(600, 800);
        root.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        root.setStyle("-fx-background-color: -fx-base;");
        DropShadow effect = new DropShadow();
        effect.setSpread(0.5);
        root.setEffect(effect);
        setCenter(root);

        BorderPane header = new BorderPane();
        root.setTop(header);

        Label title = new Label("Help");
        header.setLeft(title);
        BorderPane.setMargin(title, new Insets(5));

        Button exit = new Button("X");
        header.setRight(exit);
        exit.setOnAction(event -> hide());

        Font boldItalic = new Font("System Bold Italic", 12);

        // -------------------------- Hotkeys tab ----------------------------------------------------------------------
        Label l = new Label("Ctrl+H");
        l.setFont(boldItalic);
        HBox helpHotkey = new HBox(5, l, new Label("- Show this menu"));

        l = new Label("Ctrl+F");
        l.setFont(boldItalic);
        HBox findHotkey = new HBox(5, l, new Label("- Focus search bar"));

        l = new Label("Ctrl+E");
        l.setFont(boldItalic);
        HBox editHotkey = new HBox(5, l, new Label("- Focus tag editor"));

        l = new Label("Ctrl+Q");
        l.setFont(boldItalic);
        HBox quitHotkey = new HBox(5, l, new Label("- Quit"));

        l = new Label("Ctrl+S");
        l.setFont(boldItalic);
        HBox settingsHotkey = new HBox(5, l, new Label("- Show settings"));

        l = new Label("Ctrl+T");
        l.setFont(boldItalic);
        HBox tagsHotkey = new HBox(5, l, new Label("- Show tag list"));

        l = new Label("Ctrl+D");
        l.setFont(boldItalic);
        HBox duplicateHotkey = new HBox(5, l, new Label("- Find duplicates among currently selected items"));

        l = new Label("Ctrl+I");
        l.setFont(boldItalic);
        HBox importHotkey = new HBox(5, l, new Label("- Show import dialog for files"));

        l = new Label("Ctrl+Shift+I");
        l.setFont(boldItalic);
        HBox importFolderHotkey = new HBox(5, l, new Label("- Show import dialog for folders (recursive)"));

        l = new Label("Del");
        l.setFont(boldItalic);
        HBox delHotkey = new HBox(5, l, new Label("- Delete currently selected items"));

        l = new Label("Ctrl+Del");
        l.setFont(boldItalic);
        HBox forgetHotkey = new HBox(5, l, new Label("- Forget currently selected items from database"));

        VBox v = new VBox(5, helpHotkey, findHotkey, editHotkey, quitHotkey, settingsHotkey, tagsHotkey, duplicateHotkey, importHotkey, importFolderHotkey, delHotkey, forgetHotkey);
        ScrollPane sp = new ScrollPane(v);
        sp.setFitToWidth(true);
        sp.setPadding(new Insets(5));
        Tab hotkeys = new Tab("Hotkeys", sp);
        hotkeys.setClosable(false);

        // ---------------------------------- Search help tab ----------------------------------------------------------
        Label l1 = new Label("Excluding tags:");
        l1.setFont(boldItalic);
        l1.minWidthProperty().bind(l1.prefWidthProperty());
        Label l2 = new Label("Prepend a dash (-) to a search element to logically negate it. Examples:\n\"-tagme\" - excludes all files tagged with tagme\n\"-missing:histogram\" - excludes all files that are missing a histogram");
        l2.setWrapText(true);
        HBox h1 = new HBox(5, l1, l2);

        l1 = new Label("Specific IDs:");
        l1.setFont(boldItalic);
        l1.minWidthProperty().bind(l1.prefWidthProperty());
        l2 = new Label("Use the ID modifier (id:) to search for specific IDs or IDs in a range. Examples:\n\"id:>1234\" - includes only files whose ID is greater than 1234\n\"id:4321\" - includes only the file whose id is 4321");
        l2.setWrapText(true);
        HBox h2 = new HBox(5, l1, l2);

        l1 = new Label("Search by time:");
        l1.setFont(boldItalic);
        l1.minWidthProperty().bind(l1.prefWidthProperty());
        l2 = new Label("Use the time modifier (time:) to search for a specific time that files were added. Examples:\n\"time:<1541361629900\" - includes only files that were added before the given time in milliseconds after epoch\n\"time:1541361629900\" - includes only files that were added at exactly the given time after epoch");
        l2.setWrapText(true);
        HBox h3 = new HBox(5, l1, l2);

        l1 = new Label("Search by file path:");
        l1.setFont(boldItalic);
        l1.minWidthProperty().bind(l1.prefWidthProperty());
        l2 = new Label("Use the path modifier (path:) to search for files that contain the supplied string. Examples:\n\"path:C:/Users/PERSON/Documents\" - includes only files in PERSON's Documents folder");
        l2.setWrapText(true);
        HBox h4 = new HBox(5, l1, l2);

        l1 = new Label("Missing attributes:");
        l1.setFont(boldItalic);
        l1.minWidthProperty().bind(l1.prefWidthProperty());
        l2 = new Label("Use the missing modifier (missing:) to search for files that missing certain attributes. Examples:\n\"missing:md5\" - includes only files that are missing an MD5 hash\n\"missing:file\" - includes only files that point to a non-existent file\n\"missing:histogram\" - includes only files that are missing a histogram");
        l2.setWrapText(true);
        HBox h5 = new HBox(5, l1, l2);

        v = new VBox(5, h1, h2, h3, h4, h5);
        sp = new ScrollPane(v);
        sp.setFitToWidth(true);
        sp.setPadding(new Insets(5));
        Tab searching = new Tab("Searching", sp);
        searching.setClosable(false);

        TabPane tabPane = new TabPane(hotkeys, searching);
        root.setCenter(tabPane);
    }

}
