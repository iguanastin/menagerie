package menagerie.gui.thumbnail;

import javafx.embed.swing.SwingFXUtils;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.MediaPlayerEventListener;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;

import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;

public class VideoThumbnailThread extends Thread {

    private static final String[] VLC_THUMBNAILER_ARGS = {"--intf", "dummy", "--vout", "dummy", "--no-audio", "--no-osd", "--no-spu", "--no-stats", "--no-sub-autodetect-file", "--no-disable-screensaver", "--no-snapshot-preview"};
    private static MediaPlayer THUMBNAIL_MEDIA_PLAYER = null;

    private boolean running = false;
    private Queue<VideoThumbnailJob> jobs = new ArrayDeque<>();


    public synchronized void enqueueJob(VideoThumbnailJob job) {
        jobs.add(job);
        notifyAll();
    }

    public synchronized void stopRunning() {
        running = false;
    }

    private synchronized boolean isRunning() {
        return running;
    }

    private synchronized VideoThumbnailJob dequeuJob() {
        return jobs.poll();
    }

    @Override
    public void run() {
        running = true;
        MediaPlayer mp = getThumbnailMediaPlayer();

        while (isRunning()) {
            VideoThumbnailJob job = dequeuJob();

            while (job != null) {
                final CountDownLatch inPositionLatch = new CountDownLatch(1);

                MediaPlayerEventListener eventListener = new MediaPlayerEventAdapter() {
                    @Override
                    public void positionChanged(MediaPlayer mediaPlayer, float newPosition) {
                        inPositionLatch.countDown();
                    }
                };
                mp.addMediaPlayerEventListener(eventListener);

                if (mp.startMedia(job.getFile().getAbsolutePath())) {
                    mp.setPosition(0.1f);
                    try {
                        inPositionLatch.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mp.removeMediaPlayerEventListener(eventListener);

                    float vidWidth = (float) mp.getVideoDimension().getWidth();
                    float vidHeight = (float) mp.getVideoDimension().getHeight();
                    float scale = Thumbnail.THUMBNAIL_SIZE / vidWidth;
                    if (scale * vidHeight > Thumbnail.THUMBNAIL_SIZE) scale = Thumbnail.THUMBNAIL_SIZE / vidHeight;
                    int width = (int) (scale * vidWidth);
                    int height = (int) (scale * vidHeight);
                    BufferedImage img = mp.getSnapshot(width, height);

                    mp.stop();
                    job.imageReady(SwingFXUtils.toFXImage(img, null));
                }

                job = dequeuJob();
            }

            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException ignore) {
                }
            }
        }
    }

    private static MediaPlayer getThumbnailMediaPlayer() {
        if (THUMBNAIL_MEDIA_PLAYER == null) {
            MediaPlayerFactory factory = new MediaPlayerFactory(VLC_THUMBNAILER_ARGS);
            THUMBNAIL_MEDIA_PLAYER = factory.newHeadlessMediaPlayer();
        }

        return THUMBNAIL_MEDIA_PLAYER;
    }

    public static void releaseThumbnailMediaPlayer() {
        if (THUMBNAIL_MEDIA_PLAYER != null) {
            THUMBNAIL_MEDIA_PLAYER.release();
            THUMBNAIL_MEDIA_PLAYER = null;
        }
    }

}
