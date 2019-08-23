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

package menagerie.gui.screens.move;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import menagerie.gui.screens.Screen;
import menagerie.gui.screens.ScreenPane;
import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MoveFilesScreen extends Screen {

    private final TextField folderTextField = new TextField();
    private final Button folderBrowseButton = new Button("Browse");
    private final ListView<String> preTree = new ListView<>();
    private final ListView<String> postTree = new ListView<>();
    private final Button move = new Button("Move");
    private final Button cancel = new Button("Cancel");

    private List<MediaItem> toMove = null;


    public MoveFilesScreen() {
        addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                close();
            }
        });

        //Init "Window"
        VBox v = new VBox(5);
        v.setStyle("-fx-background-color: -fx-base;");
        v.setPadding(new Insets(5));
        DropShadow dropShadow = new DropShadow();
        dropShadow.setSpread(0.5);
        v.setEffect(dropShadow);
        setCenter(v);
        setMargin(v, new Insets(100));

        //Init header
        Button exitButton = new Button("X");
        exitButton.setOnAction(event -> close());
        BorderPane header = new BorderPane(null, null, exitButton, null, new Label("Move Files..."));

        HBox.setHgrow(folderTextField, Priority.ALWAYS);
        folderBrowseButton.setOnAction(event -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Move Files to Folder...");
            File initial = new File(folderTextField.getText());
            if (initial.exists() && initial.isDirectory()) dc.setInitialDirectory(initial);
            File target = dc.showDialog(getScene().getWindow());

            if (target != null) {
                folderTextField.setText(target.getAbsolutePath());
            }
        });
        folderTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            // TODO Update tree view
        });
        HBox folderBox = new HBox(5, new Label("Move To:"), folderTextField, folderBrowseButton);

        HBox.setHgrow(preTree, Priority.ALWAYS);
        HBox.setHgrow(postTree, Priority.ALWAYS);
        HBox treeBox = new HBox(5, preTree, postTree);
        VBox.setVgrow(treeBox, Priority.ALWAYS);

        move.setOnAction(event -> move());
        cancel.setOnAction(event -> close());
        HBox footer = new HBox(5, move, cancel);
        footer.setAlignment(Pos.CENTER_RIGHT);

        //Add children
        v.getChildren().addAll(header, new Separator(), folderBox, treeBox, footer);

        setDefaultFocusNode(cancel);
    }

    public void open(ScreenPane manager, List<Item> toMove) {
        manager.open(this);

        this.toMove = new ArrayList<>();
        toMove.forEach(item -> {
            if (item instanceof GroupItem) {
                this.toMove.addAll(((GroupItem) item).getElements());
            } else if (item instanceof MediaItem) {
                this.toMove.add((MediaItem) item);
            }
        });

        initPreTree();
    }

    private void initPreTree() {
        preTree.getItems().clear();

        FileMoveTree tree = new FileMoveTree(toMove);

        System.out.println("PreTree");
        tree.getRoots().forEach(fileMoveNode -> p(fileMoveNode, "  "));
    }

    private void p(FileMoveNode node, String prefix) {
        System.out.println(prefix + node.getFolder());
        System.out.println(prefix + "  " + node.getItems().size());

        for (FileMoveNode n : node.getNodes()) {
            p(n, prefix + "  ");
        }
    }

    private void move() {
        // TODO
    }

}
