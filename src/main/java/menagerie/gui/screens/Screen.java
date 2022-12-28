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

package menagerie.gui.screens;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

/**
 * Screen object that can be opened and managed in the ScreenPane.
 */
public abstract class Screen extends BorderPane {

  public static final String ROOT_STYLE_CLASS = "screen-root";

  private ScreenPane manager = null;

  private Node defaultFocusNode = this;


  /**
   * Closes this screen if it is open in a manager.
   */
  public void close() {
    if (manager == null) {
      return;
    }

    manager.close(this);
  }

  /**
   * Sets the manager of this screen.
   *
   * @param manager Manager.
   */
  void setManager(ScreenPane manager) {
    this.manager = manager;
  }

  /**
   * @return The manager of this screen, or null if the screen is not open.
   */
  public ScreenPane getManager() {
    return manager;
  }

  /**
   * @param defaultFocusNode Node to be focused on open.
   */
  public void setDefaultFocusNode(Node defaultFocusNode) {
    this.defaultFocusNode = defaultFocusNode;
  }

  /**
   * Attempts to focus the default focus node.
   */
  void focusDefaultNode() {
    defaultFocusNode.requestFocus();
  }

  /**
   * Method that is called after a screen has been opened.
   */
  protected void onOpen() {
  }

  /**
   * Method that is called after a screen has been closed.
   */
  protected void onClose() {
  }

}
