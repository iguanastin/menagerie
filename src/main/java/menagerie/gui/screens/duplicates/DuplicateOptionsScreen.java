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

package menagerie.gui.screens.duplicates;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import menagerie.gui.Main;
import menagerie.gui.screens.Screen;
import menagerie.gui.screens.ScreenPane;
import menagerie.gui.screens.dialogs.AlertDialogScreen;
import menagerie.gui.screens.dialogs.ProgressScreen;
import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;
import menagerie.model.menagerie.Menagerie;
import menagerie.settings.MenagerieSettings;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class DuplicateOptionsScreen extends Screen {

    private static final double DEFAULT_CONFIDENCE = 0.95;
    private static final long PROGRESS_UPDATE_INTERVAL = 16;
    private long lastProgressUpdate = 0;

    private enum Scope {
        SELECTED, SEARCHED, ALL
    }

    private final MenagerieSettings settings;

    private final DuplicatesScreen duplicateScreen;

    private final Label compareCountLabel = new Label("~N/A comparisons"), firstCountLabel = new Label("0"), secondCountLabel = new Label("0");
    private final ChoiceBox<Scope> compareChoiceBox = new ChoiceBox<>(), toChoiceBox = new ChoiceBox<>();
    private final TextField confidenceTextField = new TextField();
    private final CheckBox includeGroupElementsCheckBox = new CheckBox("Include group elements");
    private final Button previousButton = new Button("Open last");

    private List<Item> selected = null, searched = null, all = null;
    private Menagerie menagerie = null;


    public DuplicateOptionsScreen(MenagerieSettings settings) {
        duplicateScreen = new DuplicatesScreen();

        this.settings = settings;

        addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                close();
            } else if (event.getCode() == KeyCode.ENTER) {
                compareButtonOnAction();
            }
        });


        Button exit = new Button("X");
        exit.setOnAction(event -> close());
        BorderPane header = new BorderPane(null, null, exit, null, new Label("Duplicate Settings"));
        header.setPadding(new Insets(0, 0, 0, 5));

        VBox contents = new VBox(5);
        contents.setPadding(new Insets(5));

        compareChoiceBox.getItems().addAll(Scope.SELECTED, Scope.SEARCHED, Scope.ALL);
        compareChoiceBox.getSelectionModel().selectFirst();
        Label l1 = new Label("Compare:");
        HBox h = new HBox(5, l1, compareChoiceBox, firstCountLabel);
        h.setAlignment(Pos.CENTER_LEFT);
        contents.getChildren().add(h);

        Label l2 = new Label("To:");
        l2.minWidthProperty().bind(l1.widthProperty());
        toChoiceBox.getItems().addAll(Scope.SELECTED, Scope.SEARCHED, Scope.ALL);
        toChoiceBox.getSelectionModel().selectFirst();
        toChoiceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> updateCounts());
        compareChoiceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            Scope toSelected = toChoiceBox.getValue();
            switch (newValue) {
                case SELECTED:
                    toChoiceBox.getItems().clear();
                    toChoiceBox.getItems().addAll(Scope.SELECTED, Scope.SEARCHED, Scope.ALL);
                    break;
                case SEARCHED:
                    toChoiceBox.getItems().clear();
                    toChoiceBox.getItems().addAll(Scope.SEARCHED, Scope.ALL);
                    break;
                case ALL:
                    toChoiceBox.getItems().clear();
                    toChoiceBox.getItems().addAll(Scope.ALL);
                    break;
            }
            if (toChoiceBox.getItems().contains(toSelected)) {
                toChoiceBox.getSelectionModel().select(toSelected);
            } else {
                toChoiceBox.getSelectionModel().selectFirst();
            }

            updateCounts();
        });
        h = new HBox(5, l2, toChoiceBox, secondCountLabel);
        h.setAlignment(Pos.CENTER_LEFT);
        contents.getChildren().add(h);

        contents.getChildren().add(includeGroupElementsCheckBox);

        confidenceTextField.setPromptText(MediaItem.MIN_CONFIDENCE + "-" + MediaItem.MAX_CONFIDENCE);
        confidenceTextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                try {
                    double value = Double.parseDouble(confidenceTextField.getText());
                    if (value < MediaItem.MIN_CONFIDENCE) confidenceTextField.setText("" + MediaItem.MIN_CONFIDENCE);
                    else if (value > MediaItem.MAX_CONFIDENCE)
                        confidenceTextField.setText("" + MediaItem.MAX_CONFIDENCE);
                } catch (NumberFormatException e) {
                    confidenceTextField.setText("" + DEFAULT_CONFIDENCE);
                }
            }
        });
        confidenceTextField.setTooltip(new Tooltip("Similarity confidence: (" + MediaItem.MIN_CONFIDENCE + "-" + MediaItem.MAX_CONFIDENCE + ")"));
        h = new HBox(5, new Label("Confidence:"), confidenceTextField);
        h.setAlignment(Pos.CENTER_LEFT);
        contents.getChildren().add(h);

        VBox center = new VBox(5, header, new Separator(), contents);

        Button compare = new Button("Compare");
        compare.setOnAction(event -> compareButtonOnAction());
        Button cancel = new Button("Cancel");
        cancel.setOnAction(event -> close());
        previousButton.setOnAction(event -> {
            if (duplicateScreen.getPairs() != null && !duplicateScreen.getPairs().isEmpty()) {
                duplicateScreen.openWithOldPairs(getManager(), menagerie);
                close();
            }
        });
        h = new HBox(5, compareCountLabel, compare, cancel);
        h.setAlignment(Pos.CENTER_RIGHT);
        BorderPane bottom = new BorderPane(null, null, h, null, previousButton);
        bottom.setPadding(new Insets(5));

        BorderPane root = new BorderPane(center, null, null, bottom, null);
        root.setPrefWidth(500);
        root.setStyle("-fx-background-color: -fx-base;");
        DropShadow effect = new DropShadow();
        effect.setSpread(0.5);
        root.setEffect(effect);
        root.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        setCenter(root);

        setDefaultFocusNode(compare);
    }

    /**
     * Opens this screen in a manager.
     *
     * @param manager   Manager to open in.
     * @param menagerie Menagerie.
     * @param selected  Set of items that are selected.
     * @param searched  Set of items that are searched.
     * @param all       Set of all items.
     */
    public void open(ScreenPane manager, Menagerie menagerie, List<Item> selected, List<Item> searched, List<Item> all) {
        if (manager == null || menagerie == null || selected == null || searched == null || all == null) return;
        this.menagerie = menagerie;
        this.selected = selected;
        this.searched = searched;
        this.all = all;

        manager.open(this);
    }

    /**
     * Updates the count labels.
     */
    private void updateCounts() {
        int firstNum;
        if (compareChoiceBox.getValue() == Scope.SELECTED) {
            firstNum = selected.size();
        } else if (compareChoiceBox.getValue() == Scope.SEARCHED) {
            firstNum = searched.size();
        } else {
            firstNum = all.size();
        }
        firstCountLabel.setText(firstNum + "");

        int secondNum;
        if (toChoiceBox.getValue() == Scope.SELECTED) {
            secondNum = selected.size();
        } else if (toChoiceBox.getValue() == Scope.SEARCHED) {
            secondNum = searched.size();
        } else {
            secondNum = all.size();
        }
        secondCountLabel.setText(secondNum + "");

        compareCountLabel.setText("~" + firstNum * secondNum + " comparisons");
    }

    /**
     * Saves the changed settings to the settings object and writes it to file.
     */
    private void saveSettings() {
        try {
            settings.duplicatesConfidence.setValue(Double.parseDouble(confidenceTextField.getText()));
            settings.duplicatesIncludeGroups.setValue(includeGroupElementsCheckBox.isSelected());
        } catch (NumberFormatException e) {
            Main.log.log(Level.WARNING, "Failed to convert DuplicateOptionsScreen confidenceTextField to double for saving settings", e);
        }

        try {
            settings.save(new File(Main.SETTINGS_PATH));
        } catch (IOException e) {
            Main.log.log(Level.SEVERE, "Failed to save settings file", e);
        }
    }

    private void compareButtonOnAction() {
        saveSettings();

        ProgressScreen ps = new ProgressScreen();

        List<Item> compare = getComparableItems(compareChoiceBox);
        List<Item> to = getComparableItems(toChoiceBox);

        Platform.runLater(() -> ps.setProgress(0));

        DuplicateManagerThread finder = new DuplicateManagerThread(menagerie, compare, to, settings.duplicatesConfidence.getValue(), progress -> Platform.runLater(() -> {
            long time = System.currentTimeMillis();
            if (time - getLastProgressUpdate() > PROGRESS_UPDATE_INTERVAL) {
                setLastProgressUpdate(time);
                Platform.runLater(() -> ps.setProgress(progress));
            }
        }), results -> Platform.runLater(() -> {
            ps.close();
            close();
            if (results.isEmpty()) {
                new AlertDialogScreen().open(getManager(), "No Duplicates", "No duplicates were found", null);
            } else {
                duplicateScreen.open(getManager(), menagerie, results);
            }
        }));

        ps.open(getManager(), "Finding similar items", "Comparing items...", () -> {
            finder.cancel();
            close();
        });

        finder.start();
    }

    private List<Item> getComparableItems(ChoiceBox<Scope> compareChoiceBox) {
        List<Item> compare = all;
        if (compareChoiceBox.getValue() == Scope.SELECTED) {
            compare = selected;
        } else if (compareChoiceBox.getValue() == Scope.SEARCHED) {
            compare = searched;
        }
        if (includeGroupElementsCheckBox.isSelected()) {
            compare = expandGroups(compare);
        } else {
            compare = new ArrayList<>(compare);
        }
        compare.removeIf(item -> !(item instanceof MediaItem) || ((MediaItem) item).hasNoSimilar());
        return compare;
    }

    @Override
    protected void onOpen() {
        updateCounts();

        confidenceTextField.setText(settings.duplicatesConfidence.getValue() + "");
        includeGroupElementsCheckBox.setSelected(settings.duplicatesIncludeGroups.getValue());
        previousButton.setDisable(duplicateScreen.getPairs() == null || duplicateScreen.getPairs().isEmpty());
    }

    private long getLastProgressUpdate() {
        return lastProgressUpdate;
    }

    private void setLastProgressUpdate(long lastProgressUpdate) {
        this.lastProgressUpdate = lastProgressUpdate;
    }

    /**
     * @return The duplicate resolver screen associated with this screen.
     */
    public DuplicatesScreen getDuplicatesScreen() {
        return duplicateScreen;
    }

    /**
     * Expands groups so that items are only of type MediaItem.
     *
     * @param items Items with potential group items to expand.
     * @return Set of items containing all MediaItems and elements of expanded groups.
     */
    private List<Item> expandGroups(List<Item> items) {
        items = new ArrayList<>(items);
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) instanceof GroupItem) {
                GroupItem group = (GroupItem) items.remove(i);
                items.addAll(i, group.getElements());
            }
        }
        return items;
    }

    /**
     * Releases all VLCJ resources.
     */
    public void releaseVLCJ() {
        duplicateScreen.releaseVLCJ();
    }

}
