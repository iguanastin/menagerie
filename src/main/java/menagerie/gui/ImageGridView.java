package menagerie.gui;

import com.sun.javafx.scene.control.skin.VirtualFlow;
import com.sun.javafx.scene.control.skin.VirtualScrollBar;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import menagerie.model.ImageInfo;
import org.controlsfx.control.GridView;

import java.util.ArrayList;
import java.util.List;

public class ImageGridView extends GridView<ImageInfo> {

    private static final int CELL_BORDER = 2;

    private final List<ImageInfo> selected = new ArrayList<>();


    public ImageGridView() {
        setCellFactory(param -> new ImageGridCell(this));

        setCellWidth(ImageInfo.THUMBNAIL_SIZE + CELL_BORDER * 2);
        setCellHeight(ImageInfo.THUMBNAIL_SIZE + CELL_BORDER * 2);


        getItems().addListener((ListChangeListener<? super ImageInfo>) c -> {
            while (c.next()) {
                c.getRemoved().forEach(image -> {
                    if (isSelected(image)) {
                        selected.remove(image);
                    }
                });
            }
        });

        setOnMousePressed(event -> {
            selected.clear();
            updateCellSelectionCSS();
            event.consume();
        });

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
                    int rowLength = getRowLength();
                    if (index + rowLength < getItems().size()) {
                        select(getItems().get(index + rowLength), event.isControlDown(), event.isShiftDown());
                    } else {
                        select(getItems().get(getItems().size() - 1), event.isControlDown(), event.isShiftDown());
                    }
                    event.consume();
                    break;
                case UP:
                    rowLength = getRowLength();
                    if (index - rowLength >= 0) {
                        select(getItems().get(index - rowLength), event.isControlDown(), event.isShiftDown());
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
            }
        });
    }

    private int getRowLength() {
        return (int) Math.floor((getWidth() - 18) / (ImageInfo.THUMBNAIL_SIZE + CELL_BORDER * 2 + getHorizontalCellSpacing() * 2));
    }

    void cellMousePressed(ImageGridCell cell, MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY) {
            select(cell.getItem(), event.isControlDown(), event.isShiftDown());
        }
    }

    private void select(ImageInfo item, boolean ctrlDown, boolean shiftDown) {
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

        // Ensure last selected cell is visible
        for (Node n : getChildren()) {
            if (n instanceof VirtualFlow) {
                VirtualFlow<ImageGridCell> vf = (VirtualFlow<ImageGridCell>) n;
                vf.show(getItems().indexOf(getLastSelected()) / getRowLength()); // Garbage API, doesn't account for multi-element rows
                break;
            }
        }
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
        if (selected.isEmpty()) {
            return null;
        } else {
            return selected.get(selected.size() - 1);
        }
    }

    void clearSelection() {
        selected.clear();
        updateCellSelectionCSS();
    }

    boolean isSelected(ImageInfo img) {
        return selected.contains(img);
    }

    public List<ImageInfo> getSelected() {
        return selected;
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
