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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import menagerie.duplicates.DuplicateFinder;
import menagerie.duplicates.Match;
import menagerie.gui.Main;
import menagerie.gui.Thumbnail;
import menagerie.gui.media.DynamicImageView;
import menagerie.gui.screens.Screen;
import menagerie.gui.screens.ScreenPane;
import menagerie.gui.screens.dialogs.AlertDialogScreen;
import menagerie.model.menagerie.MediaItem;
import menagerie.model.menagerie.Tag;
import menagerie.util.Util;
import org.controlsfx.control.GridView;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class FindOnlineScreen extends Screen {

    private final static Insets ALL5 = new Insets(5);

    private final GridView<Match> matchGridView = new GridView<>();
    private final DynamicImageView currentItemView = new DynamicImageView();
    private final Label yourImageInfoLabel = new Label();
    private final ProgressIndicator loadingIndicator = new ProgressIndicator();
    private final Button prevButton = new Button("Previous");
    private final Label indexLabel = new Label("0/0");
    private final Button nextButton = new Button("Next");
    private final ProgressIndicator nextLoadingIndicator = new ProgressIndicator(-1);
    private final ListView<String> tagListView = new ListView<>();

    private List<MediaItem> items = null;
    private Map<MediaItem, List<Match>> matches = new HashMap<>();
    private List<DuplicateFinder> finders = null;
    private MediaItem currentItem = null;

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
        root.setStyle("-fx-background-color: -fx-base;");
        DropShadow effect = new DropShadow();
        effect.setSpread(0.5);
        root.setEffect(effect);
        setCenter(root);

        BorderPane header = new BorderPane();
        root.setTop(header);

        Label title = new Label("Find duplicates online");
        header.setLeft(title);
        BorderPane.setMargin(title, ALL5);

        Button exit = new Button("X");
        header.setRight(exit);
        exit.setOnAction(event -> close());

        header.setBottom(new Separator());

        BorderPane currentItemBP = new BorderPane(currentItemView);
        currentItemBP.setStyle("-fx-background-color: derive(-fx-color, 25%);");
        currentItemBP.setMaxSize(156, 156);
        currentItemBP.setMinSize(156, 156);
        matchGridView.setCellFactory(param -> {
            MatchGridCell c = new MatchGridCell();
            c.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY) {
                    compareScreen.open(getManager(), currentItem, c.getItem());
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
        matchGridView.setMaxHeight(350);
        matchGridView.setMinHeight(350);
        yourImageInfoLabel.setWrapText(true);
        tagListView.setFocusTraversable(false);
        tagListView.setCellFactory(param -> {
            OnlineTagListCell c = new OnlineTagListCell(tagNeme -> {
                for (Tag t : currentItem.getTags()) {
                    if (t.getName().equalsIgnoreCase(tagNeme)) return true;
                }
                return false;
            });
            c.setOnMouseClicked(event -> {
                if (c.getItem() != null) {
                    Tag t = currentItem.getMenagerie().getTagByName(c.getItem());
                    if (t == null) {
                        t = currentItem.getMenagerie().createTag(c.getItem());
                    }
                    currentItem.addTag(t);
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
        StackPane.setAlignment(loadingIndicator, Pos.CENTER);
        StackPane.setAlignment(matchGridView, Pos.TOP_CENTER);
        HBox.setHgrow(matchGridView, Priority.ALWAYS);
        StackPane sp = new StackPane(new HBox(5, matchGridView, tagListVBox), loadingIndicator);
        VBox.setVgrow(sp, Priority.ALWAYS);
        HBox yourHBox = new HBox(10, new Label("Your image:"), currentItemBP, yourImageInfoLabel);
        yourHBox.setPadding(ALL5);
        yourHBox.setAlignment(Pos.CENTER);
        loadingIndicator.setMaxSize(100, 100);
        VBox v = new VBox(5, yourHBox, new Separator(), new Label("Found online:"), sp);
        v.setPadding(ALL5);
        root.setCenter(v);

        prevButton.setOnAction(event -> displayPrevious());
        nextButton.setOnAction(event -> displayNext());
        nextLoadingIndicator.setMaxSize(25, 25);
        HBox bottom = new HBox(5, prevButton, indexLabel, nextButton, nextLoadingIndicator);
        bottom.setAlignment(Pos.CENTER);
        bottom.setPadding(ALL5);
        root.setBottom(bottom);

        compareScreen.addSuccessListener(() -> displayItem(currentItem));
    }

    public void open(ScreenPane manager, List<MediaItem> items, List<DuplicateFinder> finders) {
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

        manager.add(compareScreen);
        manager.open(this);
        this.items = items;
        this.finders = finders;

        displayItem(items.get(0));
    }

    private void displayPrevious() {
        if (prevButton.isDisabled()) return;

        int i = items.indexOf(currentItem);
        if (i > 0) {
            displayItem(items.get(i - 1));
        }
    }

    private void displayNext() {
        if (nextButton.isDisabled()) return;

        int i = items.indexOf(currentItem);
        if (i + 1 < items.size()) {
            displayItem(items.get(i + 1));
        }
    }

    private void displayItem(MediaItem item) {
        if (getCurrentItem() != null && !getCurrentItem().getThumbnail().isLoaded()) {
            getCurrentItem().getThumbnail().doNotWant();
        }
        setCurrentItem(item);
        matchGridView.getItems().clear();
        yourImageInfoLabel.setText("N/A");
        nextLoadingIndicator.setOpacity(0);
        tagListView.getItems().clear();

        final int i = items.indexOf(item);

        if (i + 1 >= items.size() || matches.get(items.get(i + 1)) == null) nextButton.setDisable(true);

        prevButton.setDisable(i == 0);
        indexLabel.setText((i + 1) + "/" + items.size());

        if (item != null) {
            loadingIndicator.setOpacity(1);
            loadingIndicator.setDisable(false);

            setThumbnail(item);
            setFileInfo(item);

            Thread t = new Thread(() -> {
                List<Match> matches = getMatches(item);

                if (getCurrentItem().equals(item)) {
                    Platform.runLater(() -> displayMatches(matches));
                }

                final int i2 = items.indexOf(item);
                if (i2 + 1 < items.size()) {
                    Platform.runLater(() -> nextLoadingIndicator.setOpacity(1));
                    getMatches(items.get(i2 + 1));
                    Platform.runLater(() -> {
                        nextLoadingIndicator.setOpacity(0);
                        nextButton.setDisable(false);
                    });
                }
            });
            t.setUncaughtExceptionHandler((t1, e) -> Main.log.log(Level.SEVERE, "Uncaught exception in thread: " + t.toString(), e));
            t.setDaemon(true);
            t.start();
        }
    }

    private void setThumbnail(MediaItem item) {
        Thumbnail thumb = item.getThumbnail();
        thumb.want();
        if (thumb.isLoaded()) {
            currentItemView.setImage(thumb.getImage());
        } else {
            thumb.addImageReadyListener(thing -> {
                Platform.runLater(() -> currentItemView.setImage(thing));
            });
        }
    }

    private void setFileInfo(MediaItem item) {
        Image img = item.getImage();
        if (!img.isBackgroundLoading() || img.getProgress() == 1) {
            yourImageInfoLabel.setText(item.getFile() + "\n" + (int) img.getWidth() + "x" + (int) img.getHeight() + "\n" + Util.bytesToPrettyString(item.getFile().length()));
        } else {
            img.progressProperty().addListener((observable, oldValue, newValue) -> {
                if (!img.isError() && newValue.doubleValue() == 1) {
                    yourImageInfoLabel.setText(item.getFile() + "\n" + (int) img.getWidth() + "x" + (int) img.getHeight() + "\n" + Util.bytesToPrettyString(item.getFile().length()));
                }
            });
        }
    }

    private List<Match> getMatches(MediaItem item) {
        if (item == null) return null;

        List<Match> matches = this.matches.get(item);
        if (matches != null) {
            return matches;
        } else {
            matches = new ArrayList<>();
        }

        for (DuplicateFinder finder : finders) {
            try {
                matches.addAll(finder.getMatchesFor(item.getFile()));
            } catch (IOException e) {
                Main.log.log(Level.WARNING, "Failed to get image info from web (1st try)", e);
                try {
                    matches.addAll(finder.getMatchesFor(item.getFile()));
                } catch (IOException ex) {
                    Main.log.log(Level.SEVERE, "Failed to get image info from web (2nd try, giving up)", ex);
                    Platform.runLater(() -> {
                        AlertDialogScreen a = new AlertDialogScreen();
                        a.open(getManager(), "Failed web get", "Repeatedly failed to get image info online", null);
                    });
                }
            }
        }

        this.matches.put(item, matches);
        return matches;
    }

    private void displayMatches(List<Match> matches) {
        matchGridView.getItems().clear();
        matchGridView.getItems().addAll(matches);
        loadingIndicator.setOpacity(0);
        loadingIndicator.setDisable(true);
        tagListView.getItems().clear();
        matches.forEach(match -> match.getTags().forEach(s -> {
            if (!tagListView.getItems().contains(s)) tagListView.getItems().add(s);
        }));
        tagListView.getItems().sort(String::compareTo);
    }

    public CompareToOnlineScreen getCompareScreen() {
        return compareScreen;
    }

    private synchronized MediaItem getCurrentItem() {
        return currentItem;
    }

    private synchronized void setCurrentItem(MediaItem currentItem) {
        this.currentItem = currentItem;
    }

}
