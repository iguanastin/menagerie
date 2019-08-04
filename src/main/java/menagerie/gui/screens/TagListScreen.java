/*
 MIT License

 Copyright (c) 2019. Austin Thompson

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */

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
import menagerie.gui.taglist.TagListCell;
import menagerie.model.menagerie.Tag;

import java.util.Comparator;
import java.util.List;
import java.util.regex.PatternSyntaxException;

public class TagListScreen extends Screen {

    private final ListView<Tag> listView = new ListView<>();
    private final TextField searchField = new TextField();
    private final ChoiceBox<String> orderBox = new ChoiceBox<>();
    private final ToggleButton descendingButton = new ToggleButton("Descending");
    private final CheckBox regexCheckBox = new CheckBox("Regex");

    private final ObservableList<Tag> tags = FXCollections.observableArrayList();


    public TagListScreen() {
        addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                close();
            }
        });

        //Init "Window"
        VBox v = new VBox(5);
        v.setPrefWidth(400);
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

        orderBox.getItems().addAll("Frequency", "Name", "ID", "Color");
        orderBox.getSelectionModel().clearAndSelect(0);
        orderBox.setOnAction(event -> updateListOrder());
        descendingButton.selectedProperty().addListener((observable, oldValue, newValue) -> updateListOrder());
        HBox orderHBox = new HBox(5, new Label("Order by:"), orderBox, descendingButton);
        orderHBox.setAlignment(Pos.CENTER_LEFT);

        //Init listView
        VBox.setVgrow(listView, Priority.ALWAYS);
        setCellFactory(param -> new TagListCell());
        listView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                close();
            }
        });

        //Init textfield
        searchField.setPromptText("Search tags by name");
        searchField.textProperty().addListener((observable, oldValue, newValue) -> updateSearchResults());
        regexCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> updateSearchResults());
        HBox searchHBox = new HBox(5, regexCheckBox, searchField);
        searchHBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        //Add children
        v.getChildren().addAll(header, new Separator(), orderHBox, searchHBox, listView);


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

    private void updateSearchResults() {
        listView.getItems().clear();
        for (Tag t : tags) {
            String name = t.getName().toLowerCase();
            String val = searchField.getText();
            if (val == null) {
                val = "";
            } else {
                val = val.trim().toLowerCase();
            }
            if (regexCheckBox.isSelected()) {
                try {
                    if (name.matches(val)) listView.getItems().add(t);
                } catch (PatternSyntaxException ignore) {
                }
            } else {
                if (name.contains(val)) listView.getItems().add(t);
            }
        }
        updateListOrder();
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

        updateSearchResults();
    }

    private void updateListOrder() {
        Comparator<Tag> comparator = Comparator.comparing(Tag::getName);

        switch (orderBox.getValue()) {
            case "ID":
                comparator = Comparator.comparingInt(Tag::getId);
                break;
            case "Frequency":
                comparator = Comparator.comparingInt(Tag::getFrequency);
                break;
            case "Color":
                comparator = (o1, o2) -> {
                    if (o1.getColor() == null) {
                        if (o2.getColor() == null) {
                            return 0;
                        } else {
                            return 1;
                        }
                    } else {
                        if (o2.getColor() == null) {
                            return -1;
                        } else {
                            return o1.getColor().compareTo(o2.getColor());
                        }
                    }
                };
                break;
        }

        if (descendingButton.isSelected()) comparator = comparator.reversed();
        listView.getItems().sort(comparator);
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
