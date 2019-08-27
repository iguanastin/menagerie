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
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.io.File;

public class FileMoveNodeCell extends VBox {

    private static final Background PRESERVED_BACKGROUND = new Background(new BackgroundFill(new Color(0, 111.0 / 255, 128.0 / 255, 1), null, null));
    private static final Insets INDENT = new Insets(0, 0, 0, 10);

    private final FileMoveNode node;


    public FileMoveNodeCell(FileMoveNode node) {
        this.node = node;
        node.preserveProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                setBackground(PRESERVED_BACKGROUND);
            } else {
                setBackground(null);
            }
        });

        String nameLabelText;
        if (node.isRoot()) {
            nameLabelText = node.getFolder().getAbsolutePath();
        } else {
            nameLabelText = node.getFolder().getName() + File.separatorChar;
        }
        Label nameLabel = new Label(nameLabelText);
        nameLabel.setTooltip(new Tooltip(node.getFolder().getAbsolutePath()));
        getChildren().add(nameLabel);

        if (node.getItems().size() > 0) {
            Label itemsLabel = new Label(node.getItems().size() + " files");
            itemsLabel.setBackground(PRESERVED_BACKGROUND);
            itemsLabel.setPadding(new Insets(0, 3, 0, 3));
            getChildren().add(itemsLabel);
            VBox.setMargin(itemsLabel, INDENT);
        }

        for (FileMoveNode subNode : node.getNodes()) {
            FileMoveNodeCell newNode = new FileMoveNodeCell(subNode);
            getChildren().add(newNode);
            VBox.setMargin(newNode, new Insets(0, 0, 0, 10));
        }

        setOnMouseClicked(event -> {
            node.setPreserve(!node.isPreserved());
            event.consume();
        });
    }

    public FileMoveNode getNode() {
        return node;
    }

}
