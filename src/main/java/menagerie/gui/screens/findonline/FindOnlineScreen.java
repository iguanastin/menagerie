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

package menagerie.gui.screens.findonline;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import menagerie.duplicates.DuplicateFinder;
import menagerie.duplicates.Match;
import menagerie.gui.Main;
import menagerie.gui.Thumbnail;
import menagerie.gui.grid.ItemGridCell;
import menagerie.gui.media.DynamicImageView;
import menagerie.gui.screens.Screen;
import menagerie.gui.screens.ScreenPane;
import menagerie.gui.screens.dialogs.AlertDialogScreen;
import menagerie.model.menagerie.MediaItem;
import menagerie.model.menagerie.Tag;
import menagerie.util.Util;
import menagerie.util.listeners.ObjectListener;
import org.controlsfx.control.GridView;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;

public class FindOnlineScreen extends Screen {

    private final static Insets ALL5 = new Insets(5);

    private final GridView<Match> matchGridView = new GridView<>();
    private final DynamicImageView currentItemView = new DynamicImageView();
    private final Label yourImageInfoLabel = new Label();
    private final Button prevButton = new Button("Previous");
    private final Label indexLabel = new Label("/0");
    private final TextField indexTextField = new TextField("0");
    private final Button nextButton = new Button("Next");
    private final ListView<String> tagListView = new ListView<>();

    private final VBox failedVBox = new VBox(5, new Label("Failed to get some or all results!"));
    private final ProgressIndicator loadingIndicator = new ProgressIndicator();
    private StackPane matchesStackPane = null;


    private final List<MatchGroup> matches = Collections.synchronizedList(new ArrayList<>());
    private final Map<MediaItem, MatchGroup> matchMap = Collections.synchronizedMap(new HashMap<>());
    private List<DuplicateFinder> finders = null;
    private final ObjectProperty<MatchGroup> currentMatch = new SimpleObjectProperty<>();

    private Thread searcherThread = null;

    private ObjectListener<MediaItem> selectListener = null;

    private final CompareToOnlineScreen compareScreen = new CompareToOnlineScreen();


    public FindOnlineScreen() {
        addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                close();
            } else if (event.getCode() == KeyCode.LEFT) {
                displayPrevious();
            } else if (event.getCode() == KeyCode.RIGHT) {
                displayNext();
            }
        });

        setPadding(new Insets(25));
        BorderPane root = new BorderPane();
        root.getStyleClass().addAll(ROOT_STYLE_CLASS);
        setCenter(root);

        VBox v = new VBox(5, initYourItemHBox(), new Separator(), initMatchesStackPane());
        v.setPadding(ALL5);

        root.setTop(initHeader());
        root.setCenter(v);
        root.setBottom(initBottom());

        compareScreen.addSuccessListener(() -> displayMatch(currentMatch.get()));

        setDefaultFocusNode(root);

        // Init searcher thread
        initSearcherThread();
    }

    private HBox initBottom() {
        prevButton.setOnAction(event -> displayPrevious());
        nextButton.setOnAction(event -> displayNext());
        indexTextField.setPrefWidth(50);
        indexTextField.setAlignment(Pos.CENTER_RIGHT);
        indexTextField.setOnAction(event -> {
            int i = matches.indexOf(getCurrentMatch());
            try {
                int temp = Integer.parseInt(indexTextField.getText()) - 1;
                i = Math.max(0, Math.min(temp, matches.size() - 1)); // Clamp to valid indices
            } catch (NumberFormatException e) {
                // Nothing
            }

            displayMatch(matches.get(i));
            requestFocus();
        });
        HBox bottom = new HBox(5, prevButton, indexTextField, indexLabel, nextButton);
        bottom.setAlignment(Pos.CENTER);
        bottom.setPadding(ALL5);
        return bottom;
    }

    private BorderPane initHeader() {
        BorderPane header = new BorderPane();

        Label title = new Label("Find duplicates online");
        header.setLeft(title);
        BorderPane.setMargin(title, ALL5);

        Button exit = new Button("X");
        header.setRight(exit);
        exit.setOnAction(event -> close());

        header.setBottom(new Separator());
        return header;
    }

    private StackPane initMatchesStackPane() {
        matchGridView.setCellFactory(param -> {
            MatchGridCell c = new MatchGridCell();
            c.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY) {
                    compareScreen.open(getManager(), currentMatch.get().getItem(), c.getItem());
                    event.consume();
                }
            });
            c.setOnContextMenuRequested(event -> {
                MenuItem open = new MenuItem("Go to URL");
                open.setOnAction(event1 -> {
                    try {
                        Desktop.getDesktop().browse(new URI(c.getItem().getPageURL()));
                    } catch (IOException | URISyntaxException e) {
                        Main.log.log(Level.SEVERE, "Failed trying to open url in default browser: " + c.getItem().getPageURL(), e);
                    }
                });

                ContextMenu cm = new ContextMenu(open);
                cm.show(c, event.getScreenX(), event.getScreenY());
            });
            return c;
        });
        matchGridView.setCellWidth(156);
        matchGridView.setCellHeight(156);
        matchGridView.setHorizontalCellSpacing(3);
        matchGridView.setVerticalCellSpacing(3);
        BorderPane matchesBorderPane = new BorderPane(matchGridView, new Label("Found matches online:"), null, null, null);
        HBox.setHgrow(matchesBorderPane, Priority.ALWAYS);
        matchesStackPane = new StackPane(new HBox(5, matchesBorderPane, initTagListVBox()));
        VBox.setVgrow(matchesStackPane, Priority.ALWAYS);

        loadingIndicator.setMaxSize(100, 100);
        StackPane.setAlignment(loadingIndicator, Pos.CENTER);
        initFailedVBox();

        return matchesStackPane;
    }

    private VBox initFailedVBox() {
        Button retryButton = new Button("Retry");
        retryButton.setOnAction(event -> {
            getCurrentMatch().setStatus(MatchGroup.Status.WAITING);
            searcherThread.interrupt();
            matchesStackPane.getChildren().add(loadingIndicator);
            matchesStackPane.getChildren().remove(failedVBox);
        });
        Button closeButton = new Button("Close");
        closeButton.setOnAction(event -> matchesStackPane.getChildren().remove(failedVBox));
        HBox failedButtonHBox = new HBox(5, retryButton, closeButton);
        failedButtonHBox.setAlignment(Pos.CENTER_RIGHT);
        failedVBox.getChildren().add(failedButtonHBox);
        failedVBox.setMaxSize(300, 100);
        failedVBox.getStyleClass().addAll(ROOT_STYLE_CLASS);
        return failedVBox;
    }

    private HBox initYourItemHBox() {
        yourImageInfoLabel.setWrapText(true);
        BorderPane currentItemBP = new BorderPane(currentItemView);
        currentItemBP.getStyleClass().addAll(ItemGridCell.DEFAULT_STYLE_CLASS);
        currentItemBP.setMaxSize(156, 156);
        currentItemBP.setMinSize(156, 156);
        currentItemBP.setOnMouseClicked(event -> {
            if (selectListener != null) {
                close();
                selectListener.pass(currentMatch.get().getItem());
            }
        });
        HBox yourHBox = new HBox(10, new Label("Your image:"), currentItemBP, yourImageInfoLabel);
        yourHBox.setPadding(ALL5);
        yourHBox.setAlignment(Pos.CENTER);
        return yourHBox;
    }

    private VBox initTagListVBox() {
        tagListView.setFocusTraversable(false);
        tagListView.setCellFactory(param -> {
            OnlineTagListCell c = new OnlineTagListCell(tagNeme -> {
                for (Tag t : currentMatch.get().getItem().getTags()) {
                    if (t.getName().equalsIgnoreCase(tagNeme)) return true;
                }
                return false;
            });
            c.setOnMouseClicked(event -> {
                if (c.getItem() != null) {
                    Tag t = currentMatch.get().getItem().getMenagerie().getTagByName(c.getItem());
                    if (t == null) {
                        t = currentMatch.get().getItem().getMenagerie().createTag(c.getItem());
                    }
                    currentMatch.get().getItem().addTag(t);
                    c.sharesTagProperty().set(true);
                    event.consume();
                }
            });

            return c;
        });
        VBox tagListVBox = new VBox(5, new Label("Found Tags (Click to add)"), tagListView);
        VBox.setVgrow(tagListView, Priority.ALWAYS);
        tagListVBox.setMaxWidth(200);
        tagListVBox.setMinWidth(200);
        return tagListVBox;
    }

    private void initSearcherThread() {
        searcherThread = new Thread(() -> {
            while (true) {
                MatchGroup item = getCurrentMatch();
                if (item == null) {
                    blockUntilItemChanges();
                } else {
                    if (item.getStatus() == MatchGroup.Status.WAITING) {
                        item.retrieveMatches(finders);
                        Platform.runLater(() -> {
                            if (getCurrentMatch().equals(item)) displayMatches(item);
                        });
                    } else {
                        MatchGroup next = null;

                        synchronized (matches) {
                            int i = matches.indexOf(item);
                            if (i + 1 < matches.size()) {
                                next = matches.get(i + 1);
                            }
                        }

                        if (next != null && next.getStatus() == MatchGroup.Status.WAITING) {
                            next.retrieveMatches(finders);
                            MatchGroup finalNext = next;
                            Platform.runLater(() -> {
                                if (getCurrentMatch().equals(finalNext)) displayMatches(finalNext);
                            });
                        } else {
                            blockUntilItemChanges();
                        }
                    }
                }
            }
        }, "FindOnline Searcher");
        searcherThread.setDaemon(true);
        searcherThread.start();
    }

    private void blockUntilItemChanges() {
        CountDownLatch cdl = new CountDownLatch(1);
        ChangeListener<MatchGroup> changeListener = (observable, oldValue, newValue) -> {
            cdl.countDown();
        };
        synchronized (currentMatch) {
            currentMatch.addListener(changeListener);
        }
        try {
            cdl.await();
        } catch (InterruptedException ignore) {
        }
        synchronized (currentMatch) {
            currentMatch.removeListener(changeListener);
        }
    }

    public void open(ScreenPane manager, List<MediaItem> items, List<DuplicateFinder> finders, ObjectListener<MediaItem> selectListener) {
        if (items == null || finders == null) {
            throw new NullPointerException("Must not be null");
        } else if (items.isEmpty()) {
            AlertDialogScreen a = new AlertDialogScreen();
            a.open(manager, "No items", "No valid items selected", null);
            return;
        } else if (finders.isEmpty()) {
            AlertDialogScreen a = new AlertDialogScreen();
            a.open(manager, "No finders", "No compatible plugins loaded, cannot search online", null);
            return;
        }

        matches.clear();
        for (MediaItem item : items) {
            MatchGroup match = matchMap.get(item);
            if (match == null) {
                match = new MatchGroup(item);
                matchMap.put(item, match);
            }
            matches.add(match);
        }

        this.finders = finders;
        this.selectListener = selectListener;

        displayMatch(matches.get(0));

        manager.open(this);
    }

    private void displayPrevious() {
        if (prevButton.isDisabled()) return;

        int i = matches.indexOf(currentMatch.get());
        if (i > 0) {
            displayMatch(matches.get(i - 1));
        }
    }

    private void displayNext() {
        if (nextButton.isDisabled()) return;

        int i = matches.indexOf(currentMatch.get());
        if (i + 1 < matches.size()) {
            displayMatch(matches.get(i + 1));
        }
    }

    private void displayMatch(MatchGroup item) {
        synchronized (currentMatch) {
            if (currentMatch.get() != null && !currentMatch.get().getItem().getThumbnail().isLoaded()) {
                currentMatch.get().getItem().getThumbnail().doNotWant();
            }
            currentMatch.set(item);
        }
        matchGridView.getItems().clear();
        yourImageInfoLabel.setText("N/A");
        tagListView.getItems().clear();

        final int i = matches.indexOf(item);
        nextButton.setDisable(i + 1 >= matches.size());
        prevButton.setDisable(i == 0);
        indexTextField.setText((i + 1) + "");
        indexLabel.setText("/" + matches.size());

        if (item != null) {
            setThumbnail(item.getItem());
            setFileInfo(item.getItem());

            displayMatches(item);

            if (item.getStatus() == MatchGroup.Status.WAITING || item.getStatus() == MatchGroup.Status.PROCESSING) {
                matchesStackPane.getChildren().add(loadingIndicator);
            } else {
                matchesStackPane.getChildren().remove(loadingIndicator);
            }
            if (item.getStatus() == MatchGroup.Status.FAILED) {
                matchesStackPane.getChildren().add(failedVBox);
            } else {
                matchesStackPane.getChildren().remove(failedVBox);
            }
        }
    }

    private void setThumbnail(MediaItem item) {
        Thumbnail thumb = item.getThumbnail();
        thumb.want();
        if (thumb.isLoaded()) {
            currentItemView.setImage(thumb.getImage());
        } else {
            thumb.addImageReadyListener(thing -> Platform.runLater(() -> currentItemView.setImage(thing)));
        }
    }

    private void setFileInfo(MediaItem item) {
        Image img = item.getImage();
        if (!img.isBackgroundLoading() || img.getProgress() == 1) {
            setFileInfoLabelDirect(item);
        } else {
            img.progressProperty().addListener((observable, oldValue, newValue) -> {
                if (!img.isError() && newValue.doubleValue() == 1 && currentMatch.get().getItem().equals(item)) {
                    setFileInfoLabelDirect(item);
                }
            });
        }
    }

    private void setFileInfoLabelDirect(MediaItem item) {
        yourImageInfoLabel.setText(item.getFile() + "\n" + (int) item.getImage().getWidth() + "x" + (int) item.getImage().getHeight() + "\n" + Util.bytesToPrettyString(item.getFile().length()));
    }

    private void displayMatches(MatchGroup item) {
        matchGridView.getItems().clear();
        matchGridView.getItems().addAll(item.getMatches());
        matchesStackPane.getChildren().remove(loadingIndicator);
        tagListView.getItems().clear();
        item.getMatches().forEach(match -> match.getTags().forEach(s -> {
            if (!tagListView.getItems().contains(s)) tagListView.getItems().add(s);
        }));
        tagListView.getItems().sort(String::compareTo);

        if (item.getStatus() == MatchGroup.Status.FAILED) {
            matchesStackPane.getChildren().add(failedVBox);
        }
    }

    public CompareToOnlineScreen getCompareScreen() {
        return compareScreen;
    }

    public ObjectProperty<MatchGroup> currentMatchProperty() {
        return currentMatch;
    }

    private MatchGroup getCurrentMatch() {
        synchronized (currentMatch) {
            return currentMatch.get();
        }
    }

    private void setCurrentMatch(MatchGroup match) {
        synchronized (currentMatch) {
            currentMatch.set(match);
        }
    }

}
