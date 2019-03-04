package menagerie.gui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import menagerie.model.menagerie.ImageInfo;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ItemInfoBox extends VBox {

    private static final String DEFAULT_ID_TEXT = "ID: N/A";
    private static final String DEFAULT_DATE_TEXT = "N/A";
    private static final String DEFAULT_FILESIZE_TEXT = "0B";
    private static final String DEFAULT_RESOLUTION_TEXT = "0x0";
    private static final String DEFAULT_FILEPATH_TEXT = "N/A";

    private Label idLabel = new Label(DEFAULT_ID_TEXT);
    private Label dateLabel = new Label(DEFAULT_DATE_TEXT);
    private Label fileSizeLabel = new Label(DEFAULT_FILESIZE_TEXT);
    private Label resolutionLabel = new Label(DEFAULT_RESOLUTION_TEXT);
    private Label filePathLabel = new Label(DEFAULT_FILEPATH_TEXT);

    private boolean extended = false;


    public ItemInfoBox() {
        setPadding(new Insets(5));
        setStyle("-fx-background-color: -fx-base;");
        setSpacing(2);

        addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            setExtended(!isExtended());
        });

        getChildren().addAll(resolutionLabel, fileSizeLabel);
    }

    public boolean isExtended() {
        return extended;
    }

    public void setExtended(boolean b) {
        if (b == extended) return;

        extended = b;
        if (extended) {
            getChildren().addAll(idLabel, dateLabel, filePathLabel);
        } else {
            getChildren().removeAll(idLabel, dateLabel, filePathLabel);
        }
    }

    public void setItem(ImageInfo item) {
        if (item != null) {
            idLabel.setText("ID: " + item.getId());
            dateLabel.setText(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault()).format(new Date(item.getDateAdded()).toInstant()));
            fileSizeLabel.setText(bytesToPrettyString(item.getFile().length()));
            filePathLabel.setText(item.getFile().toString());
            if (item.isImage()) { //TODO: Support for video resolution
                if (item.getImage().isBackgroundLoading() && item.getImage().getProgress() != 1) {
                    resolutionLabel.setText("Loading...");
                    item.getImage().progressProperty().addListener((observable, oldValue, newValue) -> {
                        if (newValue.doubleValue() == 1 && !item.getImage().isError())
                            resolutionLabel.setText((int) item.getImage().getWidth() + "x" + (int) item.getImage().getHeight());
                    });
                } else {
                    resolutionLabel.setText((int) item.getImage().getWidth() + "x" + (int) item.getImage().getHeight());
                }
            } else {
                resolutionLabel.setText(DEFAULT_RESOLUTION_TEXT);
            }
        } else {
            idLabel.setText(DEFAULT_ID_TEXT);
            dateLabel.setText(DEFAULT_DATE_TEXT);
            fileSizeLabel.setText(DEFAULT_FILESIZE_TEXT);
            resolutionLabel.setText(DEFAULT_RESOLUTION_TEXT);
            filePathLabel.setText(DEFAULT_FILEPATH_TEXT);
        }
    }

    private static String bytesToPrettyString(long bytes) {
        if (bytes > 1024 * 1024 * 1024) return String.format("%.2f", bytes / 1024.0 / 1024 / 1024) + "GB";
        else if (bytes > 1024 * 1024) return String.format("%.2f", bytes / 1024.0 / 1024) + "MB";
        else if (bytes > 1024) return String.format("%.2f", bytes / 1024.0) + "KB";
        else return bytes + "B";
    }

}
