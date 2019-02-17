package menagerie.gui.screens;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import menagerie.gui.TagListCell;
import menagerie.model.menagerie.Tag;

import java.util.Comparator;

public class TagListScreen extends Screen {

    private ListView<Tag> listView;
    private TextField searchField;
    private ChoiceBox<String> orderBox;

    private ObservableList<Tag> tags = FXCollections.observableArrayList();


    public TagListScreen(Node onShowDisable) {
        super(onShowDisable);

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

        setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                hide();
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
        BorderPane header = new BorderPane();
        orderBox = new ChoiceBox<>();
        orderBox.getItems().addAll("Name", "ID", "Frequency");
        orderBox.getSelectionModel().clearAndSelect(0);
        orderBox.setOnAction(event -> updateListOrder());
        header.setCenter(new HBox(5, new Label("Order by:"), new BorderPane(orderBox)));
        Button exitButton = new Button("X");
        exitButton.setOnAction(event -> hide());
        header.setRight(exitButton);
        setAlignment(exitButton, Pos.CENTER);

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
        });

        //Init listView
        listView = new ListView<>();
        VBox.setVgrow(listView, Priority.ALWAYS);
        setCellFactory(param -> new TagListCell());

        //Add children
        v.getChildren().addAll(header, searchField, listView);

        onShowFocus = v;
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
        }
    }

    public ObservableList<Tag> getTags() {
        return tags;
    }

    public void setCellFactory(Callback<ListView<Tag>, ListCell<Tag>> value) {
        listView.setCellFactory(value);
    }

}
