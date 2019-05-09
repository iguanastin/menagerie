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
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import menagerie.gui.screens.Screen;
import menagerie.gui.screens.ScreenPane;
import menagerie.util.listeners.PokeListener;

public class AlertDialogScreen extends Screen {

    private final Label titleLabel = new Label();
    private final Label messageLabel = new Label();

    private PokeListener closeListener = null;


    public AlertDialogScreen() {
        addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            switch (event.getCode()) {
                case ENTER:
                case ESCAPE:
                case SPACE:
                case BACK_SPACE:
                    close();
                    event.consume();
                    break;
            }
        });


        // Top
        Button exit = new Button("X");
        exit.setOnAction(event -> close());
        titleLabel.setPadding(new Insets(5));
        BorderPane top = new BorderPane(null, null, exit, new Separator(), titleLabel);

        // Center
        messageLabel.setPadding(new Insets(5));
        BorderPane.setAlignment(messageLabel, Pos.CENTER_LEFT);

        // Bottom
        Button ok = new Button("Ok");
        ok.setOnAction(event -> close());
        BorderPane.setAlignment(ok, Pos.BOTTOM_RIGHT);

        BorderPane root = new BorderPane(messageLabel, top, null, ok, null);
        root.setPrefWidth(500);
        root.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        DropShadow effect = new DropShadow();
        effect.setSpread(0.5);
        root.setEffect(effect);
        root.setStyle("-fx-background-color: -fx-base;");
        setCenter(root);
        setPadding(new Insets(25));

        setDefaultFocusNode(ok);
    }

    public void open(ScreenPane manager, String title, String message, PokeListener closeListener) {
        this.closeListener = closeListener;

        titleLabel.setText(title);
        messageLabel.setText(message);

        manager.open(this);
    }

    @Override
    protected void onClose() {
        if (closeListener != null) closeListener.poke();
    }

}
