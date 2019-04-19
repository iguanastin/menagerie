package menagerie.gui.screens.importer;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import menagerie.model.SimilarPair;
import menagerie.model.menagerie.MediaItem;
import menagerie.model.menagerie.importer.ImportJob;
import menagerie.util.listeners.ObjectListener;

import java.util.List;


public class ImportListCell extends ListCell<ImportJob> {

    private final ImporterScreen screen;

    private final ChangeListener<Number> progressChangeListener;

    private final Label waitingLabel;
    private final BorderPane waitingView;

    private final Label importingLabel;
    private final ProgressBar importingProgressBar;
    private final BorderPane importingView;

    private final Label hasSimilarLabel;
    private final Button hasSimilarResolveButton;
    private final BorderPane hasSimilarView;

    private final Label duplicateLabel;
    private final BorderPane duplicateView;

    private final Label failedLabel;
    private final BorderPane failedView;


    ImportListCell(ImporterScreen screen, ObjectListener<List<SimilarPair<MediaItem>>> duplicateResolverListener, ObjectListener<MediaItem> selectItemListener) {
        super();
        this.screen = screen;

        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(event -> {
            getItem().cancel();
            screen.removeJob(getItem());
        });
        BorderPane.setAlignment(cancelButton, Pos.CENTER_RIGHT);
        waitingLabel = new Label("N/A");
        waitingLabel.setTextOverrun(OverrunStyle.CENTER_WORD_ELLIPSIS);
        waitingView = new BorderPane(new Label("Waiting..."), waitingLabel, null, cancelButton, null);
        waitingLabel.maxWidthProperty().bind(waitingView.widthProperty().subtract(10));

        importingLabel = new Label("N/A");
        importingLabel.setTextOverrun(OverrunStyle.CENTER_WORD_ELLIPSIS);
        importingProgressBar = new ProgressBar();
        importingView = new BorderPane(new Label("Importing..."), importingLabel, null, importingProgressBar, null);
        importingLabel.maxWidthProperty().bind(importingView.widthProperty().subtract(10));

        hasSimilarLabel = new Label("N/A");
        hasSimilarLabel.setTextOverrun(OverrunStyle.CENTER_WORD_ELLIPSIS);
        hasSimilarResolveButton = new Button("Resolve");
        hasSimilarResolveButton.setOnAction(event -> {
            duplicateResolverListener.pass(getItem().getSimilarTo());
            screen.removeJob(getItem());
        });
        Button dismissButton = new Button("Dismiss");
        EventHandler<ActionEvent> dismissEventHandler = event -> screen.removeJob(getItem());
        dismissButton.setOnAction(dismissEventHandler);
        HBox h = new HBox(5, hasSimilarResolveButton, dismissButton);
        h.setAlignment(Pos.CENTER_RIGHT);
        hasSimilarView = new BorderPane(new Label("Successfully imported - Has potential duplicates"), hasSimilarLabel, null, h, null);
        hasSimilarLabel.maxWidthProperty().bind(hasSimilarView.widthProperty().subtract(10));

        duplicateLabel = new Label("N/A");
        duplicateLabel.setTextOverrun(OverrunStyle.CENTER_WORD_ELLIPSIS);
        dismissButton = new Button("Dismiss");
        dismissButton.setOnAction(dismissEventHandler);
        Button showDuplicateButton = new Button("View");
        showDuplicateButton.setOnAction(event -> {
            selectItemListener.pass(getItem().getDuplicateOf());
            screen.removeJob(getItem());
        });
        h = new HBox(5, showDuplicateButton, dismissButton);
        h.setAlignment(Pos.CENTER_RIGHT);
        duplicateView = new BorderPane(new Label("Failed - Exact duplicate already present"), duplicateLabel, null, h, null);
        duplicateLabel.maxWidthProperty().bind(duplicateView.widthProperty().subtract(10));

        failedLabel = new Label("N/A");
        failedLabel.setTextOverrun(OverrunStyle.CENTER_WORD_ELLIPSIS);
        dismissButton = new Button("Dismiss");
        dismissButton.setOnAction(dismissEventHandler);
        h = new HBox(5, dismissButton);
        h.setAlignment(Pos.CENTER_RIGHT);
        failedView = new BorderPane(new Label("Import failed"), failedLabel, null, h, null);
        failedLabel.maxWidthProperty().bind(failedView.widthProperty().subtract(10));

        progressChangeListener = (observable, oldValue, newValue) -> Platform.runLater(() -> importingProgressBar.setProgress(newValue.doubleValue()));
    }

    @Override
    protected void updateItem(ImportJob item, boolean empty) {
        if (getItem() != null) {
            getItem().getProgressProperty().removeListener(progressChangeListener);
        }

        super.updateItem(item, empty);

        if (item != null) {
            item.getProgressProperty().addListener(progressChangeListener);

            if (item.getUrl() != null) setTooltip(new Tooltip(item.getUrl().toString()));
            else setTooltip(new Tooltip(item.getFile().toString()));

            switch (item.getStatus()) {
                case WAITING:
                    showWaitingView();
                    break;
                case IMPORTING:
                    showImportingView();
                    break;
                case SUCCEEDED:
                    screen.removeJob(getItem());
                    break;
                case SUCCEEDED_SIMILAR:
                    showHasSimilarView();
                    break;
                case FAILED_DUPLICATE:
                    showDuplicateView();
                    break;
                case FAILED_IMPORT:
                    showFailedView();
                    break;
            }
        } else {
            setGraphic(null);
        }
    }

    /**
     * Makes this cell show an importing view.
     */
    private void showImportingView() {
        setGraphic(importingView);
        if (getItem() != null) {
            importingProgressBar.setProgress(getItem().getProgress());
            if (getItem().getUrl() != null) importingLabel.setText(getItem().getUrl().toString());
            else importingLabel.setText(getItem().getFile().toString());
        }
    }

    /**
     * Makes this cell show a waiting view.
     */
    private void showWaitingView() {
        setGraphic(waitingView);
        if (getItem() != null) {
            if (getItem().getUrl() != null) waitingLabel.setText(getItem().getUrl().toString());
            else waitingLabel.setText(getItem().getFile().toString());
        }
    }

    /**
     * Makes this cell show a hasSimilar view.
     */
    private void showHasSimilarView() {
        setGraphic(hasSimilarView);
        if (getItem() != null) {
            hasSimilarResolveButton.setText("Resolve: " + getItem().getSimilarTo().size());
            if (getItem().getUrl() != null) hasSimilarLabel.setText(getItem().getUrl().toString());
            else hasSimilarLabel.setText(getItem().getFile().toString());
        }
    }

    /**
     * Makes this cell show a hasDuplicate view.
     */
    private void showDuplicateView() {
        setGraphic(duplicateView);
        if (getItem() != null) {
            if (getItem().getUrl() != null) duplicateLabel.setText(getItem().getUrl().toString());
            else duplicateLabel.setText(getItem().getFile().toString());
        }
    }

    /**
     * Makes this cell show an error view.
     */
    private void showFailedView() {
        setGraphic(failedView);
        if (getItem() != null) {
            if (getItem().getUrl() != null) failedLabel.setText(getItem().getUrl().toString());
            else failedLabel.setText(getItem().getFile().toString());
        }
    }

}
