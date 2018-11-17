package menagerie.gui.thumbnail;

import javafx.scene.image.Image;

import java.io.File;

abstract class VideoThumbnailJob {

    abstract void imageReady(Image image);

    abstract File getFile();

}
