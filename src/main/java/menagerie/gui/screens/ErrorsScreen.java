package menagerie.gui.screens;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Separator;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import menagerie.gui.errors.ErrorListCell;
import menagerie.gui.errors.TrackedError;

import java.awt.*;

public class ErrorsScreen extends Screen {

    private final ListView<TrackedError> errorsList;


    public ErrorsScreen() {
        addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                close();
            }
        });

        Button x = new Button("X");
        x.setOnAction(event -> close());
        BorderPane header = new BorderPane(null, null, x, null, new Label("Errors and Warnings:"));

        errorsList = new ListView<>();
        VBox.setVgrow(errorsList, Priority.ALWAYS);
        errorsList.setCellFactory(param -> new ErrorListCell(error -> errorsList.getItems().remove(error)));

        Button dismiss = new Button("Dismiss All");
        dismiss.setOnAction(event -> {
            errorsList.getItems().clear();
            close();
        });
        HBox h = new HBox(dismiss);
        h.setAlignment(Pos.CENTER_RIGHT);

        VBox v = new VBox(5, header, new Separator(), errorsList, h);
        v.setStyle("-fx-background-color: -fx-base;");
        v.setPrefWidth(600);

        DropShadow effect = new DropShadow();
        effect.setSpread(0.5);
        v.setEffect(effect);

        setRight(v);
        setPadding(new Insets(25));
    }

    public void addError(TrackedError error) {
        errorsList.getItems().add(0, error);
        Toolkit.getDefaultToolkit().beep();
    }

    public ObservableList<TrackedError> getErrors() {
        return errorsList.getItems();
    }

}
