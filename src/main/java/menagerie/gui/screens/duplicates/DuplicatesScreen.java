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

import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import menagerie.gui.ItemInfoBox;
import menagerie.gui.media.DynamicMediaView;
import menagerie.gui.screens.Screen;
import menagerie.gui.screens.ScreenPane;
import menagerie.gui.taglist.TagListCell;
import menagerie.model.SimilarPair;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;
import menagerie.model.menagerie.Menagerie;
import menagerie.model.menagerie.Tag;
import menagerie.util.listeners.ObjectListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DuplicatesScreen extends Screen {

    private final DynamicMediaView leftMediaView = new DynamicMediaView();
    private final DynamicMediaView rightMediaView = new DynamicMediaView();
    private final ListView<Tag> leftTagList = new ListView<>();
    private final ListView<Tag> rightTagList = new ListView<>();
    private final ItemInfoBox leftInfoBox = new ItemInfoBox();
    private final ItemInfoBox rightInfoBox = new ItemInfoBox();
    private final CheckBox nonDupeCheckBox = new CheckBox("Not a duplicate");

    private final Label similarityLabel = new Label("N/A");

    private Menagerie menagerie = null;
    private List<SimilarPair<MediaItem>> pairs = null;
    private SimilarPair<MediaItem> currentPair = null;

    private boolean deleteFile = true;

    private ObjectListener<Item> selectListener = null;

    private ListChangeListener<Tag> leftTagListener = c -> {
        while (c.next()) {
            repopulateTagLists();
        }
    };

    private ListChangeListener<Tag> rightTagListener = c -> {
        while (c.next()) {
            repopulateTagLists();
        }
    };


    public DuplicatesScreen() {
        addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                close();
                event.consume();
            } else if (event.getCode() == KeyCode.LEFT) {
                previewPrev();
                event.consume();
            } else if (event.getCode() == KeyCode.RIGHT) {
                previewNext();
                event.consume();
            } else if (event.getCode() == KeyCode.END) {
                preview(pairs.get(pairs.size() - 1));
                event.consume();
            } else if (event.getCode() == KeyCode.HOME) {
                preview(pairs.get(0));
                event.consume();
            }
        });

        setStyle("-fx-background-color: -fx-base;");

        // ---------------------------------------------- Center Element -----------------------------------------------
        leftMediaView.setOnContextMenuRequested(event -> {
            MenuItem select = new MenuItem("Select in explorer");
            MenuItem combineTags = new MenuItem("Add tags to other -->");
            ContextMenu cm = new ContextMenu(select, combineTags);
            select.setOnAction(event1 -> {
                if (selectListener != null) selectListener.pass(currentPair.getObject1());
                cm.hide();
                close();
            });
            combineTags.setOnAction(event1 -> {
                currentPair.getObject1().getTags().forEach(tag -> currentPair.getObject2().addTag(tag));
                cm.hide();
            });
            cm.show(leftMediaView, event.getScreenX(), event.getScreenY());
        });
        rightMediaView.setOnContextMenuRequested(event -> {
            MenuItem select = new MenuItem("Select in explorer");
            MenuItem combineTags = new MenuItem("<-- Add tags to other");
            ContextMenu cm = new ContextMenu(select, combineTags);
            select.setOnAction(event1 -> {
                if (selectListener != null) selectListener.pass(currentPair.getObject2());
                cm.hide();
                close();
            });
            combineTags.setOnAction(event1 -> {
                currentPair.getObject2().getTags().forEach(tag -> currentPair.getObject1().addTag(tag));
                cm.hide();
            });
            cm.show(rightMediaView, event.getScreenX(), event.getScreenY());
        });
        leftTagList.setCellFactory(param -> {
            TagListCell c = new TagListCell() {
                @Override
                protected void updateItem(Tag tag, boolean empty) {
                    super.updateItem(tag, empty);

                    if (tag == null || currentPair.getObject2().hasTag(tag)) {
                        setStyle(null);
                    } else {
                        setStyle("-fx-background-color: blue;");
                    }
                }
            };

            MenuItem addToOther = new MenuItem("Add to other");
            addToOther.setOnAction(event -> currentPair.getObject2().addTag(c.getItem()));
            MenuItem removeTag = new MenuItem("Remove tag");
            removeTag.setOnAction(event -> currentPair.getObject1().removeTag(c.getItem()));
            ContextMenu cm = new ContextMenu(addToOther, new SeparatorMenuItem(), removeTag);
            c.setOnContextMenuRequested(event -> cm.show(c, event.getScreenX(), event.getScreenY()));
            return c;
        });
        leftTagList.setPrefWidth(200);
        rightTagList.setCellFactory(param -> {
            TagListCell c = new TagListCell() {
                @Override
                protected void updateItem(Tag tag, boolean empty) {
                    super.updateItem(tag, empty);

                    if (tag == null || currentPair.getObject1().hasTag(tag)) {
                        setStyle(null);
                    } else {
                        setStyle("-fx-background-color: blue;");
                    }
                }
            };

            MenuItem addToOther = new MenuItem("Add to other");
            addToOther.setOnAction(event -> currentPair.getObject1().addTag(c.getItem()));
            MenuItem removeTag = new MenuItem("Remove tag");
            removeTag.setOnAction(event -> currentPair.getObject2().removeTag(c.getItem()));
            ContextMenu cm = new ContextMenu(addToOther, new SeparatorMenuItem(), removeTag);
            c.setOnContextMenuRequested(event -> cm.show(c, event.getScreenX(), event.getScreenY()));
            return c;
        });
        rightTagList.setPrefWidth(200);
        leftInfoBox.setAlignment(Pos.BOTTOM_LEFT);
        leftInfoBox.setMaxHeight(USE_PREF_SIZE);
        leftInfoBox.setOpacity(0.75);
        BorderPane.setAlignment(leftInfoBox, Pos.BOTTOM_LEFT);
        rightInfoBox.setAlignment(Pos.BOTTOM_RIGHT);
        rightInfoBox.setMaxHeight(USE_PREF_SIZE);
        rightInfoBox.setOpacity(0.75);
        BorderPane.setAlignment(rightInfoBox, Pos.BOTTOM_RIGHT);
        BorderPane lbp = new BorderPane(null, null, leftTagList, null, leftInfoBox);
        lbp.setPickOnBounds(false);
        BorderPane rbp = new BorderPane(null, null, rightInfoBox, null, rightTagList);
        rbp.setPickOnBounds(false);
        SplitPane sp = new SplitPane(new StackPane(leftMediaView, lbp), new StackPane(rightMediaView, rbp));
        sp.setOnMouseEntered(event -> {
            leftTagList.setDisable(false);
            rightTagList.setDisable(false);
            leftTagList.setOpacity(0.75);
            rightTagList.setOpacity(0.75);
        });
        sp.setOnMouseExited(event -> {
            leftTagList.setDisable(true);
            rightTagList.setDisable(true);
            leftTagList.setOpacity(0);
            rightTagList.setOpacity(0);
        });
        setCenter(sp);

        // ---------------------------------------- Bottom element -----------------------------------------------------
        VBox bottom = new VBox(5);
        bottom.setPadding(new Insets(5));
        // Construct first element
        HBox hbc = new HBox(5, similarityLabel);
        hbc.setAlignment(Pos.CENTER);
        Button leftDeleteButton = new Button("Delete");
        leftDeleteButton.setOnAction(event -> deleteItem(currentPair.getObject1()));
        Button rightDeleteButton = new Button("Delete");
        rightDeleteButton.setOnAction(event -> deleteItem(currentPair.getObject2()));
        HBox hbl = new HBox(leftDeleteButton);
        hbl.setAlignment(Pos.CENTER_LEFT);
        HBox hbr = new HBox(rightDeleteButton);
        hbr.setAlignment(Pos.CENTER_RIGHT);
        bottom.getChildren().add(new BorderPane(hbc, null, hbr, null, hbl));
        // Construct second element
        Button prevPairButton = new Button("<-");
        prevPairButton.setOnAction(event -> previewPrev());
        Button closeButton = new Button("Close");
        closeButton.setOnAction(event -> close());
        Button nextPairButton = new Button("->");
        nextPairButton.setOnAction(event -> previewNext());
        nonDupeCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                menagerie.addNonDuplicate(currentPair);
            } else {
                menagerie.removeNonDuplicate(currentPair);
            }
        });
        HBox hb = new HBox(5, prevPairButton, closeButton, nextPairButton, nonDupeCheckBox);
        hb.setAlignment(Pos.CENTER);
        bottom.getChildren().add(hb);
        setBottom(bottom);


        setDefaultFocusNode(closeButton);
    }

    /**
     * Opens this screen in a manager and displays the first pair.
     *
     * @param manager   Manager to open in.
     * @param menagerie Menagerie.
     * @param pairs     Pairs to view and resolve.
     */
    public void open(ScreenPane manager, Menagerie menagerie, List<SimilarPair<MediaItem>> pairs) {
        if (manager == null || menagerie == null || pairs == null || pairs.isEmpty()) return;

        openWithOldPairs(manager, menagerie);

        this.pairs = pairs;
        preview(pairs.get(0));
    }

    public void openWithOldPairs(ScreenPane manager, Menagerie menagerie) {
        this.menagerie = menagerie;

        leftTagList.setDisable(false);
        rightTagList.setDisable(false);
        leftTagList.setOpacity(0.75);
        rightTagList.setOpacity(0.75);

        manager.open(this);
    }

    /**
     * Displays the next pair, if there is one.
     */
    private void previewNext() {
        if (pairs == null || pairs.isEmpty()) return;

        if (currentPair == null) {
            preview(pairs.get(0));
        } else {
            int i = pairs.indexOf(currentPair);
            if (i >= 0) {
                if (i + 1 < pairs.size()) preview(pairs.get(i + 1));
            } else {
                preview(pairs.get(0));
            }
        }
    }

    /**
     * Displays the previous pair, if there is one.
     */
    private void previewPrev() {
        if (pairs == null || pairs.isEmpty()) return;

        if (currentPair == null) {
            preview(pairs.get(0));
        } else {
            int i = pairs.indexOf(currentPair);
            if (i > 0) {
                preview(pairs.get(i - 1));
            } else {
                preview(pairs.get(0));
            }
        }
    }

    /**
     * Displays a pair.
     *
     * @param pair Pair to display.
     */
    private void preview(SimilarPair<MediaItem> pair) {
        if (currentPair != null) {
            currentPair.getObject1().getTags().removeListener(leftTagListener);
            currentPair.getObject2().getTags().removeListener(rightTagListener);
        }
        currentPair = pair;

        if (pair != null) {
            leftMediaView.preview(pair.getObject1());
            rightMediaView.preview(pair.getObject2());

            leftTagList.getItems().clear();
            leftTagList.getItems().addAll(pair.getObject1().getTags());
            leftTagList.getItems().sort(Comparator.comparing(Tag::getName));
            currentPair.getObject1().getTags().addListener(leftTagListener);

            rightTagList.getItems().clear();
            rightTagList.getItems().addAll(pair.getObject2().getTags());
            rightTagList.getItems().sort(Comparator.comparing(Tag::getName));
            currentPair.getObject2().getTags().addListener(rightTagListener);

            leftInfoBox.setItem(pair.getObject1());
            rightInfoBox.setItem(pair.getObject2());

            DecimalFormat df = new DecimalFormat("#.##");
            similarityLabel.setText((pairs.indexOf(pair) + 1) + "/" + pairs.size() + ": " + df.format(pair.getSimilarity() * 100) + "%");

            nonDupeCheckBox.setSelected(menagerie.hasNonDuplicate(pair));
        } else {
            leftMediaView.preview(null);
            rightMediaView.preview(null);

            leftTagList.getItems().clear();
            rightTagList.getItems().clear();

            leftInfoBox.setItem(null);
            rightInfoBox.setItem(null);

            similarityLabel.setText("N/A");
            nonDupeCheckBox.setSelected(false);
        }
    }

    /**
     * Attempts to delete an item. WARNING: Deletes the file.
     *
     * @param toDelete Item to delete.
     */
    private void deleteItem(MediaItem toDelete) {
        if (menagerie == null) return;

        if (currentPair.getObject1().equals(toDelete)) leftMediaView.stop();
        if (currentPair.getObject2().equals(toDelete)) rightMediaView.stop();

        int index = pairs.indexOf(currentPair);

        if (isDeleteFile()) {
            menagerie.deleteItem(toDelete);
        } else {
            menagerie.forgetItem(toDelete);
        }

        //Remove other pairs containing the deleted image
        for (SimilarPair pair : new ArrayList<>(pairs)) {
            if (toDelete.equals(pair.getObject1()) || toDelete.equals(pair.getObject2())) {
                int i = pairs.indexOf(pair);
                pairs.remove(pair);
                if (i < index) {
                    index--;
                }
            }
        }

        if (index > pairs.size() - 1) index = pairs.size() - 1;

        if (pairs.isEmpty()) {
            close();
        } else {
            preview(pairs.get(index));
        }
    }

    private void repopulateTagLists() {
        leftTagList.getItems().clear();
        if (currentPair != null) {
            leftTagList.getItems().addAll(currentPair.getObject1().getTags());
            leftTagList.getItems().sort(Comparator.comparing(Tag::getName));
        }

        rightTagList.getItems().clear();
        if (currentPair != null) {
            rightTagList.getItems().addAll(currentPair.getObject2().getTags());
            rightTagList.getItems().sort(Comparator.comparing(Tag::getName));
        }
    }

    public List<SimilarPair<MediaItem>> getPairs() {
        return pairs;
    }

    /**
     * @return True if this duplicate screen will delete the file when deleting the duplicate.
     */
    public boolean isDeleteFile() {
        return deleteFile;
    }

    /**
     * @param deleteFile Delete the file when deleting duplicates.
     */
    public void setDeleteFile(boolean deleteFile) {
        this.deleteFile = deleteFile;
    }

    /**
     * @param selectListener Listener waiting for the user to ask to select an item in the explorer.
     */
    public void setSelectListener(ObjectListener<Item> selectListener) {
        this.selectListener = selectListener;
    }

    public ItemInfoBox getLeftInfoBox() {
        return leftInfoBox;
    }

    public ItemInfoBox getRightInfoBox() {
        return rightInfoBox;
    }

    /**
     * Releases all VLCJ resources.
     */
    public void releaseVLCJ() {
        leftMediaView.releaseVLCJ();
        rightMediaView.releaseVLCJ();
    }

}
