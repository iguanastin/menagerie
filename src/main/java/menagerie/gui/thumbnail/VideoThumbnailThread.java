package menagerie.gui.thumbnail;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import menagerie.gui.Main;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.MediaPlayerEventListener;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.logging.Level;

/**
 * Thread that constructs thumbnails of video files serially using VLCJ.
 */
public class VideoThumbnailThread extends Thread {

    private static final String[] VLC_THUMBNAILER_ARGS = {"--intf", "dummy", "--vout", "dummy", "--no-audio", "--no-osd", "--no-spu", "--no-stats", "--no-sub-autodetect-file", "--no-disable-screensaver", "--no-snapshot-preview"};

    private volatile boolean running = false;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final BlockingQueue<VideoThumbnailJob> jobs = new LinkedBlockingQueue<>();
    private MediaPlayer mediaPlayer;


    /**
     * Enqueues a job to this thread. FIFO.
     *
     * @param job Job to enqueue.
     */
    public void enqueueJob(VideoThumbnailJob job) {
        jobs.add(job);
    }

    /**
     * Tells the thread to stop running. If a job is currently in progress, it will complete normally.
     */
    public void stopRunning() {
        running = false;
    }

    @Override
    public void run() {
        if (!Main.isVlcjLoaded()) return;

        running = true;
        MediaPlayerFactory factory = new MediaPlayerFactory(VLC_THUMBNAILER_ARGS);
        mediaPlayer = factory.newHeadlessMediaPlayer();

        while (running) {
            try {
                VideoThumbnailJob job;
                job = jobs.take();

                try {
                    final CountDownLatch inPositionLatch = new CountDownLatch(1);

                    MediaPlayerEventListener eventListener = new MediaPlayerEventAdapter() {
                        @Override
                        public void positionChanged(MediaPlayer mediaPlayer, float newPosition) {
                            inPositionLatch.countDown();
                        }
                    };
                    Objects.requireNonNull(mediaPlayer).addMediaPlayerEventListener(eventListener);

                    if (mediaPlayer.startMedia(job.getFile().getAbsolutePath())) {
                        mediaPlayer.setPosition(0.1f);
                        try {
                            inPositionLatch.await();
                        } catch (InterruptedException e) {
                            Main.log.log(Level.WARNING, "Video thumbnailer interrupted while waiting for video init", e);
                        }
                        mediaPlayer.removeMediaPlayerEventListener(eventListener);

                        float vidWidth = (float) mediaPlayer.getVideoDimension().getWidth();
                        float vidHeight = (float) mediaPlayer.getVideoDimension().getHeight();
                        float scale = Thumbnail.THUMBNAIL_SIZE / vidWidth;
                        if (scale * vidHeight > Thumbnail.THUMBNAIL_SIZE) scale = Thumbnail.THUMBNAIL_SIZE / vidHeight;
                        int width = (int) (scale * vidWidth);
                        int height = (int) (scale * vidHeight);

                        // Use executor and Future to limit time spent computing in order to avoid infinite blocking
                        Future<Image> future = executor.submit(() -> SwingFXUtils.toFXImage(mediaPlayer.getSnapshot(width, height), null));
                        try {
                            job.imageReady(future.get(1, TimeUnit.SECONDS));
                        } catch (Exception ignored) {
                        } finally {
                            future.cancel(true);
                        }

                        mediaPlayer.stop();
                    }
                } catch (Exception e) {
                    Main.log.log(Level.SEVERE, "Unexpected error caught while running video thumbnailer job", e);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Releases the VLCJ resources of this thumbnailer.
     */
    public void releaseResources() {
        if (mediaPlayer != null) mediaPlayer.release();
        executor.shutdownNow();
    }

}
