package menagerie.gui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import menagerie.model.menagerie.ImageInfo;

public class ItemInfoBox extends VBox {

    private Label fileSize, resolution, filePath;

    private static final String DEFAULT_FILESIZE_TEXT = "0B";
    private static final String DEFAULT_RESOLUTION_TEXT = "0x0";
    private static final String DEFAULT_FILEPATH_TEXT = "N/A";


    public ItemInfoBox() {
        setPadding(new Insets(5));
        setStyle("-fx-background-color: -fx-base;");
        setSpacing(5);

        fileSize = new Label(DEFAULT_FILESIZE_TEXT);
        resolution = new Label(DEFAULT_RESOLUTION_TEXT);
        filePath = new Label(DEFAULT_FILEPATH_TEXT);
        getChildren().addAll(fileSize, resolution, filePath);
    }

    public void setItem(ImageInfo item) {
        if (item != null) {
            fileSize.setText(bytesToPrettyString(item.getFile().length()));
            filePath.setText(item.getFile().toString());
            if (item.isImage()) { //TODO: Support for video resolution
                if (item.getImage().isBackgroundLoading() && item.getImage().getProgress() != 1) {
                    resolution.setText("Loading...");
                    item.getImage().progressProperty().addListener((observable, oldValue, newValue) -> {
                        if (newValue.doubleValue() == 1 && !item.getImage().isError())
                            resolution.setText((int) item.getImage().getWidth() + "x" + (int) item.getImage().getHeight());
                    });
                } else {
                    resolution.setText((int) item.getImage().getWidth() + "x" + (int) item.getImage().getHeight());
                }
            } else {
                resolution.setText(DEFAULT_RESOLUTION_TEXT);
            }
        } else {
            fileSize.setText(DEFAULT_FILESIZE_TEXT);
            resolution.setText(DEFAULT_RESOLUTION_TEXT);
            filePath.setText(DEFAULT_FILEPATH_TEXT);
        }
    }

    private static String bytesToPrettyString(long bytes) {
        if (bytes > 1024 * 1024 * 1024) return String.format("%.2f", bytes / 1024.0 / 1024 / 1024) + "GB";
        else if (bytes > 1024 * 1024) return String.format("%.2f", bytes / 1024.0 / 1024) + "MB";
        else if (bytes > 1024) return String.format("%.2f", bytes / 1024.0) + "KB";
        else return bytes + "B";
    }

}
