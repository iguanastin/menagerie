package menagerie.gui;

import com.sun.javafx.scene.control.skin.VirtualFlow;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import menagerie.model.ImageInfo;
import menagerie.model.Menagerie;
import org.controlsfx.control.GridView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ImageGridView extends GridView<ImageInfo> {

    static final int CELL_BORDER = 2;


    private final ClipboardContent clipboard = new ClipboardContent();

    private final List<ImageInfo> selected = new ArrayList<>();
    private ImageInfo lastSelected = null;
    private SelectionListener selectionListener = null;

    private boolean dragging = false;


    public ImageGridView() {
        setCellWidth(ImageInfo.THUMBNAIL_SIZE + CELL_BORDER * 2);
        setCellHeight(ImageInfo.THUMBNAIL_SIZE + CELL_BORDER * 2);


        setCellFactory(param -> {
            ImageGridCell c = new ImageGridCell(this);
            c.setOnDragDetected(event -> {
                if (!selected.isEmpty() && event.isPrimaryButtonDown()) {
                    if (!isSelected(c.getItem())) select(c.getItem(), event.isControlDown(), event.isShiftDown());

                    Dragboard db = c.startDragAndDrop(TransferMode.ANY);
                    List<File> files = new ArrayList<>();
                    selected.forEach(img -> files.add(img.getFile()));
                    clipboard.putFiles(files);
                    db.setContent(clipboard);

                    dragging = true;
                    event.consume();
                }
            });
            c.setOnDragDone(event -> {
                dragging = false;
                event.consume();
            });
            c.setOnMouseReleased(event -> {
                if (!dragging) {
                    if (event.getButton() == MouseButton.PRIMARY) {
                        select(c.getItem(), event.isControlDown(), event.isShiftDown());
                        event.consume();
                    } else if (event.getButton() == MouseButton.SECONDARY) {
                        ContextMenu m = new ContextMenu(new MenuItem("TEst 1"), new MenuItem("test 2"));
                        m.show(c, event.getScreenX(), event.getScreenY());
                        event.consume();
                    }
                }
            });
            return c;
        });

        getItems().addListener((ListChangeListener<? super ImageInfo>) c -> {
            while (c.next()) {
                c.getRemoved().forEach(selected::remove);
            }
            updateCellSelectionCSS();
        });

        setOnMouseReleased(event -> {
            selected.clear();
            updateCellSelectionCSS();
            event.consume();
        });

        initOnKeyPressed();
    }

    private void initOnKeyPressed() {
        setOnKeyPressed(event -> {
            if (getItems().isEmpty()) return;

            int index = getItems().indexOf(getLastSelected());
            switch (event.getCode()) {
                case LEFT:
                    if (index > 0) select(getItems().get(index - 1), event.isControlDown(), event.isShiftDown());
                    event.consume();
                    break;
                case RIGHT:
                    if (index < getItems().size() - 1)
                        select(getItems().get(index + 1), event.isControlDown(), event.isShiftDown());
                    event.consume();
                    break;
                case DOWN:
                    if (selected.isEmpty() && index == -1) {
                        select(getItems().get(0), event.isControlDown(), event.isShiftDown());
                    } else {
                        if (index + getRowLength() < getItems().size()) {
                            select(getItems().get(index + getRowLength()), event.isControlDown(), event.isShiftDown());
                        } else {
                            select(getItems().get(getItems().size() - 1), event.isControlDown(), event.isShiftDown());
                        }
                    }
                    event.consume();
                    break;
                case UP:
                    if (index - getRowLength() >= 0) {
                        select(getItems().get(index - getRowLength()), event.isControlDown(), event.isShiftDown());
                    } else {
                        select(getItems().get(0), event.isControlDown(), event.isShiftDown());
                    }
                    event.consume();
                    break;
                case A:
                    if (event.isControlDown()) {
                        if (selected.size() == getItems().size()) {
                            clearSelection();
                        } else {
                            clearSelection();
                            selected.addAll(getItems());
                            updateCellSelectionCSS();
                        }
                    }
                    event.consume();
                    break;
                case HOME:
                    select(getItems().get(0), event.isControlDown(), event.isShiftDown());
                    event.consume();
                    break;
                case END:
                    select(getItems().get(getItems().size() - 1), event.isControlDown(), event.isShiftDown());
                    event.consume();
                    break;
                case PAGE_DOWN:
                    if (index + (getPageLength() - 1) * getRowLength() < getItems().size()) {
                        select(getItems().get(index + (getPageLength() - 1) * getRowLength()), event.isControlDown(), event.isShiftDown());
                    } else {
                        select(getItems().get(getItems().size() - 1), event.isControlDown(), event.isShiftDown());
                    }
                    event.consume();
                    break;
                case PAGE_UP:
                    if (index - (getPageLength() - 1) * getRowLength() >= 0) {
                        select(getItems().get(index - (getPageLength() - 1) * getRowLength()), event.isControlDown(), event.isShiftDown());
                    } else {
                        select(getItems().get(0), event.isControlDown(), event.isShiftDown());
                    }
                    event.consume();
                    break;
                case DELETE:
                    if (!selected.isEmpty()) {
                        Alert d = new Alert(Alert.AlertType.CONFIRMATION);

                        if (event.isControlDown()) {
                            d.setTitle("Forget files");
                            d.setHeaderText("Remove selected files from database? (" + selected.size() + " files)");
                            d.setContentText("This action CANNOT be undone");
                        } else {
                            d.setTitle("Delete files");
                            d.setHeaderText("Permanently delete selected files? (" + selected.size() + " files)");
                            d.setContentText("This action CANNOT be undone (files will be deleted)");
                        }

                        Optional result = d.showAndWait();
                        if (result.isPresent() && result.get() == ButtonType.OK) {
                            List<ImageInfo> temp = new ArrayList<>(selected.size());
                            temp.addAll(selected);
                            temp.forEach(img -> img.getMenagerie().removeImage(img, !event.isControlDown()));
                        }

                        event.consume();
                    }
                    break;
            }
        });
    }

    private int getRowLength() {
        return (int) Math.floor((getWidth() - 18) / (ImageInfo.THUMBNAIL_SIZE + CELL_BORDER * 2 + getHorizontalCellSpacing() * 2));
    }

    private int getPageLength() {
        return (int) Math.floor(getHeight() / (ImageInfo.THUMBNAIL_SIZE + CELL_BORDER * 2 + getHorizontalCellSpacing() * 2));
    }

    void select(ImageInfo item, boolean ctrlDown, boolean shiftDown) {
        if (ctrlDown) {
            if (isSelected(item)) {
                selected.remove(item);
            } else {
                selected.add(item);
            }
            updateCellSelectionCSS();
        } else if (shiftDown) {
            ImageInfo first = getFirstSelected();
            if (first == null) {
                selected.add(item);
            } else {
                selectRange(first, item);
            }
            updateCellSelectionCSS();
        } else {
            if (isSelected(item) && selected.size() == 1) {
                selected.clear();
            } else {
                selected.clear();
                selected.add(item);
            }
            updateCellSelectionCSS();
        }
        lastSelected = item;

        // Ensure last selected cell is visible
        if (getLastSelected() != null) {
            for (Node n : getChildren()) {
                if (n instanceof VirtualFlow) {
                    VirtualFlow<ImageGridCell> vf = (VirtualFlow<ImageGridCell>) n;
                    vf.show(getItems().indexOf(getLastSelected()) / getRowLength()); // Garbage API, doesn't account for multi-element rows
                    break;
                }
            }
        }

        // Notify selection listener
        if (selectionListener != null) selectionListener.targetSelected(item);
    }

    private void selectRange(ImageInfo first, ImageInfo last) {
        selected.clear();
        selected.add(first);
        final int start = getItems().indexOf(first);
        final int end = getItems().indexOf(last);
        if (end >= start) {
            for (int i = start; i <= end; i++) {
                selected.add(getItems().get(i));
            }
        } else {
            for (int i = start; i >= end; i--) {
                selected.add(getItems().get(i));
            }
        }
    }

    private ImageInfo getFirstSelected() {
        if (selected.isEmpty()) {
            return null;
        } else {
            return selected.get(0);
        }
    }

    private ImageInfo getLastSelected() {
        return lastSelected;
    }

    List<ImageInfo> getSelected() {
        return selected;
    }

    void clearSelection() {
        selected.clear();
        updateCellSelectionCSS();
    }

    boolean isSelected(ImageInfo img) {
        return selected.contains(img);
    }

    void setSelectionListener(SelectionListener selectionListener) {
        this.selectionListener = selectionListener;
    }

    private void updateCellSelectionCSS() {
        for (Node n : getChildren()) {
            if (n instanceof VirtualFlow) {
                VirtualFlow<ImageGridCell> vf = (VirtualFlow<ImageGridCell>) n;
                vf.rebuildCells();
                break;
            }
        }
    }

}
