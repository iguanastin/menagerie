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

public class MenagerieSettings extends Settings {


    public MenagerieSettings() {
        GroupSetting importGroup = new GroupSetting("import-group", "Importing", null, false, false, true);
        importGroup.getChildren().add(new FolderSetting("default-folder", "Default Folder", "Folder to import files into, where applicable", false, null));
        importGroup.getChildren().add(new StringSetting("user-filetypes", "Also import filetypes", "File extensions, separated by spaces. E.g. \".txt .pdf .rar\"", false, null));
        importGroup.getChildren().add(new BooleanSetting("url-filename", "Use filename from URL", "When importing from web, skip opening the save file dialog", false, true));
        importGroup.getChildren().add(new BooleanSetting("tag-images", "Tag images with 'image'", null, false, false));
        importGroup.getChildren().add(new BooleanSetting("tag-videos", "Tag videos with 'video'", null, false, false));
        importGroup.getChildren().add(new BooleanSetting("tag-tagme", "Tag everything with 'tagme'", null, false, true));
        GroupSetting autoImportGroup = new GroupSetting("auto-import-group", "Automatically import", null, false, true, false);
        autoImportGroup.getChildren().add(new FolderSetting("auto-import-folder", "Auto import from folder", null, false, null));
        autoImportGroup.getChildren().add(new BooleanSetting("auto-import-move", "Move auto-imported files to default folder", null, false, true));
        importGroup.getChildren().add(autoImportGroup);
        getSettings().add(importGroup);

        GroupSetting duplicateGroup = new GroupSetting("duplicate-group", "Duplicate Finding", null, false, false, true);
        duplicateGroup.getChildren().add(new DoubleSetting("duplicate-confidence", "Duplicate Confidence", null, false, 0.95));
        getSettings().add(duplicateGroup);

        GroupSetting videoGroup = new GroupSetting("video-group", "Video Playback", null, false, false, true);
        videoGroup.getChildren().add(new BooleanSetting("repeat-video", "Repeat video", null, false, true));
        videoGroup.getChildren().add(new BooleanSetting("mute-video", "Mute video", null, false, false));
        videoGroup.getChildren().add(new FolderSetting("vlc-folder", "VLC Folder", null, false, null));
        getSettings().add(videoGroup);

        GroupSetting dbGroup = new GroupSetting("db-group", "Database", null, false, false, true);
        dbGroup.getChildren().add(new StringSetting("db-url", "Database URL", null, false, "~/menagerie"));
        dbGroup.getChildren().add(new StringSetting("db-user", "Database User", null, false, "sa"));
        dbGroup.getChildren().add(new StringSetting("db-pass", "Database Pass", null, false, ""));
        dbGroup.getChildren().add(new BooleanSetting("db-backup", "Backup database on startup", null, false, true));
        getSettings().add(dbGroup);

        GroupSetting explorerGroup = new GroupSetting("explorer-group", "Explorer", null, false, false, true);
        explorerGroup.getChildren().add(new IntSetting("grid-width", "Grid width", null, false, 3));
        getSettings().add(explorerGroup);

        getSettings().add(new BooleanSetting("help-on-start", null, null, true, true));
        getSettings().add(new BooleanSetting("licenses-on-start", null, null, true, true));
        getSettings().add(new BooleanSetting("window-maximized", null, null, true, false));
        getSettings().add(new BooleanSetting("expand-item-info", null, null, true, false));
        getSettings().add(new IntSetting("window-x", null, null, true, 0));
        getSettings().add(new IntSetting("window-y", null, null, true, 0));
        getSettings().add(new IntSetting("window-width", null, null, true, 800));
        getSettings().add(new IntSetting("window-height", null, null, true, 600));
    }

}
