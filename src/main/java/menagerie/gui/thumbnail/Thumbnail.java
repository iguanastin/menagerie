package menagerie.gui.thumbnail;

import javafx.scene.image.Image;
import menagerie.gui.Main;
import menagerie.util.Filters;

import java.io.File;
import java.io.IOException;

/**
 * JavaFX Image wrapper specifically for loading thumbnails of various types.
 */
public class Thumbnail {

    // ------------------------------- Constants -----------------------------------------

    /**
     * Vertical and horizontal maximum size of thumbnails.
     */
    public static final int THUMBNAIL_SIZE = 150;

    private static final VideoThumbnailThread videoThumbnailThread = new VideoThumbnailThread();

    // -------------------------------- Variables -----------------------------------------

    private Image image;

    private boolean loaded = false;

    private ThumbnailImageReadyListener imageReadyListener = null;
    private ThumbnailImageLoadedListener imageLoadedListener = null;


    /**
     * Constructs a thumbnail for a file and begins loading it.
     *
     * @param file A media file that is accepted by the {@link Filters}
     * @throws IOException If file is not accepted by the {@link Filters}
     */
    public Thumbnail(File file) throws IOException {
        if (Main.isVlcjLoaded() && !videoThumbnailThread.isAlive()) {
            videoThumbnailThread.setDaemon(true);
            videoThumbnailThread.start();
        }

        if (Filters.IMAGE_NAME_FILTER.accept(file)) {
            image = new Image(file.toURI().toString(), THUMBNAIL_SIZE, THUMBNAIL_SIZE, true, true, true);
            registerListenersToImage();
        } else if (Filters.VIDEO_NAME_FILTER.accept(file)) {
            if (videoThumbnailThread.isAlive()) {
                videoThumbnailThread.enqueueJob(new VideoThumbnailJob() {
                    @Override
                    public void imageReady(Image image) {
                        Thumbnail.this.image = image;
                        loaded = true;
                        if (getImageReadyListener() != null) getImageReadyListener().imageReady(image);
                        if (getImageLoadedListener() != null) getImageLoadedListener().finishedLoading(image);
                    }

                    @Override
                    public File getFile() {
                        return file;
                    }
                });
            }
        } else {
            throw new IOException("Unsupported filetype");
        }
    }

    /**
     * Wraps a thumbnail around an existing image.
     *
     * @param image Image to wrap.
     */
    public Thumbnail(Image image) {
        this.image = image;

        registerListenersToImage();
    }

    /**
     * Registers imageLoaded listeners if image has not finished loading yet.
     */
    private void registerListenersToImage() {
        if (image.isBackgroundLoading() && image.getProgress() != 1.0) {
            image.progressProperty().addListener((observable, oldValue, newValue) -> {
                if (!image.isError() && newValue.doubleValue() == 1.0) {
                    loaded = true;
                    if (getImageLoadedListener() != null) getImageLoadedListener().finishedLoading(image);
                }
            });
        } else {
            loaded = true;
        }
    }

    /**
     * @return The listener listening for the image to finish loading.
     */
    public synchronized ThumbnailImageLoadedListener getImageLoadedListener() {
        return imageLoadedListener;
    }

    /**
     * @return The listener listening for the image to be ready to use.
     */
    public synchronized ThumbnailImageReadyListener getImageReadyListener() {
        return imageReadyListener;
    }

    /**
     *
     * @param imageLoadedListener Image loaded listener.
     */
    public synchronized void setImageLoadedListener(ThumbnailImageLoadedListener imageLoadedListener) {
        this.imageLoadedListener = imageLoadedListener;
    }

    /**
     *
     * @param imageReadyListener Image ready listener.
     */
    public synchronized void setImageReadyListener(ThumbnailImageReadyListener imageReadyListener) {
        this.imageReadyListener = imageReadyListener;
    }

    /**
     *
     * @return The Thumbnailer that creates thumbnails for video files.
     */
    public static VideoThumbnailThread getVideoThumbnailThread() {
        return videoThumbnailThread;
    }

    /**
     *
     * @return This thumbnail's image.
     */
    public synchronized Image getImage() {
        return image;
    }

    /**
     *
     * @return True if the image has been completely loaded.
     */
    public synchronized boolean isLoaded() {
        return loaded;
    }

}
