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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileMoveNodeCell extends VBox {

    private static final Background PRESERVED_BACKGROUND = new Background(new BackgroundFill(new Color(0, 111.0 / 255, 128.0 / 255, 1), null, null));
    private static final Background COLLAPSED_BACKGROUND = new Background(new BackgroundFill(new Color(128.0 / 255, 119.0 / 255, 0, 1), null, null));
    private static final Insets INDENT = new Insets(0, 0, 0, 10);
    private static final Border BORDER = new Border(new BorderStroke(Color.BLACK, null, null, Color.BLACK, BorderStrokeStyle.SOLID, BorderStrokeStyle.NONE, BorderStrokeStyle.NONE, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT, null));

    private final BooleanProperty expanded = new SimpleBooleanProperty(true);
    private final Button expandButton = new Button("-");
    private Label itemsLabel = null;
    private final List<FileMoveNodeCell> subNodes = new ArrayList<>();

    private final FileMoveNode node;


    public FileMoveNodeCell(FileMoveNode node) {
        this.node = node;
        node.preserveProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                setBackground(PRESERVED_BACKGROUND);
            } else if (!isExpanded()) {
                setBackground(COLLAPSED_BACKGROUND);
            } else {
                setBackground(null);
            }
        });

        expandButton.setPadding(new Insets(0, 3, 0, 3));
        expandButton.setOnAction(event -> setExpanded(!isExpanded()));
        String folderName;
        if (node.isRoot()) {
            folderName = node.getFolder().getAbsolutePath();
        } else {
            folderName = node.getFolder().getName() + File.separatorChar;
        }
        Label titleLabel = new Label();
        titleLabel.setText(folderName);
        titleLabel.setTooltip(new Tooltip(node.getFolder().getAbsolutePath()));
        HBox titleBox = new HBox(5, expandButton, titleLabel);
        titleBox.setOnMouseClicked(event -> {
            node.setPreserve(!node.isPreserved());
            event.consume();
        });
        getChildren().add(titleBox);

        if (node.getItems().size() > 0) {
            itemsLabel = new Label(node.getItems().size() + " files");
            itemsLabel.setBackground(PRESERVED_BACKGROUND);
            itemsLabel.setPadding(new Insets(0, 3, 0, 3));
            VBox.setMargin(itemsLabel, INDENT);
            getChildren().add(itemsLabel);
        }

        for (FileMoveNode subNode : node.getNodes()) {
            FileMoveNodeCell newNode = new FileMoveNodeCell(subNode);
            subNodes.add(newNode);
            getChildren().add(newNode);
            VBox.setMargin(newNode, new Insets(0, 0, 0, 10));
        }

        setBorder(BORDER);
    }

    public FileMoveNode getNode() {
        return node;
    }

    public boolean isExpanded() {
        return expanded.get();
    }

    public BooleanProperty expandedProperty() {
        return expanded;
    }

    public void setExpanded(boolean b) {
        expanded.set(b);

        if (b) {
            expandButton.setText("-");
            if (itemsLabel != null) getChildren().add(itemsLabel);
            getChildren().addAll(subNodes);

            if (!node.isPreserved()) {
                setBackground(null);
            }
        } else {
            expandButton.setText("+");
            if (itemsLabel != null) getChildren().remove(itemsLabel);
            getChildren().removeAll(subNodes);

            if (!node.isPreserved()) {
                setBackground(COLLAPSED_BACKGROUND);
            }
        }
    }

}
