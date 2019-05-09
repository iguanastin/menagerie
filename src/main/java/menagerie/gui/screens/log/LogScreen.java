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

package menagerie.gui.screens.log;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import menagerie.gui.screens.Screen;

public class LogScreen extends Screen {

    private final ListView<LogItem> listView;


    public LogScreen() {
        addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                close();
                event.consume();
            } else if (event.getCode() == KeyCode.L && event.isControlDown()) {
                close();
                event.consume();
            }
        });


        Button exit = new Button("X");
        exit.setOnAction(event -> close());
        Label title = new Label("Console log:");
        setAlignment(title, Pos.CENTER_LEFT);
        BorderPane top = new BorderPane(null, null, exit, null, title);
        top.setPadding(new Insets(0, 0, 0, 5));

        listView = new ListView<>();
        listView.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                close();
                event.consume();
            } else if (event.getCode() == KeyCode.L && event.isControlDown()) {
                close();
                event.consume();
            }
        });

        BorderPane root = new BorderPane(listView, top, null, null, null);
        root.setPrefWidth(800);
        root.setStyle("-fx-background-color: -fx-base;");
        DropShadow effect = new DropShadow();
        effect.setSpread(0.5);
        root.setEffect(effect);
        root.setMaxWidth(USE_PREF_SIZE);
        setRight(root);
        setPadding(new Insets(25));

        setDefaultFocusNode(exit);
    }

    /**
     * @return The ListView in this screen.
     */
    public ListView<LogItem> getListView() {
        return listView;
    }

    @Override
    protected void onOpen() {
        listView.scrollTo(listView.getItems().size() - 1);
    }

}
