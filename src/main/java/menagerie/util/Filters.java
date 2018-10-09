package menagerie.util;


import javafx.stage.FileChooser;

import java.io.FileFilter;

public abstract class Filters {

    public static final FileFilter IMAGE_FILTER = file -> {
        String name = file.getName().toLowerCase();
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".gif");
    };

    public static final FileFilter VIDEO_FILTER = file -> {
        String name = file.getName().toLowerCase();
        return name.endsWith(".mp4") || name.endsWith(".avi") || name.endsWith(".webm") || name.endsWith(".flv") || name.endsWith(".wmv") || name.endsWith(".3gp") || name.endsWith(".mov") || name.endsWith(".mpg");
    };

    public static final FileFilter IMG_VID_FILTER = file -> IMAGE_FILTER.accept(file) || VIDEO_FILTER.accept(file);

    public static final FileChooser.ExtensionFilter EXTENSION_FILTER = new FileChooser.ExtensionFilter("Image and Video Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.mp4", "*.avi", "*.webm", "*.flv", "*.wmv", "*.3gp", "*.mov", "*.mpg");

}
