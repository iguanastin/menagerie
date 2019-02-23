package menagerie.gui.screens;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import menagerie.gui.media.DynamicMediaView;
import menagerie.model.menagerie.ImageInfo;

import java.util.ArrayList;
import java.util.List;

public class SlideshowScreen extends Screen {

    private final DynamicMediaView mediaView;

    private final List<ImageInfo> items = new ArrayList<>();
    private ImageInfo showing = null;


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

    public void open(ScreenPane manager, List<ImageInfo> items) {
        manager.open(this);

        this.items.clear();
        this.items.addAll(items);
        if (!items.isEmpty()) {
            preview(items.get(0));
        } else {
            preview(null);
        }
    }

    @Override
    protected void onHide() {
        items.clear();
        preview(null);
    }

    public ImageInfo getShowing() {
        return showing;
    }

    public void setItemContextMenu(ContextMenu contextMenu) {
        mediaView.setOnContextMenuRequested(event -> contextMenu.show(mediaView, event.getScreenX(), event.getScreenY()));
    }

    public void releaseMediaPlayer() {
        mediaView.releaseMediaPlayer();
    }

    public void removeCurrent() {
        if (items.isEmpty() || showing == null) return;

        final int index = items.indexOf(getShowing());
        items.remove(getShowing());

        if (!items.isEmpty()) {
            preview(items.get(Math.max(0, Math.min(index, items.size() - 1))));
        } else {
            preview(null);
            close();
        }
    }

    private void preview(ImageInfo item) {
        showing = item;
        mediaView.preview(item);
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
