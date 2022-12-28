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

package menagerie.gui.taglist;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import menagerie.util.listeners.ObjectListener;

public class SimpleCSSColorPicker extends HBox {

  protected static final String[] DEFAULT_COLORS =
      {"#609dff", "cyan", "#22e538", "yellow", "orange", "red", "#ff7ae6", "#bf51ff"};

  private ObjectListener<String> colorPickedListener = null;
  private final TextField textfield = new TextField();

  public SimpleCSSColorPicker() {
    this(DEFAULT_COLORS);
  }

  public SimpleCSSColorPicker(ObjectListener<String> colorPickedListener) {
    this(DEFAULT_COLORS, colorPickedListener);
  }

  public SimpleCSSColorPicker(String[] colors) {
    setSpacing(5);
    setPadding(new Insets(5));

    for (String css : colors) {
      Button b = new Button();
      b.prefWidthProperty().bind(b.prefHeightProperty());
      b.setOnAction(event -> confirmedColor(css));
      b.setStyle(String.format("-fx-base: %s;", css));
      getChildren().add(b);
    }
    Button b = new Button("Default");
    b.setOnAction(event -> confirmedColor(null));
    getChildren().add(b);

    textfield.setPromptText("Custom");
    textfield.setOnAction(event -> confirmedColor(textfield.getText()));
    getChildren().add(textfield);
  }

  public SimpleCSSColorPicker(String[] colors, ObjectListener<String> colorPickedListener) {
    this(colors);
    setColorPickedListener(colorPickedListener);
  }

  void setColorPickedListener(ObjectListener<String> colorPickedListener) {
    this.colorPickedListener = colorPickedListener;
  }

  private void confirmedColor(String css) {
      if (colorPickedListener != null) {
          colorPickedListener.pass(css);
      }
  }

  public TextField getTextfield() {
    return textfield;
  }

}
