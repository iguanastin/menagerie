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

package menagerie.gui.screens.findonline;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import menagerie.duplicates.Match;
import menagerie.gui.media.PanZoomImageView;
import menagerie.gui.screens.Screen;
import menagerie.gui.screens.ScreenPane;
import menagerie.model.menagerie.MediaItem;

public class CompareToOnlineScreen extends Screen {

    private final static Insets ALL5 = new Insets(5);

    private final PanZoomImageView itemView = new PanZoomImageView();
    private final PanZoomImageView matchView = new PanZoomImageView();
    private final Label itemLabel = new Label();
    private final ProgressIndicator loadingIndicator = new ProgressIndicator(-1);

    private Image matchImage = null;

    private MediaItem currentItem = null;
    private Match currentMatch = null;


    public CompareToOnlineScreen() {
        addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                close();
            }
        });

        setPadding(new Insets(25));
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: -fx-base;");
        DropShadow effect = new DropShadow();
        effect.setSpread(0.5);
        root.setEffect(effect);
        setCenter(root);

        BorderPane header = new BorderPane();
        root.setTop(header);

        Label title = new Label("Find duplicates online");
        header.setLeft(title);
        BorderPane.setMargin(title, ALL5);

        Button exit = new Button("X");
        header.setRight(exit);
        exit.setOnAction(event -> close());

        header.setBottom(new Separator());

        VBox leftVBox = new VBox(5, new Label("Your image:"), itemView);
        StackPane.setAlignment(loadingIndicator, Pos.CENTER);
        loadingIndicator.setMaxSize(100, 100);
        VBox rightVBox = new VBox(5, new Label("Match:"), new StackPane(matchView, loadingIndicator));
        rightVBox.setAlignment(Pos.TOP_RIGHT);
        SplitPane sp = new SplitPane(leftVBox, rightVBox);
        root.setCenter(sp);
    }

    public void open(ScreenPane manager, MediaItem item, Match match) {
        if (item == null || match == null) {
            throw new NullPointerException("Must not be null");
        }

        if (matchImage != null) {
            matchImage.cancel();
            matchImage = null;
        }

        manager.open(this);
        this.currentItem = item;
        this.currentMatch = match;

        itemView.setImage(item.getImage());
        matchView.setImage(null);
        if (match.getImageURL() != null) {
            loadingIndicator.setDisable(false);
            loadingIndicator.setOpacity(1);

            matchImage = new Image(match.getImageURL(), true);
            matchImage.progressProperty().addListener((observable, oldValue, newValue) -> {
                if (!matchImage.isError() && newValue.doubleValue() == 1) {
                    matchView.setImage(matchImage);
                    loadingIndicator.setDisable(true);
                    loadingIndicator.setOpacity(0);
                }
            });
        }
    }

}
