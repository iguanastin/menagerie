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

package menagerie.util;


import javafx.stage.FileChooser;

import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * A utility class containing filters for the Menagerie environment.
 */
public abstract class Filters {

    public static final String[] IMAGE_EXTS = {".png", ".jpg", ".jpeg", ".gif", ".bmp"};
    public static final String[] VIDEO_EXTS = {".mp4", ".avi", ".webm", ".flv", ".wmv", ".3gp", ".mov", ".mpg", ".m4v"};
    public static final List<String> USER_EXTS = new ArrayList<>();

    /**
     * File filter that accepts only image extensions.
     */
    public static final FileFilter IMAGE_NAME_FILTER = file -> {
        String name = file.getName().toLowerCase();
        for (String ext : IMAGE_EXTS) {
            if (name.endsWith(ext)) return true;
        }
        return false;
    };
    /**
     * File filter that accepts only video extensions.
     */
    public static final FileFilter VIDEO_NAME_FILTER = file -> {
        String name = file.getName().toLowerCase();
        for (String ext : VIDEO_EXTS) {
            if (name.endsWith(ext)) return true;
        }
        return false;
    };
    /**
     * File filter that accepts user defined extensions.
     */
    public static final FileFilter USER_NAME_FILTER = file -> {
        String name = file.getName().toLowerCase();
        for (String ext : USER_EXTS) {
            if (name.endsWith(ext)) return true;
        }
        return false;
    };
    /**
     * File filter that accepts image OR video extensions.
     */
    public static final FileFilter FILE_NAME_FILTER = file -> IMAGE_NAME_FILTER.accept(file) || VIDEO_NAME_FILTER.accept(file) || USER_NAME_FILTER.accept(file);

    /**
     * Utility method that retrieves the extension filter. If the extension filter has no been initialized, it will initialize it.
     *
     * @return Extension filter that accepts image and video extensions.
     */
    public static FileChooser.ExtensionFilter getExtensionFilter() {
        List<String> exts = new ArrayList<>();
        for (String str : IMAGE_EXTS) exts.add("*" + str);
        for (String str : VIDEO_EXTS) exts.add("*" + str);
        USER_EXTS.forEach(s -> {
            if (s.startsWith(".")) exts.add("*" + s);
            else exts.add("*." + s);
        });
        return new FileChooser.ExtensionFilter("Accepted Files", exts);
    }

}
