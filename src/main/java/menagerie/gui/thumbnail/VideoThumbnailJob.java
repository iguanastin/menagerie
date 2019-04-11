package menagerie.gui.thumbnail;

import javafx.scene.image.Image;

import java.io.File;

interface VideoThumbnailJob {

    /**
     * Called when the image is ready to be used by the FX application.
     *
     * @param image The image that was constructed, or null if the job failed/timed out.
     */
    void imageReady(Image image);

    /**
     * @return The file this job is constructing a thumbnail for.
     */
    File getFile();

}
