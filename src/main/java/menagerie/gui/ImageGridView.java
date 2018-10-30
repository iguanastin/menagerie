package menagerie.gui;

import com.sun.javafx.scene.control.skin.VirtualFlow;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.*;
import menagerie.model.ImageInfo;
import org.controlsfx.control.GridView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ImageGridView extends GridView<ImageInfo> {

    static final int CELL_BORDER = 2;


    private final ClipboardContent clipboard = new ClipboardContent();

    private final List<ImageInfo> selected = new ArrayList<>();
    private ImageInfo lastSelected = null;

    private SelectionListener selectionListener = null;
    private ProgressQueueListener progressQueueListener = null;

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
                if (!dragging && event.getButton() == MouseButton.PRIMARY) {
                    select(c.getItem(), event.isControlDown(), event.isShiftDown());
                    event.consume();
                }
            });
            c.setOnContextMenuRequested(event -> {
                MenuItem i1 = new MenuItem("Open in Explorer");
                i1.setOnAction(event1 -> {
                    try {
                        Runtime.getRuntime().exec("explorer.exe /select, " + c.getItem().getFile().getAbsolutePath());
                    } catch (IOException e) {
                        e.printStackTrace();
                        Main.showErrorMessage("Unexpected Error", "Error opening file explorer", e.getLocalizedMessage());
                    }
                });

                MenuItem i2 = new MenuItem("Build MD5 Hash");
                i2.setOnAction(event1 -> {
                    List<Runnable> queue = new ArrayList<>();
                    selected.forEach(img -> {
                        if (img.getMD5() == null) {
                            queue.add(() -> {
                                img.initializeMD5();
                                img.commitMD5ToDatabase();
                            });
                        }
                    });
                    if (!queue.isEmpty()) {
                        if (progressQueueListener != null) {
                            progressQueueListener.processProgressQueue("Building MD5s", "Building MD5 hashes for " + queue.size() + " files...", queue);
                        } else {
                            queue.forEach(Runnable::run);
                        }
                    }
                });
                MenuItem i3 = new MenuItem("Build Histogram");
                i3.setOnAction(event1 -> {
                    List<Runnable> queue = new ArrayList<>();
                    selected.forEach(img -> {
                        if (img.getHistogram() == null) {
                            queue.add(() -> {
                                img.initializeHistogram();
                                img.commitHistogramToDatabase();
                            });
                        }
                    });
                    if (!queue.isEmpty()) {
                        if (progressQueueListener != null) {
                            progressQueueListener.processProgressQueue("Building Histograms", "Building image histograms for " + queue.size() + " files...", queue);
                        } else {
                            queue.forEach(Runnable::run);
                        }
                    }
                });

                MenuItem i4 = new MenuItem("Find Duplicates");
                i4.setOnAction(event1 -> {
                    //TODO: handle this
                });

                MenuItem i5 = new MenuItem("Remove");
                i5.setOnAction(event1 -> {
                    deleteEventUserInput(false);
                });
                MenuItem i6 = new MenuItem("Delete");
                i6.setOnAction(event1 -> {
                    deleteEventUserInput(true);
                });

                ContextMenu m = new ContextMenu(i1, new SeparatorMenuItem(), i2, i3, new SeparatorMenuItem(), i4, new SeparatorMenuItem(), i5, i6);
                m.show(c, event.getScreenX(), event.getScreenY());
                event.consume();
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
            if (event.getButton() == MouseButton.PRIMARY) {
                selected.clear();
                updateCellSelectionCSS();
                event.consume();
            }
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
                    deleteEventUserInput(!event.isControlDown());
                    event.consume();
                    break;
            }
        });
    }

    private void deleteEventUserInput(boolean deleteFiles) {
        if (!selected.isEmpty()) {
            Alert d = new Alert(Alert.AlertType.CONFIRMATION);

            if (deleteFiles) {
                d.setTitle("Delete files");
                d.setHeaderText("Permanently delete selected files? (" + selected.size() + " files)");
                d.setContentText("This action CANNOT be undone (files will be deleted)");
            } else {
                d.setTitle("Forget files");
                d.setHeaderText("Remove selected files from database? (" + selected.size() + " files)");
                d.setContentText("This action CANNOT be undone");
            }

            Optional result = d.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                new ArrayList<>(selected).forEach(img -> img.remove(deleteFiles));
            }

        }
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

    public void setProgressQueueListener(ProgressQueueListener progressQueueListener) {
        this.progressQueueListener = progressQueueListener;
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

    void setLastSelected(ImageInfo img) {
        lastSelected = img;
    }

    boolean unselect(ImageInfo img) {
        boolean r = selected.remove(img);
        updateCellSelectionCSS();
        return r;
    }

}
