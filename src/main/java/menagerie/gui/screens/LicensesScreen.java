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

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import menagerie.gui.Main;
import menagerie.model.Settings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;

public class LicensesScreen extends Screen {

    private final Button agree = new Button("Agree");

    private static final Insets ALL5 = new Insets(5);

    private int index = 0;

    public LicensesScreen(Settings settings, File licenseFolder, int targetLicenseVersion) {
        BorderPane top = new BorderPane(null, null, null, new Separator(), new Label("License Agreement"));
        top.setPadding(ALL5);

        File[] licenses = licenseFolder.listFiles(File::isFile);
        TextField titleField = new TextField();
        titleField.setEditable(false);
        titleField.setFocusTraversable(false);
        TextArea textArea = new TextArea();
        VBox.setVgrow(textArea, Priority.ALWAYS);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setFocusTraversable(false);
        HBox h = new HBox(5, new Label("License:"), titleField);
        h.setAlignment(Pos.CENTER_LEFT);
        VBox center = new VBox(5, h, textArea);
        center.setPadding(ALL5);
        if (licenses != null && licenses.length > 0) {
            try {
                titleField.setText(licenses[0].getName());
                textArea.setText(String.join("\n", Files.readAllLines(licenses[0].toPath())));
            } catch (IOException e) {
                Main.log.log(Level.SEVERE, "Unable to read license file: " + licenses[0], e);
                System.exit(1);
            }
        }

        agree.setOnAction(event -> {
            index++;
            if (licenses != null && index < licenses.length) {
                try {
                    titleField.setText(licenses[index].getName());
                    textArea.setText(String.join("\n", Files.readAllLines(licenses[index].toPath())));
                } catch (IOException e) {
                    Main.log.log(Level.SEVERE, "Unable to read license file: " + licenses[index], e);
                    System.exit(1);
                }
            } else {
                close();
                if (licenses != null && licenses.length > 0)
                    settings.setInt(Settings.Key.LICENSES_AGREED, targetLicenseVersion);
            }
        });
        Button cancel = new Button("Cancel");
        cancel.setOnAction(event -> System.exit(0));
        BorderPane bottom = new BorderPane(null, null, new HBox(5, agree, cancel), null, null);
        bottom.setPadding(ALL5);

        BorderPane root = new BorderPane(center, top, null, bottom, null);
        root.setStyle("-fx-background-color: -fx-base;");
        root.setPrefSize(800, 800);
        root.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        DropShadow effect = new DropShadow();
        effect.setSpread(0.5);
        root.setEffect(effect);
        setCenter(root);

        setDefaultFocusNode(agree);
    }

}
