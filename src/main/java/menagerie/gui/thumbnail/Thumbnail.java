package menagerie.gui.thumbnail;

import javafx.scene.image.Image;
import menagerie.util.Filters;

import java.io.File;
import java.io.IOException;

public class Thumbnail {

    // ------------------------------- Constants -----------------------------------------

    public static final int THUMBNAIL_SIZE = 150;

    private static final VideoThumbnailThread videoThumbnailThread = new VideoThumbnailThread();

    // -------------------------------- Variables -----------------------------------------

    private Image image;

    private boolean loaded = false;

    private ThumbnailImageReadyListener imageReadyListener = null;
    private ThumbnailImageLoadedListener imageLoadedListener = null;


    public Thumbnail(File file) throws IOException {
        if (!videoThumbnailThread.isAlive()) {
            videoThumbnailThread.setDaemon(true);
            videoThumbnailThread.start();
        }

        if (Filters.IMAGE_FILTER.accept(file)) {
            image = new Image(file.toURI().toString(), THUMBNAIL_SIZE, THUMBNAIL_SIZE, true, true, true);
            registerListenersToImage();
        } else if (Filters.VIDEO_FILTER.accept(file)) {
            videoThumbnailThread.enqueueJob(new VideoThumbnailJob() {
                @Override
                void imageReady(Image image) {
                    Thumbnail.this.image = image;
                    loaded = true;
                    if (getImageReadyListener() != null) getImageReadyListener().imageReady(image);
                    if (getImageLoadedListener() != null) getImageLoadedListener().finishedLoading(image);
                }

                @Override
                File getFile() {
                    return file;
                }
            });
        } else {
            throw new IOException("Unsupported filetype");
        }
    }

    public Thumbnail(Image image) {
        this.image = image;

        registerListenersToImage();
    }

    private void registerListenersToImage() {
        if (image.isBackgroundLoading() && image.getProgress() != 1.0) {
            image.progressProperty().addListener((observable, oldValue, newValue) -> {
                if (!image.isError() && newValue.doubleValue() == 1.0) {
                    loaded = true;
                    if (getImageLoadedListener() != null) getImageLoadedListener().finishedLoading(image);
                }
            });
            image.errorProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    //TODO
                }
            });
        } else {
            loaded = true;
        }
    }

    public synchronized ThumbnailImageLoadedListener getImageLoadedListener() {
        return imageLoadedListener;
    }

    public synchronized ThumbnailImageReadyListener getImageReadyListener() {
        return imageReadyListener;
    }

    public synchronized void setImageLoadedListener(ThumbnailImageLoadedListener imageLoadedListener) {
        this.imageLoadedListener = imageLoadedListener;
    }

    public synchronized void setImageReadyListener(ThumbnailImageReadyListener imageReadyListener) {
        this.imageReadyListener = imageReadyListener;
    }

    public synchronized Image getImage() {
        return image;
    }

    public synchronized boolean isLoaded() {
        return loaded;
    }

}
