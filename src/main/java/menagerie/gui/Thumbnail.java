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

package menagerie.gui;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import menagerie.model.menagerie.Item;
import menagerie.util.Filters;
import menagerie.util.listeners.ObjectListener;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventListener;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

/**
 * JavaFX Image wrapper specifically for loading thumbnails of various types.
 */
public class Thumbnail {

    // ------------------------------- Constants -----------------------------------------

    private static final String[] VLC_THUMBNAILER_ARGS = {"--intf", "dummy", "--vout", "dummy", "--no-audio", "--no-osd", "--no-spu", "--no-stats", "--no-sub-autodetect-file", "--no-disable-screensaver", "--no-snapshot-preview"};

    /**
     * Vertical and horizontal maximum size of thumbnails.
     */
    public static final int THUMBNAIL_SIZE = 150;

    // -------------------------------- Variables -----------------------------------------

    private final File file;
    private final Item owner;
    private Image image;

    private boolean loaded = false;
    private boolean doNotLoad = false;

    private final Set<ObjectListener<Image>> imageReadyListeners = new HashSet<>();
    private final Set<ObjectListener<Image>> imageLoadedListeners = new HashSet<>();

    private static volatile boolean imageThreadRunning = false;
    private static volatile boolean videoThreadRunning = false;
    private static final BlockingQueue<Thumbnail> imageQueue = new LinkedBlockingQueue<>();
    private static final BlockingQueue<Thumbnail> videoQueue = new LinkedBlockingQueue<>();
    private static MediaPlayer vlcjMediaPlayer;


    /**
     * Wraps a thumbnail around an existing image.
     *
     * @param image Image to wrap.
     */
    public Thumbnail(Item owner, Image image) {
        this.image = image;
        this.owner = owner;
        file = null;

        registerListenersToImage();
    }

    /**
     * Constructs a thumbnail for a file and begins loading it.
     *
     * @param file A media file that is accepted by the {@link Filters}
     * @throws IOException If file is not accepted by the {@link Filters}
     */
    public Thumbnail(Item owner, File file) throws IOException {
        this.owner = owner;
        this.file = file;

        if (Main.isVlcjLoaded() && !videoThreadRunning) {
            startVideoThread();
        }

        if (!imageThreadRunning) {
            startImageThread();
        }

        if (Filters.IMAGE_NAME_FILTER.accept(file)) {
            imageQueue.add(this);
        } else if (Filters.VIDEO_NAME_FILTER.accept(file)) {
            videoQueue.add(this);
        } else {
            throw new IOException("Unsupported filetype");
        }
    }

    private void loadImageFromDisk() {
        image = new Image(file.toURI().toString(), THUMBNAIL_SIZE, THUMBNAIL_SIZE, true, true);
        registerListenersToImage();
    }

    private void loadVideoFromDisk() {
        final CountDownLatch inPositionLatch = new CountDownLatch(1);

        MediaPlayerEventListener eventListener = new MediaPlayerEventAdapter() {
            @Override
            public void positionChanged(MediaPlayer mediaPlayer, float newPosition) {
                inPositionLatch.countDown();
            }
        };
        Objects.requireNonNull(vlcjMediaPlayer).events().addMediaPlayerEventListener(eventListener);

        try {
            if (vlcjMediaPlayer.media().start(file.getAbsolutePath())) {
                vlcjMediaPlayer.controls().setPosition(0.1f);
                try {
                    inPositionLatch.await();
                } catch (InterruptedException e) {
                    Main.log.log(Level.WARNING, "Video thumbnailer interrupted while waiting for video init", e);
                }
                vlcjMediaPlayer.events().removeMediaPlayerEventListener(eventListener);

                if (vlcjMediaPlayer.video().videoDimension() != null) {
                    float vidWidth = (float) vlcjMediaPlayer.video().videoDimension().getWidth();
                    float vidHeight = (float) vlcjMediaPlayer.video().videoDimension().getHeight();
                    float scale = Thumbnail.THUMBNAIL_SIZE / vidWidth;
                    if (scale * vidHeight > Thumbnail.THUMBNAIL_SIZE) scale = Thumbnail.THUMBNAIL_SIZE / vidHeight;
                    int width = (int) (scale * vidWidth);
                    int height = (int) (scale * vidHeight);

                    try {
                        image = SwingFXUtils.toFXImage(vlcjMediaPlayer.snapshots().get(width, height), null);

                        synchronized (imageReadyListeners) {
                            imageReadyListeners.forEach(listener -> listener.pass(image));
                        }
                        synchronized (imageLoadedListeners) {
                            imageLoadedListeners.forEach(listener -> listener.pass(image));
                        }
                        loaded = true;
                    } catch (RuntimeException e) {
                        Main.log.log(Level.WARNING, "Failed to get video snapshot of file: " + file, e);
                    }
                }

                vlcjMediaPlayer.controls().stop();
            }
        } catch (Exception e) {
            Main.log.log(Level.WARNING, "Error while trying to create video thumbnail: " + file, e);
        }
    }

    private static void startImageThread() {
        Thread t = new Thread(() -> {
            imageThreadRunning = true;

            while (imageThreadRunning) {
                try {
                    Thumbnail thumb = imageQueue.take();
                    if (thumb.isDoNotLoad()) {
                        thumb.owner.purgeThumbnail();
                        continue;
                    }

                    thumb.loadImageFromDisk();
                } catch (InterruptedException ignore) {
                }
            }
        }, "Image Thumbnailer Thread");
        t.setDaemon(true);
        t.start();
    }

    private static void startVideoThread() {
        Thread t = new Thread(() -> {
            videoThreadRunning = true;
            if (vlcjMediaPlayer != null) vlcjMediaPlayer.release();
            vlcjMediaPlayer = new MediaPlayerFactory(VLC_THUMBNAILER_ARGS).mediaPlayers().newEmbeddedMediaPlayer();

            while (videoThreadRunning) {
                try {
                    Thumbnail thumb = videoQueue.take();
                    if (thumb.isDoNotLoad()) {
                        thumb.owner.purgeThumbnail();
                        continue;
                    }

                    thumb.loadVideoFromDisk();
                    if (!thumb.loaded) {
                        thumb.owner.purgeThumbnail();
                    }
                } catch (InterruptedException ignore) {
                }
            }
        }, "Video Thumbnailer Thread");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Registers imageLoaded listeners if image has not finished loading yet.
     */
    private void registerListenersToImage() {
        if (image.isBackgroundLoading() && image.getProgress() != 1.0) {
            synchronized (imageReadyListeners) {
                imageReadyListeners.forEach(listener -> listener.pass(image));
            }
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
            synchronized (imageReadyListeners) {
                imageReadyListeners.forEach(listener -> listener.pass(image));
            }
            synchronized (imageLoadedListeners) {
                imageLoadedListeners.forEach(listener -> listener.pass(image));
            }
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

    public synchronized void setDoNotLoad(boolean doNotLoad) {
        this.doNotLoad = doNotLoad;
    }

    public synchronized boolean isDoNotLoad() {
        return doNotLoad;
    }

    public static void releaseVLCJResources() {
        if (vlcjMediaPlayer != null) {
            vlcjMediaPlayer.release();
            vlcjMediaPlayer = null;
        }
    }

    /**
     * @return This thumbnail's image.
     */
    public synchronized Image getImage() {
        return image;
    }

    /**
     * @return True if the image has been completely loaded.
     */
    public synchronized boolean isLoaded() {
        return loaded;
    }

}
