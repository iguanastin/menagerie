package menagerie.gui.screens;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import menagerie.gui.media.DynamicMediaView;
import menagerie.model.menagerie.MediaItem;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.Menagerie;
import menagerie.util.PokeListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SlideshowScreen extends Screen {

    private final DynamicMediaView mediaView;

    private final List<Item> items = new ArrayList<>();
    private Item showing = null;
    private Menagerie menagerie;


    public SlideshowScreen() {
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
            }
        });

        setStyle("-fx-background-color: -fx-base;");
        mediaView = new DynamicMediaView();
        mediaView.setRepeat(true);
        mediaView.setMute(false);
        setCenter(mediaView);

        Button left = new Button("<-");
        left.setOnAction(event -> previewLast());

        Button close = new Button("Close");
        close.setOnAction(event -> close());

        Button right = new Button("->");
        right.setOnAction(event -> previewNext());

        HBox h = new HBox(5, left, close, right);
        h.setAlignment(Pos.CENTER);
        h.setPadding(new Insets(5));
        setBottom(h);
    }

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

    public Item getShowing() {
        return showing;
    }

    public void setItemContextMenu(ContextMenu contextMenu) {
        mediaView.setOnContextMenuRequested(event -> contextMenu.show(mediaView, event.getScreenX(), event.getScreenY()));
    }

    public void tryDeleteCurrent(boolean deleteFile) {
        PokeListener onFinish = () -> {
            menagerie.removeImages(Collections.singletonList(getShowing()), deleteFile);

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

        if (deleteFile) {
            new ConfirmationScreen().open(getManager(), "Delete files", "Permanently delete selected files? (1 file)\n\n" +
                    "This action CANNOT be undone (files will be deleted)", onFinish, null);
        } else {
            new ConfirmationScreen().open(getManager(), "Forget files", "Remove selected files from database? (1 file)\n\n" +
                    "This action CANNOT be undone", onFinish, null);
        }
    }

    private void preview(Item item) {
        showing = item;
        if (item instanceof MediaItem) {
            mediaView.preview((MediaItem) item);
        } else {
            mediaView.preview(null);
        }
    }

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

}
