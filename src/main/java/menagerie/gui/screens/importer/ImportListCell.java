/*
 MIT License

 Copyright (c) 2019. Austin Thompson

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */

package menagerie.gui.screens.importer;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import menagerie.model.menagerie.MediaItem;
import menagerie.model.menagerie.importer.ImportJob;
import menagerie.util.listeners.ObjectListener;


public class ImportListCell extends ListCell<ImportJob> {

  private final ChangeListener<Number> progressChangeListener;

  private final Label waitingLabel;
  private final BorderPane waitingView;

  private final Label importingLabel;
  private final ProgressBar importingProgressBar;
  private final BorderPane importingView;

  private final Label duplicateLabel;
  private final BorderPane duplicateView;

  private final Label failedLabel;
  private final BorderPane failedView;

  private final ChangeListener<ImportJob.Status> statusListener =
      (observable, oldValue, newValue) -> Platform.runLater(() -> {
        if (getItem() != null) {
          updateView(getItem().getStatus());
        } else {
          setGraphic(null);
        }
      });


  ImportListCell(ImporterScreen screen, ObjectListener<MediaItem> selectItemListener) {
    super();

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
    importingView =
        new BorderPane(new Label("Importing..."), importingLabel, null, importingProgressBar, null);
    importingLabel.maxWidthProperty().bind(importingView.widthProperty().subtract(10));


    duplicateLabel = new Label("N/A");
    duplicateLabel.setTextOverrun(OverrunStyle.CENTER_WORD_ELLIPSIS);
    Button dismissButton = new Button("Dismiss");
    EventHandler<ActionEvent> dismissEventHandler = event -> screen.removeJob(getItem());
    dismissButton.setOnAction(dismissEventHandler);
    Button showDuplicateButton = new Button("View");
    showDuplicateButton.setOnAction(event -> {
      selectItemListener.pass(getItem().getDuplicateOf());
      screen.removeJob(getItem());
    });
    HBox h = new HBox(5, showDuplicateButton, dismissButton);
    h.setAlignment(Pos.CENTER_RIGHT);
    duplicateView =
        new BorderPane(new Label("Failed - Exact duplicate already present"), duplicateLabel, null,
            h, null);
    duplicateLabel.maxWidthProperty().bind(duplicateView.widthProperty().subtract(10));

    failedLabel = new Label("N/A");
    failedLabel.setTextOverrun(OverrunStyle.CENTER_WORD_ELLIPSIS);
    dismissButton = new Button("Dismiss");
    dismissButton.setOnAction(dismissEventHandler);
    h = new HBox(5, dismissButton);
    h.setAlignment(Pos.CENTER_RIGHT);
    failedView = new BorderPane(new Label("Import failed"), failedLabel, null, h, null);
    failedLabel.maxWidthProperty().bind(failedView.widthProperty().subtract(10));

    progressChangeListener = (observable, oldValue, newValue) -> Platform.runLater(
        () -> importingProgressBar.setProgress(newValue.doubleValue()));
  }

  @Override
  protected void updateItem(ImportJob item, boolean empty) {
    if (getItem() != null) {
      getItem().progressProperty().removeListener(progressChangeListener);
      getItem().statusProperty().removeListener(statusListener);
    }

    super.updateItem(item, empty);

    if (item != null) {
      item.progressProperty().addListener(progressChangeListener);
      item.statusProperty().addListener(statusListener);

      if (item.getUrl() != null) {
        setTooltip(new Tooltip(item.getUrl().toString()));
      } else {
        setTooltip(new Tooltip(item.getFile().toString()));
      }

      updateView(item.getStatus());
    } else {
      setGraphic(null);
    }
  }

  private void updateView(ImportJob.Status status) {
    switch (status) {
      case WAITING:
        showWaitingView();
        break;
      case IMPORTING:
        showImportingView();
        break;
      case FAILED_DUPLICATE:
        showDuplicateView();
        break;
      case FAILED_IMPORT:
        showFailedView();
        break;
      default:
        setGraphic(null);
        break;
    }
  }

  /**
   * Makes this cell show an importing view.
   */
  private void showImportingView() {
    setGraphic(importingView);
    if (getItem() != null) {
      importingProgressBar.setProgress(getItem().getProgress());
      if (getItem().getUrl() != null) {
        importingLabel.setText(getItem().getUrl().toString());
      } else {
        importingLabel.setText(getItem().getFile().toString());
      }
    }
  }

  /**
   * Makes this cell show a waiting view.
   */
  private void showWaitingView() {
    setGraphic(waitingView);
    if (getItem() != null) {
      if (getItem().getUrl() != null) {
        waitingLabel.setText(getItem().getUrl().toString());
      } else {
        waitingLabel.setText(getItem().getFile().toString());
      }
    }
  }

  /**
   * Makes this cell show a hasDuplicate view.
   */
  private void showDuplicateView() {
    setGraphic(duplicateView);
    if (getItem() != null) {
      if (getItem().getUrl() != null) {
        duplicateLabel.setText(getItem().getUrl().toString());
      } else {
        duplicateLabel.setText(getItem().getFile().toString());
      }
    }
  }

  /**
   * Makes this cell show an error view.
   */
  private void showFailedView() {
    setGraphic(failedView);
    if (getItem() != null) {
      if (getItem().getUrl() != null) {
        failedLabel.setText(getItem().getUrl().toString());
      } else {
        failedLabel.setText(getItem().getFile().toString());
      }
    }
  }

}
