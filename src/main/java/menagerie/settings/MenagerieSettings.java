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

package menagerie.settings;

import java.io.File;
import java.io.IOException;

public class MenagerieSettings extends Settings {


    public MenagerieSettings() {
        GroupSetting importGroup = new GroupSetting("import-group", "Importing");
        importGroup.getChildren().add(new FolderSetting("default-folder", "Default import folder"));
        importGroup.getChildren().add(new StringSetting("user-filetypes", "Also accept these filetypes"));
        importGroup.getChildren().add(new BooleanSetting("url-filename", "Use filename from URL", true));
        importGroup.getChildren().add(new BooleanSetting("tag-images", "Tag images with 'image' on import"));
        importGroup.getChildren().add(new BooleanSetting("tag-videos", "Tag videos with 'video' on import"));
        importGroup.getChildren().add(new BooleanSetting("tag-tagme", "Tag everything with 'tagme' on import"));
        GroupSetting autoImportGroup = new GroupSetting("auto-import-group", "Automatically import", true, false);
        autoImportGroup.getChildren().add(new FolderSetting("auto-import-folder", "Auto import from folder"));
        autoImportGroup.getChildren().add(new BooleanSetting("auto-import-move", "Move auto-imported files to default folder", true));
        importGroup.getChildren().add(autoImportGroup);
        getSettings().add(importGroup);

        GroupSetting duplicateGroup = new GroupSetting("duplicate-group", "Duplicate Finding");
        duplicateGroup.getChildren().add(new DoubleSetting("duplicate-confidence", "Duplicate Confidence", 0.95));
        getSettings().add(duplicateGroup);

        GroupSetting videoGroup = new GroupSetting("video-group", "Video Playback");
        videoGroup.getChildren().add(new BooleanSetting("repeat-video", "Repeat video", true));
        videoGroup.getChildren().add(new BooleanSetting("mute-video", "Mute video"));
        videoGroup.getChildren().add(new FolderSetting("vlc-folder", "VLC Folder"));
        getSettings().add(videoGroup);

        GroupSetting dbGroup = new GroupSetting("db-group", "Database");
        dbGroup.getChildren().add(new StringSetting("db-url", "Database URL", "~/menagerie"));
        dbGroup.getChildren().add(new StringSetting("db-user", "Database User", "sa"));
        dbGroup.getChildren().add(new StringSetting("db-pass", "Database Pass", ""));
        dbGroup.getChildren().add(new BooleanSetting("db-backup", "Backup database on startup", true));
        getSettings().add(dbGroup);

        GroupSetting explorerGroup = new GroupSetting("explorer-group", "Explorer");
        explorerGroup.getChildren().add(new IntSetting("grid-width", "Grid width", 3));
        getSettings().add(explorerGroup);

        getSettings().add(new BooleanSetting("help-on-start", "Show help on startup", true));
        getSettings().add(new BooleanSetting("licenses-on-start", "Show license agreement on startup", true));
        getSettings().add(new BooleanSetting("window-maximized", "Window maximized"));
        getSettings().add(new BooleanSetting("expand-item-info", "Item info expanded"));
        getSettings().add(new IntSetting("window-x", "Window X"));
        getSettings().add(new IntSetting("window-y", "Window Y"));
        getSettings().add(new IntSetting("window-width", "Window width"));
        getSettings().add(new IntSetting("window-height", "Window height"));

    }

    public static void main(String[] args) throws IOException {
        MenagerieSettings s = new MenagerieSettings();
        s.save(new File("/s/bach/g/under/austinbt/test.out"));
    }

}
