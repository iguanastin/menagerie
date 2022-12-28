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

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import menagerie.gui.screens.Screen;
import menagerie.gui.screens.ScreenPane;
import menagerie.util.listeners.PokeListener;

public class ConfirmationScreen extends Screen {

  private final Label titleLabel;
  private final Label messageLabel;

  private PokeListener okListener = null;
  private PokeListener cancelListener = null;

  public ConfirmationScreen() {
    addEventHandler(KeyEvent.KEY_PRESSED, event -> {
      if (event.getCode() == KeyCode.ESCAPE || event.getCode() == KeyCode.BACK_SPACE) {
        close();
        event.consume();
      } else if (event.getCode() == KeyCode.ENTER) {
          if (okListener != null) {
              okListener.poke();
          }
        close();
        event.consume();
      }
    });

    titleLabel = new Label("TITLE");
    messageLabel = new Label("MESSAGE");
    messageLabel.setWrapText(true);

    Button ok = new Button("Ok");
    ok.setOnAction(event -> {
        if (okListener != null) {
            okListener.poke();
        }
      close();
    });
    Button cancel = new Button("Cancel");
    cancel.setOnAction(event -> {
        if (cancelListener != null) {
            cancelListener.poke();
        }
      close();
    });

    HBox h = new HBox(5, ok, cancel);
    h.setAlignment(Pos.CENTER_RIGHT);
    VBox v = new VBox(5, titleLabel, new Separator(), messageLabel, h);
    v.setPrefWidth(500);
    v.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
    v.setPadding(new Insets(5));
    v.getStyleClass().addAll(ROOT_STYLE_CLASS);
    setCenter(v);

    setDefaultFocusNode(ok);
  }

  /**
   * Opens this screen in a manager.
   *
   * @param manager        Manager to open in.
   * @param title          Title text.
   * @param message        Message text.
   * @param okListener     Listener waiting for confirm event.
   * @param cancelListener Listener waiting for cancel event.
   */
  public void open(ScreenPane manager, String title, String message, PokeListener okListener,
                   PokeListener cancelListener) {
    manager.open(this);

    titleLabel.setText(title);
    messageLabel.setText(message);
    this.okListener = okListener;
    this.cancelListener = cancelListener;
  }

}
