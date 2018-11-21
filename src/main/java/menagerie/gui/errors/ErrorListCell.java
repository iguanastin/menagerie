package menagerie.gui.errors;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ErrorListCell extends ListCell<TrackedError> {

    private final BorderPane root;
    private final VBox bottomVBox;
    private final Label titleLabel;
    private final Label whatHappenedLabel;
    private final Label timeLabel;
    private final Label causeLabel;
    private final Label severityLabel;
    private final Label exceptionLabel;
    private final Button expandButton;


    public ErrorListCell(ErrorListCellDismissListener dismissListener) {
        titleLabel = new Label("N/A");

        whatHappenedLabel = new Label("N/A");
        whatHappenedLabel.setWrapText(true);
        whatHappenedLabel.setPadding(new Insets(0, 0, 0, 20));

        timeLabel = new Label("N/A");
        timeLabel.setPadding(new Insets(0, 0, 0, 20));

        causeLabel = new Label("N/A");
        causeLabel.setWrapText(true);
        causeLabel.setPadding(new Insets(0, 0, 0, 20));

        exceptionLabel = new Label("N/A");
        exceptionLabel.setWrapText(true);
        exceptionLabel.setPadding(new Insets(0, 0, 0, 20));

        severityLabel = new Label("N/A");

        expandButton = new Button("+");
        expandButton.setOnAction(event -> {
            if (getItem() != null) {
                getItem().setShowExpanded(!getItem().isShowExpanded());
            }
            updateExpanded();
        });

        Button dismissButton = new Button("Dismiss");
        dismissButton.setOnAction(event -> {
            if (dismissListener != null && getItem() != null) dismissListener.dismiss(getItem());
        });

        Label l1 = new Label("What happened:");
        l1.setStyle("-fx-text-fill: -fx-mid-text-color;");
        Label l2 = new Label("When:");
        l2.setStyle("-fx-text-fill: -fx-mid-text-color;");
        Label l3 = new Label("Likely cause:");
        l3.setStyle("-fx-text-fill: -fx-mid-text-color;");
        Label l4 = new Label("Stack Trace:");
        l4.setStyle("-fx-text-fill: -fx-mid-text-color;");
        bottomVBox = new VBox(new Separator(), l1, whatHappenedLabel, new Separator(), l2, timeLabel, new Separator(), l3, causeLabel, new Separator(), l4, exceptionLabel);

        HBox hbox = new HBox(5, expandButton, titleLabel);
        hbox.setAlignment(Pos.CENTER_LEFT);
        root = new BorderPane(severityLabel, null, dismissButton, null, hbox);
    }

    @Override
    protected void updateItem(TrackedError item, boolean empty) {
        super.updateItem(item, empty);

        if (!empty) {
            setGraphic(root);

            updateExpanded();

            titleLabel.setText(item.getTitle());

            whatHappenedLabel.setText(item.getWhatHappened());

            timeLabel.setText(item.getDate().toString());

            causeLabel.setText(item.getLikelyCause());

            if (item.getException() != null) {
                StringWriter stacktraceStringWriter = new StringWriter();
                PrintWriter pw = new PrintWriter(stacktraceStringWriter);
                item.getException().printStackTrace(pw);
                exceptionLabel.setText(stacktraceStringWriter.toString());
            } else {
                exceptionLabel.setText(null);
            }

            switch (item.getSeverity()) {
                case HIGH:
                    severityLabel.setText("[SEVERE]");
                    severityLabel.setStyle("-fx-text-fill: red;");
                    break;
                case NORMAL:
                    severityLabel.setText("[MODERATE]");
                    severityLabel.setStyle("-fx-text-fill: yellow;");
                    break;
            }
        } else {
            setGraphic(null);
        }
    }

    private void updateExpanded() {
        if (getItem() != null && getItem().isShowExpanded()) {
            expandButton.setText("-");
            root.setBottom(bottomVBox);
        } else {
            expandButton.setText("+");
            root.setBottom(null);
        }
    }

}
