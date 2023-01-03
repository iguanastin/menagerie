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

package menagerie.gui.screens.dialogs;

import java.text.DecimalFormat;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import menagerie.gui.screens.Screen;
import menagerie.gui.screens.ScreenPane;
import menagerie.util.listeners.PokeListener;

public class ProgressScreen extends Screen {

  private final Label title;
  private final Label message;
  private final Label count;
  private final ProgressBar progress;

  private PokeListener cancelListener = null;

  private static final DecimalFormat df = new DecimalFormat("#.##");

  public ProgressScreen() {
    addEventHandler(KeyEvent.KEY_PRESSED, event -> {
      if (event.getCode() == KeyCode.ESCAPE) {
        close();
        event.consume();
      }
    });

    BorderPane root = new BorderPane();
    root.getStyleClass().addAll(ROOT_STYLE_CLASS);
    root.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
    root.setPrefWidth(400);
    setCenter(root);

    title = new Label("No Title");
    BorderPane.setAlignment(title, Pos.CENTER_LEFT);
    root.setTop(title);

    progress = new ProgressBar();
    progress.setPrefWidth(root.getPrefWidth());
    message = new Label("No Message");
    VBox center = new VBox(5, message, progress);
    center.setPadding(new Insets(5));
    root.setCenter(center);

    BorderPane bottom = new BorderPane();
    setMargin(bottom, new Insets(5));
    Button cancel = new Button("Cancel");
    cancel.setOnAction(event -> {
      close();
      event.consume();
    });
    bottom.setRight(cancel);
    count = new Label("0/0");
    count.setPadding(new Insets(5));
    bottom.setLeft(count);
    root.setBottom(bottom);

    setDefaultFocusNode(cancel);
  }

  /**
   * Opens this screen in a manager.
   *
   * @param manager        Manager to open this screen.
   * @param titleText      Title bar text.
   * @param messageText    Message text.
   * @param cancelListener Listener for the cancel event.
   */
  public void open(ScreenPane manager, String titleText, String messageText,
                   PokeListener cancelListener) {
    this.cancelListener = null;
    manager.open(this);

    this.cancelListener = cancelListener;

    title.setText(titleText);
    message.setText(messageText);
    setProgress(0, 0);
  }

  /**
   * Sets the progress.
   *
   * @param i     Number of complete operations.
   * @param total Number of total operations.
   */
  public void setProgress(int i, int total) {
    count.setText(df.format(100.0 * i / total) + "%");
    progress.setProgress((double) i / total);
  }

  public void setProgress(double progress) {
    count.setText(progress > 0 ? df.format(progress * 100) + "%" : "Working...");
    this.progress.setProgress(progress);
  }

  @Override
  protected void onClose() {
    if (cancelListener != null) {
      cancelListener.poke();
    }
  }

}
