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

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import menagerie.duplicates.DuplicateFinder;
import menagerie.duplicates.Match;
import menagerie.gui.Main;
import menagerie.gui.media.DynamicImageView;
import menagerie.gui.screens.Screen;
import menagerie.gui.screens.ScreenPane;
import menagerie.gui.screens.dialogs.AlertDialogScreen;
import menagerie.model.menagerie.MediaItem;
import org.controlsfx.control.GridView;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class FindOnlineScreen extends Screen {

    private final static Insets ALL5 = new Insets(5);

    private final GridView<Match> matchGridView = new GridView<>();
    private final DynamicImageView currentItemView = new DynamicImageView();

    private List<MediaItem> items = null;
    private List<DuplicateFinder> finders = null;
    private MediaItem currentItem = null;


    public FindOnlineScreen() {
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

        matchGridView.setCellFactory(param -> {
            MatchGridCell c = new MatchGridCell();
            c.setOnMouseClicked(event -> {
                try {
                    Desktop.getDesktop().browse(new URI(c.getItem().getPageURL()));
                } catch (IOException | URISyntaxException e) {
                    Main.log.log(Level.SEVERE, "Failed trying to open url in default browser", e);
                }
            });
            return c;
        });
        matchGridView.setCellWidth(156);
        matchGridView.setCellHeight(156);
        matchGridView.setHorizontalCellSpacing(3);
        matchGridView.setVerticalCellSpacing(3);
        SplitPane sp = new SplitPane(currentItemView, matchGridView);
        sp.setPadding(ALL5);
        root.setCenter(sp);
    }

    public void open(ScreenPane manager, List<MediaItem> items, List<DuplicateFinder> finders) {
        if (items == null || finders == null) {
            throw new NullPointerException("Must not be null");
        } else if (items.isEmpty()) {
            AlertDialogScreen a = new AlertDialogScreen();
            a.open(manager, "No items", "No valid items selected", null);
            return;
        } else if (finders.isEmpty()) {
            AlertDialogScreen a = new AlertDialogScreen();
            a.open(manager, "No finders", "No compatible plugins loaded, cannot search online", null);
            return;
        }

        manager.open(this);
        this.items = items;
        this.finders = finders;

        displayItem(items.get(0));
    }

    public void displayItem(MediaItem item) {
        setCurrentItem(item);
        matchGridView.getItems().clear();

        if (item != null) {
            currentItemView.setImage(item.getImage());

            Thread t = new Thread(() -> {
                List<Match> matches = new ArrayList<>();
                for (DuplicateFinder finder : finders) {
                    try {
                        matches.addAll(finder.getMatchesFor(item.getFile()));
                    } catch (IOException e) {
                        Main.log.log(Level.SEVERE, "Failed to get image info from web (1st try)", e);
                        try {
                            matches.addAll(finder.getMatchesFor(item.getFile()));
                        } catch (IOException ex) {
                            Main.log.log(Level.SEVERE, "Failed to get image info from web (2nd try, giving up)", ex);
                            Platform.runLater(() -> {
                                AlertDialogScreen a = new AlertDialogScreen();
                                a.open(getManager(), "Failed web get", "Repeatedly failed to get image info online", null);
                            });
                        }
                    }
                }

                if (getCurrentItem().equals(item)) {
                    Platform.runLater(() -> displayMatches(matches));
                }
            });
            t.setDaemon(true);
            t.start();
        }
    }

    private void displayMatches(List<Match> matches) {
        matchGridView.getItems().clear();
        matchGridView.getItems().addAll(matches);
    }

    public synchronized MediaItem getCurrentItem() {
        return currentItem;
    }

    public synchronized void setCurrentItem(MediaItem currentItem) {
        this.currentItem = currentItem;
    }

}
