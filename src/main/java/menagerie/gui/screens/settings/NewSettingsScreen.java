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

package menagerie.gui.screens.settings;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import menagerie.gui.Main;
import menagerie.gui.screens.Screen;
import menagerie.gui.screens.ScreenPane;
import menagerie.settings.Setting;
import menagerie.settings.SettingNode;
import menagerie.settings.Settings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class NewSettingsScreen extends Screen {

    private static final Insets ALL5 = new Insets(5);

    private final ScrollPane scrollPane = new ScrollPane();

    private Settings settings = null;
    private final List<SettingNode> settingNodes = new ArrayList<>();


    public NewSettingsScreen() {

        addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                close();
            } else if (event.getCode() == KeyCode.ENTER) {
                accept();
            }
        });


        Button exit = new Button("X");
        exit.setFocusTraversable(false);
        exit.setOnAction(event -> close());
        BorderPane top = new BorderPane(null, null, exit, new Separator(), new Label("Settings"));
        top.setPadding(ALL5);

        Button accept = new Button("Accept");
        accept.setOnAction(event -> accept());
        Button cancel = new Button("Cancel");
        cancel.setOnAction(event -> close());
        HBox bottom = new HBox(5, accept, cancel);
        bottom.setPadding(ALL5);
        bottom.setAlignment(Pos.CENTER_RIGHT);

        scrollPane.setPadding(ALL5);
        BorderPane root = new BorderPane(scrollPane, top, null, bottom, null);
        root.setPrefSize(800, 600);
        root.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        DropShadow effect = new DropShadow();
        effect.setSpread(0.5);
        root.setEffect(effect);
        root.setStyle("-fx-background-color: -fx-base;");
        setCenter(root);
        setPadding(new Insets(25));

        setDefaultFocusNode(accept);
    }

    private void accept() {
        settingNodes.forEach(SettingNode::applyToSetting);
        try {
            settings.save();
        } catch (IOException e) {
            Main.log.log(Level.WARNING, "Failed to save settings file: " + settings.getFile(), e);
        }
        close();
    }

    public void open(ScreenPane manager, Settings settings) {
        this.settings = settings;

        manager.open(this);
    }

    @Override
    protected void onOpen() {
        settingNodes.clear();

        VBox root = new VBox();
        scrollPane.setContent(root);
        scrollPane.setFitToWidth(true);

        if (settings != null) {
            for (Setting setting : settings.getSettings()) {
                if (setting.isHidden()) continue;

                SettingNode node = setting.makeJFXNode();

                if (node != null) {
                    root.getChildren().add(node.getNode());
                    settingNodes.add(node);
                }
            }
        }
    }

}
