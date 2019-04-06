package menagerie.gui.grid;

import com.sun.javafx.scene.control.skin.VirtualFlow;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import menagerie.gui.thumbnail.Thumbnail;
import menagerie.model.menagerie.Item;
import org.controlsfx.control.GridView;

import java.util.HashSet;
import java.util.Set;

public class ItemGridView extends GridView<Item> {

    public static final int CELL_BORDER = 4;

    private final ObservableList<Item> selected = FXCollections.observableArrayList();
    private Item lastSelected = null;

    private final Set<GridSelectionListener> selectionListeners = new HashSet<>();


    public ItemGridView() {
        setCellWidth(Thumbnail.THUMBNAIL_SIZE + CELL_BORDER * 2);
        setCellHeight(Thumbnail.THUMBNAIL_SIZE + CELL_BORDER * 2);

        getItems().addListener((ListChangeListener<? super Item>) c -> {
            boolean changed = false;
            while (c.next()) {
                selected.removeAll(c.getRemoved());

                changed = true;
            }

            if (changed) updateCellSelectionCSS();
        });

        setOnMouseReleased(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                selected.clear();
                updateCellSelectionCSS();
                event.consume();
            }
        });

        initOnKeyPressed();
    }

    private void initOnKeyPressed() {
        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
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
            }
        });
    }

    private int getRowLength() {
        return (int) Math.floor((getWidth() - 18) / (Thumbnail.THUMBNAIL_SIZE + CELL_BORDER * 2 + getHorizontalCellSpacing() * 2));
    }

    private int getPageLength() {
        return (int) Math.floor(getHeight() / (Thumbnail.THUMBNAIL_SIZE + CELL_BORDER * 2 + getHorizontalCellSpacing() * 2));
    }

    public void select(Item item, boolean ctrlDown, boolean shiftDown) {
        if (!getItems().contains(item)) return;

        if (ctrlDown) {
            if (isSelected(item)) {
                selected.remove(item);
            } else {
                selected.add(item);
            }
            updateCellSelectionCSS();
        } else if (shiftDown) {
            Item first = getFirstSelected();
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
                    ((VirtualFlow) n).show(getItems().indexOf(getLastSelected()) / getRowLength()); // Garbage API, doesn't account for multi-element rows
                    break;
                }
            }
        }

        // Notify selection listener
        selectionListeners.forEach(listener -> listener.targetSelected(item));
    }

    private void selectRange(Item first, Item last) {
        selected.clear();
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

    private Item getFirstSelected() {
        if (selected.isEmpty()) {
            return null;
        } else {
            return selected.get(0);
        }
    }

    public Item getLastSelected() {
        return lastSelected;
    }

    public ObservableList<Item> getSelected() {
        return selected;
    }

    public void clearSelection() {
        selected.clear();
        updateCellSelectionCSS();
    }

    public boolean isSelected(Item img) {
        return selected.contains(img);
    }

    public boolean addSelectionListener(GridSelectionListener listener) {
        return selectionListeners.add(listener);
    }

    public boolean removeSelectionListener(GridSelectionListener listener) {
        return selectionListeners.remove(listener);
    }

    private void updateCellSelectionCSS() {
        for (Node n : getChildren()) {
            if (n instanceof VirtualFlow) {
                ((VirtualFlow) n).rebuildCells();
                break;
            }
        }
    }

    public void setLastSelected(Item item) {
        lastSelected = item;
    }

}
