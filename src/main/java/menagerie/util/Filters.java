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
     * File filter that accepts image OR video extensions.
     */
    public static final FileFilter FILE_NAME_FILTER = file -> IMAGE_NAME_FILTER.accept(file) || VIDEO_NAME_FILTER.accept(file);

    private static FileChooser.ExtensionFilter EXTENSION_FILTER = null;

    /**
     * Utility method that retrieves the extension filter. If the extension filter has no been initialized, it will initialize it.
     *
     * @return Extension filter that accepts image and video extensions.
     */
    public static FileChooser.ExtensionFilter getExtensionFilter() {
        if (EXTENSION_FILTER == null) {
            List<String> exts = new ArrayList<>();
            for (String str : IMAGE_EXTS) exts.add("*" + str);
            for (String str : VIDEO_EXTS) exts.add("*" + str);
            EXTENSION_FILTER = new FileChooser.ExtensionFilter("Accepted Files", exts);
        }

        return EXTENSION_FILTER;
    }

}
