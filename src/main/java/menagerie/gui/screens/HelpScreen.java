package menagerie.gui.screens;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import menagerie.model.menagerie.Tag;
import menagerie.util.Filters;

public class HelpScreen extends Screen {

    private final Font BOLD_ITALIC = new Font("System Bold Italic", 12);
    private final Insets LEFT20 = new Insets(0, 0, 0, 20);
    private final Insets ALL5 = new Insets(5);


    public HelpScreen() {
        addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                close();
            }
        });

        setPadding(new Insets(25));
        BorderPane root = new BorderPane();
        root.setPrefSize(800, 800);
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
        BorderPane.setMargin(title, ALL5);

        Button exit = new Button("X");
        header.setRight(exit);
        exit.setOnAction(event -> close());

        header.setBottom(new Separator());

        // ---------------------------------- Search help tab ----------------------------------------------------------
        TabPane tabPane = new TabPane(constructGeneralTab(), constructExplorerTab(), constructTagEditingTab(), constructSearchingTab(), constructHotKeysTab());
        root.setCenter(tabPane);
    }

    private Tab constructExplorerTab() {
        VBox v = new VBox(5);

        Label l = new Label("The explorer is the main screen; the unified hub for viewing, searching, and editing everything in your Menagerie.\n ");
        l.setWrapText(true);
        v.getChildren().addAll(l, new Separator());

        l = new Label("Preview info");
        l.setFont(BOLD_ITALIC);
        v.getChildren().add(l);
        l = new Label("- Click the info box in the bottom right of the preview to toggle the full view with more info about the selected item.");
        l.setPadding(LEFT20);
        l.setWrapText(true);
        v.getChildren().add(l);

        l = new Label("Previewing images");
        l.setFont(BOLD_ITALIC);
        v.getChildren().add(l);
        l = new Label("- Click to toggle between 100% zoom and fitted zoom\n- Click and drag to pan the image around\n- Use the scroll wheel to zoom in increments");
        l.setPadding(LEFT20);
        l.setWrapText(true);
        v.getChildren().add(l);

        l = new Label("Previewing videos");
        l.setFont(BOLD_ITALIC);
        v.getChildren().add(l);
        l = new Label("- Left click the video to pause or resume it\n- Right click the video to toggle mute\n- Scroll with mouse over the video to advance or retreat 10 seconds of video");
        l.setPadding(LEFT20);
        l.setWrapText(true);
        v.getChildren().add(l);

        ScrollPane sp = new ScrollPane(v);
        sp.setFitToWidth(true);
        sp.setPadding(ALL5);
        Tab t = new Tab("Explorer", sp);
        t.setClosable(false);
        return t;
    }

    private Tab constructGeneralTab() {
        VBox v = new VBox(5);

        Label l = new Label("Welcome to Menagerie, an organizational tool images and videos.\n\nMenagerie is a tag based system that tracks local files on your PC, allowing you organize, search, and view them faster and more efficiently.\n ");
        l.setWrapText(true);
        v.getChildren().add(l);

        l = new Label("Features");
        l.setFont(BOLD_ITALIC);
        v.getChildren().add(l);

        String[] strs = {"- Tagging", "- Searching tags and various properties", String.format("- Support for image formats: %s", String.join(", ", Filters.IMAGE_EXTS)), String.format("- Support for video formats (requires VLC to be installed): %s", String.join(", ", Filters.VIDEO_EXTS)), "- Exact file duplicate detection", "- Image fuzzy duplicate detection", "- Item groups", "- Auto-importing from a folder", "- Dragging and dropping files to and from local disk and web", "- Slideshow viewing", "- Etc."};
        for (String str : strs) {
            l = new Label(str);
            l.setWrapText(true);
            l.setPadding(LEFT20);
            v.getChildren().add(l);
        }

        ScrollPane sp = new ScrollPane(v);
        sp.setFitToWidth(true);
        sp.setPadding(ALL5);
        Tab t = new Tab("General", sp);
        t.setClosable(false);
        return t;
    }

    private Tab constructTagEditingTab() {

        VBox v = new VBox(5);

        Label l = new Label(String.format("Notes:\n    - When editing tags with the tag edit field (bottom left of the screen), edit will affect all selected items.\n    - Tag names must match the regex: \"%s\"\n        Or, in other words, all characters must be alphanumerical or standard US keyboard symbols.\n    - Tag edits can be undone by pressing CTRL+Z.\n ", Tag.NAME_REGEX));
        l.setWrapText(true);
        v.getChildren().addAll(l, new Separator());

        l = new Label("Useful hotkeys:\n    Ctrl+E - Focuses the tag editor\n    Ctrl+Space or Tab - Autocomplete a tag name\n    Ctrl+Enter - Autocomplete a tag and commit the edit\n ");
        l.setWrapText(true);
        v.getChildren().addAll(l, new Separator());

        l = new Label("Adding tags");
        l.setFont(BOLD_ITALIC);
        v.getChildren().add(l);
        l = new Label("To add a tag, type a tag name into the tag editor field and press enter.\nMultiple tags can be typed out (separated by spaces) before committing the changes.\nAdding a tag name that does not exist will create a new tag.");
        l.setWrapText(true);
        l.setPadding(LEFT20);
        v.getChildren().add(l);

        l = new Label("Removing tags");
        l.setFont(BOLD_ITALIC);
        v.getChildren().add(l);
        l = new Label("To remove a tag, type a tag name into the tag editor field preceded by a dash (aka. Minus, or '-') and press enter.\nMultiple tags can be typed out (separated by spaces) before committing the changes.");
        l.setWrapText(true);
        l.setPadding(LEFT20);
        v.getChildren().add(l);

        l = new Label("Auto completion");
        l.setFont(BOLD_ITALIC);
        v.getChildren().add(l);
        l = new Label("When typing tags into the tag editor field, partial tag names can be auto-completed to existing tag names.\nPress TAB or CTRL+SPACE to complete the tag name with the first prediction.\nPress CTRL+ENTER to complete the tag name with the first prediction and then commit the edit.\nUse UP or DOWN arrow keys and SPACE or ENTER to choose other predictions.");
        l.setWrapText(true);
        l.setPadding(LEFT20);
        v.getChildren().add(l);

        ScrollPane sp = new ScrollPane(v);
        sp.setFitToWidth(true);
        sp.setPadding(ALL5);
        Tab t = new Tab("Editing Tags", sp);
        t.setClosable(false);
        return t;
    }

    private Tab constructSearchingTab() {

        VBox v = new VBox(5);

        Label l = new Label("Notes:\n    - Searching parses the input as a space-separated set of rules.\n    - A rule either has a prefix defining a special rule, or it is treated as a tag rule.\n ");
        l.setWrapText(true);
        v.getChildren().addAll(l, new Separator());

        l = new Label("Useful hotkeys:\n    Ctrl+F - Focuses the search field\n    Ctrl+Space or Tab - Autocomplete a tag name\n    Ctrl+Enter - Autocomplete a tag and search\n ");
        l.setWrapText(true);
        v.getChildren().addAll(l, new Separator());

        Label l1 = new Label("Excluding items");
        l1.setFont(BOLD_ITALIC);
        Label l2 = new Label("Prefix any search rule with a dash (a.k.a. minus, '-') to logically invert it.\n\nExamples:\n\"-tagme\" - excludes all items tagged with 'tagme'\n\"-type:group\" - excludes all items that are group items");
        l2.setWrapText(true);
        l2.setPadding(LEFT20);
        v.getChildren().addAll(l1, l2);

        l1 = new Label("Requiring tags");
        l1.setFont(BOLD_ITALIC);
        l2 = new Label("Use the exact name of a tag to require it.\n\nExamples:\n\"tagme\" - includes only items tagged with tagme\n\"test_tag_1 test_tag_2\" - includes only items tagged with test_tag_1 AND test_tag_2");
        l2.setWrapText(true);
        l2.setPadding(LEFT20);
        v.getChildren().addAll(l1, l2);

        l1 = new Label("Specifying types");
        l1.setFont(BOLD_ITALIC);
        l2 = new Label("Use the type modifier (type:) to include specific types of items.\n\nExamples:\n\"type:group\" - includes only group items\n\"type:media\" - includes only media items");
        l2.setWrapText(true);
        l2.setPadding(LEFT20);
        v.getChildren().addAll(l1, l2);

        l1 = new Label("Specifying IDs");
        l1.setFont(BOLD_ITALIC);
        l2 = new Label("Use the ID modifier (id:) to search for specific IDs or IDs in a range.\n\nExamples:\n\"id:>1234\" - includes only items whose ID is greater than 1234\n\"id:4321\" - includes only the item whose id is 4321");
        l2.setWrapText(true);
        l2.setPadding(LEFT20);
        v.getChildren().addAll(l1, l2);

        l1 = new Label("Search by time");
        l1.setFont(BOLD_ITALIC);
        l2 = new Label("Use the time modifier (time:) to search for a specific time that items were added.\n\nExamples:\n\"time:<1541361629900\" - includes only items that were added before the given time in milliseconds after epoch\n\"time:1541361629900\" - includes only items that were added at exactly the given time after epoch");
        l2.setWrapText(true);
        l2.setPadding(LEFT20);
        v.getChildren().addAll(l1, l2);

        l1 = new Label("Search by file path");
        l1.setFont(BOLD_ITALIC);
        l2 = new Label("Use the path modifier (path:) to search for file paths that contain the a string.\n\nExamples:\n\"path:C:/Users/PERSON/Documents\" - includes only files in PERSON's Documents folder");
        l2.setWrapText(true);
        l2.setPadding(LEFT20);
        v.getChildren().addAll(l1, l2);

        l1 = new Label("Missing attributes");
        l1.setFont(BOLD_ITALIC);
        l2 = new Label("Use the missing modifier (missing:) to search for items that missing certain attributes.\n\nExamples:\n\"missing:md5\" - only includes items that are missing an MD5 hash\n\"missing:file\" - includes only items that point to a non-existent file\n\"missing:histogram\" - includes only items that are missing a histogram");
        l2.setWrapText(true);
        l2.setPadding(LEFT20);
        v.getChildren().addAll(l1, l2);

        l = new Label("Search Rules:");
        l.setFont(BOLD_ITALIC);
        v.getChildren().addAll(new Separator(), l);

        String[] strs = new String[]{"LEGEND:", "  (___) = Optional ___", "  [___] = Required ___", "  | = Or", " ", "[TAG]", "-[RULE]", "id:(<|>)[INT]", "[time|date]:(<|>)[LONG]", "[path|file]:[STRING]", "missing:[md5|hist|histogram|file]", "type:[group|media]", "tags:(<|>)[INT]", "in:[INT]"};
        for (String str : strs) {
            l = new Label(str);
            l.setPadding(LEFT20);
            v.getChildren().add(l);
        }

        ScrollPane sp = new ScrollPane(v);
        sp.setFitToWidth(true);
        sp.setPadding(ALL5);
        Tab searching = new Tab("Searching", sp);
        searching.setClosable(false);
        return searching;
    }

    private Tab constructHotKeysTab() {
        VBox v1 = new VBox();
        VBox v2 = new VBox();

        Label l = new Label("Ctrl+H");
        l.setFont(BOLD_ITALIC);
        v1.getChildren().add(l);
        v2.getChildren().add(new Label("- Show this menu"));

        l = new Label("Ctrl+F");
        l.setFont(BOLD_ITALIC);
        v1.getChildren().add(l);
        v2.getChildren().add(new Label("- Focus search bar"));

        l = new Label("Ctrl+E");
        l.setFont(BOLD_ITALIC);
        v1.getChildren().add(l);
        v2.getChildren().add(new Label("- Focus tag editor"));

        l = new Label("Ctrl+Q");
        l.setFont(BOLD_ITALIC);
        v1.getChildren().add(l);
        v2.getChildren().add(new Label("- Quit"));

        l = new Label("Ctrl+S");
        l.setFont(BOLD_ITALIC);
        v1.getChildren().add(l);
        v2.getChildren().add(new Label("- Show settings"));

        l = new Label("Ctrl+T");
        l.setFont(BOLD_ITALIC);
        v1.getChildren().add(l);
        v2.getChildren().add(new Label("- Show tag list"));

        l = new Label("Ctrl+N");
        l.setFont(BOLD_ITALIC);
        v1.getChildren().add(l);
        v2.getChildren().add(new Label("- Show import notifications"));

        l = new Label("Ctrl+D");
        l.setFont(BOLD_ITALIC);
        v1.getChildren().add(l);
        v2.getChildren().add(new Label("- Find duplicates among currently selected items"));

        l = new Label("Ctrl+I");
        l.setFont(BOLD_ITALIC);
        v1.getChildren().add(l);
        v2.getChildren().add(new Label("- Show import dialog for files"));

        l = new Label("Ctrl+Shift+I");
        l.setFont(BOLD_ITALIC);
        v1.getChildren().add(l);
        v2.getChildren().add(new Label("- Show import dialog for folders (recursive)"));

        l = new Label("Del");
        l.setFont(BOLD_ITALIC);
        v1.getChildren().add(l);
        v2.getChildren().add(new Label("- Delete currently selected items"));

        l = new Label("Ctrl+Del");
        l.setFont(BOLD_ITALIC);
        v1.getChildren().add(l);
        v2.getChildren().add(new Label("- Forget currently selected items from database"));

        l = new Label("Ctrl+G");
        l.setFont(BOLD_ITALIC);
        v1.getChildren().add(l);
        v2.getChildren().add(new Label("- Group all selected items together"));

        l = new Label("Ctrl+U");
        l.setFont(BOLD_ITALIC);
        v1.getChildren().add(l);
        v2.getChildren().add(new Label("- Ungroup all selected groups"));

        HBox h = new HBox(5, v1, v2);
        ScrollPane sp = new ScrollPane(h);
        sp.setFitToWidth(true);
        sp.setPadding(ALL5);
        Tab hotkeys = new Tab("Hotkeys", sp);
        hotkeys.setClosable(false);
        return hotkeys;
    }

}
