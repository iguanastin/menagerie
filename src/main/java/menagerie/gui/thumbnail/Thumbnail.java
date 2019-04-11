package menagerie.gui.thumbnail;

import javafx.scene.image.Image;
import menagerie.gui.Main;
import menagerie.util.Filters;
import menagerie.util.listeners.ObjectListener;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

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

    private final Set<ObjectListener<Image>> imageReadyListeners = new HashSet<>();
    private final Set<ObjectListener<Image>> imageLoadedListeners = new HashSet<>();


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
                        synchronized (imageReadyListeners) {
                            imageReadyListeners.forEach(listener -> listener.pass(image));
                        }
                        synchronized (imageLoadedListeners) {
                            imageLoadedListeners.forEach(listener -> listener.pass(image));
                        }
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
                    synchronized (imageLoadedListeners) {
                        imageLoadedListeners.forEach(listener -> listener.pass(image));
                    }
                }
            });
        } else {
            loaded = true;
        }
    }

    public boolean addImageReadyListener(ObjectListener<Image> listener) {
        return imageReadyListeners.add(listener);
    }

    public boolean addImageLoadedListener(ObjectListener<Image> listener) {
        return imageLoadedListeners.add(listener);
    }

    public boolean removeImageReadyListener(ObjectListener<Image> listener) {
        return imageReadyListeners.remove(listener);
    }

    public boolean removeImageLoadedListener(ObjectListener<Image> listener) {
        return imageLoadedListeners.remove(listener);
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
