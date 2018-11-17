package menagerie.util;


import javafx.stage.FileChooser;
import uk.co.caprica.vlcj.filter.VideoFileFilter;

import java.io.FileFilter;

public abstract class Filters {

    public static final FileFilter IMAGE_FILTER = file -> {
        String name = file.getName().toLowerCase();
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".gif");
    };

//    public static final FileFilter VIDEO_FILTER = file -> {
//        String name = file.getName().toLowerCase();
//        return name.endsWith(".mp4") || name.endsWith(".avi") || name.endsWith(".webm") || name.endsWith(".flv") || name.endsWith(".wmv") || name.endsWith(".3gp") || name.endsWith(".mov") || name.endsWith(".mpg");
//    };

    public static final FileFilter VIDEO_FILTER = VideoFileFilter.INSTANCE;

    public static final FileFilter IMG_VID_FILTER = file -> IMAGE_FILTER.accept(file) || VIDEO_FILTER.accept(file);

    public static final FileChooser.ExtensionFilter IMAGE_EXTENSION_FILTER = new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif");

}
