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

    private static volatile boolean imageThreadRunning = false;
    private static volatile boolean videoThreadRunning = false;
    private static final BlockingQueue<Thumbnail> imageQueue = new LinkedBlockingQueue<>();
    private static final BlockingQueue<Thumbnail> videoQueue = new LinkedBlockingQueue<>();
    private static MediaPlayer mediaPlayer;
    private static MediaPlayerFactory mediaPlayerFactory;


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
        final CountDownLatch inPositionLatch = new CountDownLatch(2);

        MediaPlayerEventListener eventListener = new MediaPlayerEventAdapter() {
            @Override
            public void positionChanged(MediaPlayer mediaPlayer, float newPosition) {
                inPositionLatch.countDown();
            }

            @Override
            public void videoOutput(MediaPlayer mediaPlayer, int newCount) {
                inPositionLatch.countDown();
            }
        };
        Objects.requireNonNull(mediaPlayer).events().addMediaPlayerEventListener(eventListener);

        try {
            if (mediaPlayer.media().start(file.getAbsolutePath())) {
                mediaPlayer.submit(() -> mediaPlayer.controls().setPosition(0.01f));
                inPositionLatch.await();
                mediaPlayer.events().removeMediaPlayerEventListener(eventListener);

                if (mediaPlayer.video().videoDimension() != null) {
                    float vidWidth = (float) mediaPlayer.video().videoDimension().getWidth();
                    float vidHeight = (float) mediaPlayer.video().videoDimension().getHeight();
                    float scale = Thumbnail.THUMBNAIL_SIZE / vidWidth;
                    if (scale * vidHeight > Thumbnail.THUMBNAIL_SIZE) scale = Thumbnail.THUMBNAIL_SIZE / vidHeight;
                    int width = (int) (scale * vidWidth);
                    int height = (int) (scale * vidHeight);

                    try {
                        image = SwingFXUtils.toFXImage(mediaPlayer.snapshots().get(width, height), null);

                        synchronized (imageReadyListeners) {
                            imageReadyListeners.forEach(listener -> listener.pass(image));
                        }
                        loaded = true;
                    } catch (RuntimeException e) {
                        Main.log.log(Level.WARNING, "Failed to get video snapshot of file: " + file, e);
                    }
                }

                mediaPlayer.controls().stop();
            }
        } catch (Throwable t) {
            Main.log.log(Level.WARNING, "Error while trying to create video thumbnail: " + file, t);
        }
    }

    private static void startImageThread() {
        imageThreadRunning = true;

        Thread t = new Thread(() -> {
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
        videoThreadRunning = true;

        Thread t = new Thread(() -> {
            if (mediaPlayer != null) mediaPlayer.release();
            mediaPlayerFactory = new MediaPlayerFactory(VLC_THUMBNAILER_ARGS);
            mediaPlayer = mediaPlayerFactory.mediaPlayers().newEmbeddedMediaPlayer();

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
                }
            });
        } else {
            loaded = true;
            synchronized (imageReadyListeners) {
                imageReadyListeners.forEach(listener -> listener.pass(image));
            }
        }
    }

    public boolean addImageReadyListener(ObjectListener<Image> listener) {
        return imageReadyListeners.add(listener);
    }

    public boolean removeImageReadyListener(ObjectListener<Image> listener) {
        return imageReadyListeners.remove(listener);
    }

    public synchronized void setDoNotLoad(boolean doNotLoad) {
        this.doNotLoad = doNotLoad;
    }

    public synchronized boolean isDoNotLoad() {
        return doNotLoad;
    }

    public static void releaseVLCJResources() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayerFactory.release();
            mediaPlayer = null;
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
