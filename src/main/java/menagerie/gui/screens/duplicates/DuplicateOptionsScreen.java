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
import menagerie.gui.screens.ProgressScreen;
import menagerie.gui.screens.Screen;
import menagerie.gui.screens.ScreenPane;
import menagerie.model.Settings;
import menagerie.model.SimilarPair;
import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;
import menagerie.model.menagerie.Menagerie;
import menagerie.util.CancellableThread;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class DuplicateOptionsScreen extends Screen {

    private enum Scope {
        SELECTED,
        SEARCHED,
        ALL
    }

    private final Settings settings;

    private final DuplicatesScreen duplicateScreen;

    private final Label compareCountLabel, firstCountLabel, secondCoundLabel;
    private final ChoiceBox<Scope> compareChoiceBox, toChoiceBox;
    private final CheckBox compareGreyscaleCheckBox;
    private final TextField confidenceTextField;

    private List<Item> selected = null, searched = null, all = null;
    private Menagerie menagerie = null;


    public DuplicateOptionsScreen(Settings settings) {
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

        compareChoiceBox = new ChoiceBox<>();
        compareChoiceBox.getItems().addAll(Scope.SELECTED, Scope.SEARCHED, Scope.ALL);
        compareChoiceBox.getSelectionModel().selectFirst();
        Label l1 = new Label("Compare:");
        firstCountLabel = new Label("0");
        HBox h = new HBox(5, l1, compareChoiceBox, firstCountLabel);
        h.setAlignment(Pos.CENTER_LEFT);
        contents.getChildren().add(h);

        Label l2 = new Label("To:");
        l2.minWidthProperty().bind(l1.widthProperty());
        toChoiceBox = new ChoiceBox<>();
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
        secondCoundLabel = new Label("0");
        h = new HBox(5, l2, toChoiceBox, secondCoundLabel);
        h.setAlignment(Pos.CENTER_LEFT);
        contents.getChildren().add(h);

        compareGreyscaleCheckBox = new CheckBox("Compare greyscale images");
        compareGreyscaleCheckBox.setTooltip(new Tooltip("Comparing greyscale images is wildly inaccurate"));
        contents.getChildren().add(compareGreyscaleCheckBox);

        confidenceTextField = new TextField();
        confidenceTextField.setPromptText("0.8-1.0");
        confidenceTextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                try {
                    double value = Double.parseDouble(confidenceTextField.getText());
                    if (value < 0.8) confidenceTextField.setText("0.8");
                    else if (value > 1) confidenceTextField.setText("1");
                } catch (NumberFormatException e) {
                    confidenceTextField.setText("0.95");
                }
            }
        });
        confidenceTextField.setTooltip(new Tooltip("Similarity confidence: (0.8-1.0)"));
        h = new HBox(5, new Label("Confidence:"), confidenceTextField);
        h.setAlignment(Pos.CENTER_LEFT);
        contents.getChildren().add(h);

        VBox center = new VBox(5, header, new Separator(), contents);

        compareCountLabel = new Label("~N/A comparisons");
        Button compare = new Button("Compare");
        compare.setOnAction(event -> compareButtonOnAction());
        Button cancel = new Button("Cancel");
        cancel.setOnAction(event -> close());
        HBox bottom = new HBox(5, compareCountLabel, compare, cancel);
        bottom.setPadding(new Insets(5));
        bottom.setAlignment(Pos.CENTER_RIGHT);

        BorderPane root = new BorderPane(center, null, null, bottom, null);
        root.setPrefWidth(500);
        root.setStyle("-fx-background-color: -fx-base;");
        DropShadow effect = new DropShadow();
        effect.setSpread(0.5);
        root.setEffect(effect);
        root.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        setCenter(root);

        setDefaultFocusNode(compare);

        duplicateScreen = new DuplicatesScreen();
    }

    public void open(ScreenPane manager, Menagerie menagerie, List<Item> selected, List<Item> searched, List<Item> all) {
        if (manager == null || menagerie == null || selected == null || searched == null || all == null) return;
        this.menagerie = menagerie;
        this.selected = expandGroups(selected);
        this.searched = expandGroups(searched);
        this.all = expandGroups(all);

        manager.open(this);
    }

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
        secondCoundLabel.setText(secondNum + "");

        compareCountLabel.setText("~" + firstNum * secondNum + " comparisons");
    }

    private void saveSettings() {
        settings.setBoolean(Settings.Key.COMPARE_GREYSCALE, compareGreyscaleCheckBox.isSelected());
        try {
            settings.setDouble(Settings.Key.CONFIDENCE, Double.parseDouble(confidenceTextField.getText()));
        } catch (NumberFormatException e) {
            Main.log.log(Level.WARNING, "Failed to convert DuplicateOptionsScreen confidenceTextField to double for saving settings", e);
        }

        try {
            settings.save();
        } catch (FileNotFoundException e) {
            Main.log.log(Level.SEVERE, "Failed to save settings", e);
        }
    }

    private void compareButtonOnAction() {
        saveSettings();

        final List<SimilarPair> pairs = new ArrayList<>();
        final double confidence = settings.getDouble(Settings.Key.CONFIDENCE);
        final boolean compareGreyscale = settings.getBoolean(Settings.Key.COMPARE_GREYSCALE);
        ProgressScreen ps = new ProgressScreen();
        CancellableThread ct = new CancellableThread() {
            @Override
            public void run() {
                //Find lists to compare
                List<Item> compare = all;
                if (compareChoiceBox.getValue() == Scope.SELECTED) {
                    compare = selected;
                } else if (compareChoiceBox.getValue() == Scope.SEARCHED) {
                    compare = searched;
                }
                List<Item> to = all;
                if (toChoiceBox.getValue() == Scope.SELECTED) {
                    to = selected;
                } else if (toChoiceBox.getValue() == Scope.SEARCHED) {
                    to = searched;
                }
                to = new ArrayList<>(to);

                final int ffs = compare.size();
                Platform.runLater(() -> ps.setProgress(0, ffs));

                //Find duplicates
                int i = 0;
                for (Item i1 : compare) {
                    if (!running) {
                        Platform.runLater(ps::close);
                        return;
                    }

                    if (!(i1 instanceof MediaItem)) continue;

                    // Ensures no comparing to self
                    to.remove(i1);

                    // Find duplicates of i1
                    for (Item i2 : to) {
                        if (!(i2 instanceof MediaItem)) continue;

                        final double similarity = ((MediaItem) i1).getSimilarityTo((MediaItem) i2, compareGreyscale);
                        if (similarity >= confidence) {
                            pairs.add(new SimilarPair((MediaItem) i1, (MediaItem) i2, similarity));
                        }
                    }

                    // Update GUI
                    final int finalI = i;
                    final int finalTotal = compare.size();
                    Platform.runLater(() -> ps.setProgress(finalI, finalTotal));

                    // Increment counter
                    i++;
                }

                Platform.runLater(() -> {
                    duplicateScreen.open(getManager(), menagerie, pairs);
                    ps.close();
                    close();
                });
            }
        };
        ps.open(getManager(), "Finding similar items", "Comparing items...", () -> {
            ct.cancel();
            close();
        });
        ct.start();
    }

    @Override
    protected void onOpen() {
        updateCounts();

        compareGreyscaleCheckBox.setSelected(settings.getBoolean(Settings.Key.COMPARE_GREYSCALE));
        confidenceTextField.setText(settings.getDouble(Settings.Key.CONFIDENCE) + "");
    }

    public DuplicatesScreen getDuplicatesScreen() {
        return duplicateScreen;
    }

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

}