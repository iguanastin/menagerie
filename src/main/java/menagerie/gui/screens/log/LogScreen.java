package menagerie.gui.screens.log;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import menagerie.gui.screens.Screen;

public class LogScreen extends Screen {

    private final ListView<String> listView;


    public LogScreen() {
        addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                close();
                event.consume();
            } else if (event.getCode() == KeyCode.L && event.isControlDown()) {
                close();
                event.consume();
            }
        });


        Button exit = new Button("X");
        exit.setOnAction(event -> close());
        Label title = new Label("Console log:");
        setAlignment(title, Pos.CENTER_LEFT);
        BorderPane top = new BorderPane(null, null, exit, null, title);
        top.setPadding(new Insets(0, 0, 0, 5));

        listView = new ListView<>();
        listView.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                close();
                event.consume();
            } else if (event.getCode() == KeyCode.L && event.isControlDown()) {
                close();
                event.consume();
            }
        });

        BorderPane root = new BorderPane(listView, top, null, null, null);
        root.setPrefWidth(800);
        root.setStyle("-fx-background-color: -fx-base;");
        DropShadow effect = new DropShadow();
        effect.setSpread(0.5);
        root.setEffect(effect);
        root.setMaxWidth(USE_PREF_SIZE);
        setRight(root);
        setPadding(new Insets(25));

        setDefaultFocusNode(exit);
    }

    public ListView<String> getListView() {
        return listView;
    }

    @Override
    protected void onOpen() {
        listView.scrollTo(listView.getItems().size() - 1);
    }

}
