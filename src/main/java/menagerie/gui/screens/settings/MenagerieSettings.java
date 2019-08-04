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

import menagerie.settings.*;

public class MenagerieSettings extends Settings {


    public MenagerieSettings() {
        GroupSetting importGroup = new GroupSetting("import-group").label("Importing");
        importGroup.getChildren().add(new FolderSetting("default-folder").label("Default Folder").tip("Folder to import files into, where applicable"));
        importGroup.getChildren().add(new StringSetting("user-filetypes").label("Also import filetypes").tip("File extensions, separated by spaces. E.g. \".txt .pdf .rar\""));
        importGroup.getChildren().add(new BooleanSetting("url-filename", true).label("Use filename from URL").tip("When importing from web, skip opening the save file dialog"));
        importGroup.getChildren().add(new BooleanSetting("tag-images", false).label("Tag images with 'image'"));
        importGroup.getChildren().add(new BooleanSetting("tag-videos", false).label("Tag videos with 'video'"));
        importGroup.getChildren().add(new BooleanSetting("tag-tagme", true).label("Tag everything with 'tagme'"));
        GroupSetting autoImportGroup = new GroupSetting("auto-import-group").label("Automatically import").toggleable().disable();
        autoImportGroup.getChildren().add(new FolderSetting("auto-import-folder").label("Auto import from folder").tip("Folder to automatically import files from"));
        autoImportGroup.getChildren().add(new BooleanSetting("auto-import-move", true).label("Move auto-imported files to default folder"));
        importGroup.getChildren().add(autoImportGroup);
        getSettings().add(importGroup);

        GroupSetting duplicateGroup = new GroupSetting("duplicate-group").label("Duplicate Finding");
        duplicateGroup.getChildren().add(new DoubleSetting("duplicate-confidence", 0.95).range(0.9, 1.0).label("Duplicate Confidence").tip("Value between 0.90 and 1.00"));
        getSettings().add(duplicateGroup);

        GroupSetting videoGroup = new GroupSetting("video-group").label("Video Playback");
        videoGroup.getChildren().add(new BooleanSetting("repeat-video", true).label("Repeat video"));
        videoGroup.getChildren().add(new BooleanSetting("mute-video", false).label("Mute video"));
        videoGroup.getChildren().add(new FolderSetting("vlc-folder").label("VLC Folder").tip("Only needs to be specified if VLC cannot be found in the normal places"));
        getSettings().add(videoGroup);

        GroupSetting dbGroup = new GroupSetting("db-group").label("Database");
        dbGroup.getChildren().add(new StringSetting("db-url", "~/menagerie").label("Database URL").tip("\"~/menagerie\" by default"));
        dbGroup.getChildren().add(new StringSetting("db-user", "sa").label("Database User").tip("\"sa\" by default"));
        dbGroup.getChildren().add(new StringSetting("db-pass", "").label("Database Pass").tip("Empty by default"));
        dbGroup.getChildren().add(new BooleanSetting("db-backup", true).label("Backup database on launch"));
        getSettings().add(dbGroup);

        GroupSetting explorerGroup = new GroupSetting("explorer-group").label("Explorer");
        explorerGroup.getChildren().add(new IntSetting("grid-width", 3).range(1, 8).label("Grid width"));
        getSettings().add(explorerGroup);

        getSettings().add(new BooleanSetting("help-on-start", true).hide());
        getSettings().add(new BooleanSetting("licenses-on-start", true).hide());
        getSettings().add(new BooleanSetting("window-maximized", false).hide());
        getSettings().add(new BooleanSetting("expand-item-info", false).hide());
        getSettings().add(new IntSetting("window-x").hide());
        getSettings().add(new IntSetting("window-y").hide());
        getSettings().add(new IntSetting("window-width", 800).hide());
        getSettings().add(new IntSetting("window-height", 600).hide());
    }

}
