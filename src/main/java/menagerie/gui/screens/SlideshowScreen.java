package menagerie.gui.screens;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import menagerie.gui.ItemInfoBox;
import menagerie.gui.media.DynamicMediaView;
import menagerie.gui.screens.dialogs.ConfirmationScreen;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;
import menagerie.model.menagerie.Menagerie;
import menagerie.util.listeners.ObjectListener;
import menagerie.util.listeners.PokeListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SlideshowScreen extends Screen {

    private final DynamicMediaView mediaView = new DynamicMediaView();
    private final ItemInfoBox infoBox = new ItemInfoBox();
    private final Label countLabel = new Label("0/0");

    private final List<Item> items = new ArrayList<>();
    private Item showing = null;
    private Menagerie menagerie;


    public SlideshowScreen(ObjectListener<Item> selectListener) {
        addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.LEFT) {
                previewLast();
                event.consume();
            } else if (event.getCode() == KeyCode.RIGHT) {
                previewNext();
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                close();
                event.consume();
            } else if (event.getCode() == KeyCode.DELETE) {
                tryDeleteCurrent(!event.isControlDown());
                event.consume();
            } else if (event.getCode() == KeyCode.END) {
                preview(items.get(items.size() - 1));
                event.consume();
            } else if (event.getCode() == KeyCode.HOME) {
                preview(items.get(0));
                event.consume();
            } else if (event.getCode() == KeyCode.R && event.isControlDown()) {
                reverse();
                event.consume();
            } else if (event.getCode() == KeyCode.S && event.isControlDown()) {
                shuffle();
                event.consume();
            }
        });

        setStyle("-fx-background-color: -fx-base;");
        mediaView.setRepeat(true);
        mediaView.setMute(false);
        infoBox.setMaxWidth(USE_PREF_SIZE);
        infoBox.setAlignment(Pos.BOTTOM_RIGHT);
        infoBox.setOpacity(0.75);
        BorderPane.setAlignment(infoBox, Pos.BOTTOM_RIGHT);
        BorderPane.setMargin(infoBox, new Insets(5));
        BorderPane bp = new BorderPane(null, null, null, infoBox, null);
        bp.setPickOnBounds(false);
        StackPane sp = new StackPane(mediaView, bp);
        setCenter(sp);

        Button left = new Button("<-");
        left.setOnAction(event -> previewLast());

        Button select = new Button("Select");
        select.setOnAction(event -> {
            if (showing != null && selectListener != null) selectListener.pass(showing);
        });

        Button close = new Button("Close");
        close.setOnAction(event -> close());

        Button shuffle = new Button("Shuffle");
        shuffle.setOnAction(event -> shuffle());

        Button reverse = new Button("Reverse");
        reverse.setOnAction(event -> reverse());

        Button right = new Button("->");
        right.setOnAction(event -> previewNext());

        HBox h = new HBox(5, left, select, right, countLabel);
        h.setAlignment(Pos.CENTER);
        bp = new BorderPane(h, null, close, null, new HBox(5, shuffle, reverse));
        bp.setPadding(new Insets(5));
        setBottom(bp);
    }

    /**
     * Reverses the order of the items.
     */
    private void reverse() {
        Collections.reverse(items);
        updateCountLabel();
    }

    /**
     * Shuffles the order of the items.
     */
    private void shuffle() {
        Collections.shuffle(items);
        updateCountLabel();
        if (!items.isEmpty()) preview(items.get(0));
    }

    /**
     * Opens this screen in a manager.
     *
     * @param manager   Manager to open in.
     * @param menagerie Menagerie.
     * @param items     Items to display.
     */
    public void open(ScreenPane manager, Menagerie menagerie, List<Item> items) {
        this.items.clear();
        this.items.addAll(items);

        manager.open(this);

        this.menagerie = menagerie;
    }

    @Override
    protected void onOpen() {
        if (!items.isEmpty()) {
            preview(items.get(0));
        } else {
            preview(null);
        }
    }

    @Override
    protected void onClose() {
        items.clear();
        preview(null);
    }

    /**
     * @return Currently displayed item.
     */
    public Item getShowing() {
        return showing;
    }

    public ItemInfoBox getInfoBox() {
        return infoBox;
    }

    /**
     * Attempts to delete or remove the currently displayed item.
     *
     * @param deleteFile Delete the file after removing from the Menagerie.
     */
    public void tryDeleteCurrent(boolean deleteFile) {
        PokeListener onFinish = () -> {
            if (deleteFile) {
                menagerie.deleteItem(getShowing());
            } else {
                menagerie.forgetItem(getShowing());
            }

            if (items.isEmpty() || showing == null) return;

            final int index = items.indexOf(getShowing());
            items.remove(getShowing());

            if (!items.isEmpty()) {
                preview(items.get(Math.max(0, Math.min(index, items.size() - 1))));
            } else {
                preview(null);
                close();
            }
        };

        mediaView.stop();
        if (deleteFile) {
            new ConfirmationScreen().open(getManager(), "Delete files", "Permanently delete selected files? (1 file)\n\n" + "This action CANNOT be undone (files will be deleted)", onFinish, null);
        } else {
            new ConfirmationScreen().open(getManager(), "Forget files", "Remove selected files from database? (1 file)\n\n" + "This action CANNOT be undone", onFinish, null);
        }
    }

    /**
     * Displays the given item.
     *
     * @param item Item to display.
     */
    private void preview(Item item) {
        showing = item;

        updateCountLabel();

        if (item instanceof MediaItem) {
            mediaView.preview((MediaItem) item);
            infoBox.setItem((MediaItem) item);
        } else {
            mediaView.preview(null);
        }
    }

    /**
     * Updates contents of the count label.
     */
    private void updateCountLabel() {
        if (showing != null) {
            countLabel.setText(String.format("%d/%d", items.indexOf(showing) + 1, items.size()));
        } else {
            countLabel.setText("" + items.size());
        }
    }

    /**
     * Previews the next item in the list, if there is one.
     */
    private void previewNext() {
        if (items.isEmpty()) {
            preview(null);
            return;
        }

        if (!items.contains(showing)) {
            preview(items.get(0));
        } else {
            final int index = items.indexOf(showing);
            if (index < items.size() - 1) {
                preview(items.get(index + 1));
            }
        }
    }

    /**
     * Previews the previous item in the list, if there is one.
     */
    private void previewLast() {
        if (items.isEmpty()) {
            preview(null);
            return;
        }

        if (!items.contains(showing)) {
            preview(items.get(0));
        } else {
            final int index = items.indexOf(showing);
            if (index > 0) {
                preview(items.get(index - 1));
            }
        }
    }

    /**
     * Releases VLCJ resources.
     */
    public void releaseVLCJ() {
        mediaView.releaseVLCJ();
    }

}
