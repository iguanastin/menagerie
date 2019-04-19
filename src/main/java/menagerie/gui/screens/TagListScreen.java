package menagerie.gui.screens;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import menagerie.gui.TagListCell;
import menagerie.model.menagerie.Tag;

import java.util.Comparator;
import java.util.List;

public class TagListScreen extends Screen {

    private final ListView<Tag> listView;
    private final TextField searchField;
    private final ChoiceBox<String> orderBox;

    private final ObservableList<Tag> tags = FXCollections.observableArrayList();


    public TagListScreen() {
        addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                close();
            }
        });

        //Init "Window"
        VBox v = new VBox(5);
        v.setPrefWidth(300);
        v.setMaxWidth(USE_PREF_SIZE);
        v.setStyle("-fx-background-color: -fx-base;");
        v.setPadding(new Insets(5));
        DropShadow dropShadow = new DropShadow();
        dropShadow.setSpread(0.5);
        v.setEffect(dropShadow);
        setCenter(v);
        setMargin(v, new Insets(25));

        //Init header
        Button exitButton = new Button("X");
        exitButton.setOnAction(event -> close());
        BorderPane header = new BorderPane(null, null, exitButton, null, new Label("Tags"));

        orderBox = new ChoiceBox<>();
        orderBox.getItems().addAll("Name", "ID", "Frequency");
        orderBox.getSelectionModel().clearAndSelect(0);
        orderBox.setOnAction(event -> updateListOrder());
        HBox h = new HBox(5, new Label("Order by:"), orderBox);
        h.setAlignment(Pos.CENTER_LEFT);

        //Init listView
        listView = new ListView<>();
        VBox.setVgrow(listView, Priority.ALWAYS);
        setCellFactory(param -> new TagListCell());
        listView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                close();
            }
        });

        //Init textfield
        searchField = new TextField();
        searchField.setPromptText("Search tags by name");
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            listView.getItems().clear();
            for (Tag t : tags) {
                if (t.getName().toLowerCase().startsWith(newValue.toLowerCase())) {
                    listView.getItems().add(t);
                }
            }
            updateListOrder();
        });

        //Add children
        v.getChildren().addAll(header, new Separator(), h, searchField, listView);


        tags.addListener((ListChangeListener<? super Tag>) c -> {
            while (c.next()) {
                listView.getItems().removeAll(c.getRemoved());
                for (Tag t : c.getAddedSubList()) {
                    if (!listView.getItems().contains(t) && t.getName().toLowerCase().startsWith(searchField.getText().toLowerCase())) {
                        listView.getItems().add(t);
                    }
                }
                updateListOrder();
            }
        });

        setDefaultFocusNode(searchField);
    }

    /**
     * Opens the screen in a manager with a set of tags.
     *
     * @param manager Manager.
     * @param tags    Tags to display.
     */
    public void open(ScreenPane manager, List<Tag> tags) {
        manager.open(this);

        this.tags.clear();
        this.tags.addAll(tags);
    }

    private void updateListOrder() {
        switch (orderBox.getValue()) {
            case "ID":
                listView.getItems().sort(Comparator.comparingInt(Tag::getId));
                break;
            case "Frequency":
                listView.getItems().sort(Comparator.comparingInt(Tag::getFrequency).reversed());
                break;
            case "Name":
                listView.getItems().sort(Comparator.comparing(Tag::getName));
                break;
            default:
                listView.getItems().sort(Comparator.comparing(Tag::getName));
                break;
        }
    }

    /**
     * @return The tags that are currently being displayed.
     */
    public ObservableList<Tag> getTags() {
        return tags;
    }

    /**
     * @param value Cell factory for the list view.
     */
    public void setCellFactory(Callback<ListView<Tag>, ListCell<Tag>> value) {
        listView.setCellFactory(value);
    }

}
