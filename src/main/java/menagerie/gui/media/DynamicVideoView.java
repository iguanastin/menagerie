package menagerie.gui.media;

import com.sun.jna.Memory;
import javafx.application.Platform;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.stage.Screen;
import uk.co.caprica.vlcj.component.DirectMediaPlayerComponent;
import uk.co.caprica.vlcj.player.direct.BufferFormat;
import uk.co.caprica.vlcj.player.direct.BufferFormatCallback;
import uk.co.caprica.vlcj.player.direct.DirectMediaPlayer;
import uk.co.caprica.vlcj.player.direct.format.RV32BufferFormat;

import java.awt.*;
import java.nio.ByteBuffer;

/**
 * Dynamically sized view that shows a video using VLCJ.
 */
public class DynamicVideoView extends ImageView {

    private final DirectMediaPlayerComponent mediaPlayerComponent = new CanvasPlayerComponent();
    private final WritablePixelFormat<ByteBuffer> pixelFormat = PixelFormat.getByteBgraPreInstance();
    private final FloatProperty videoSourceRatioProperty = new SimpleFloatProperty(0.4f);
    private final WritableImage writableImage;

    private boolean released = false;


    public DynamicVideoView() {
        super();
        //        NativeLibrary.addSearchPath("vlclib", new DefaultWindowsNativeDiscoveryStrategy().discover());

        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        writableImage = new WritableImage((int) visualBounds.getWidth(), (int) visualBounds.getHeight());
        setImage(writableImage);

        addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (!released && getMediaPlayer() != null) {
                if (event.getButton() == MouseButton.PRIMARY) {
                    if (getMediaPlayer().isPlaying()) {
                        getMediaPlayer().pause();
                    } else {
                        getMediaPlayer().play();
                    }
                } else if (event.getButton() == MouseButton.SECONDARY) {
                    getMediaPlayer().mute(!getMediaPlayer().isMute());
                }
                event.consume();
            }
        });
        addEventHandler(ScrollEvent.SCROLL, event -> {
            if (!released && getMediaPlayer() != null) {
                float delta = 10000.0f / getMediaPlayer().getLength();
                if (event.getDeltaY() < 0) delta = -delta;
                getMediaPlayer().setPosition(Math.min(0.9999f, Math.max(getMediaPlayer().getPosition() + delta, 0)));
            }
        });
    }

    /**
     * @return The VLCJ media player backing this view.
     */
    private DirectMediaPlayer getMediaPlayer() {
        if (!released) {
            return mediaPlayerComponent.getMediaPlayer();
        } else {
            return null;
        }
    }

    @Override
    public double minWidth(double height) {
        return 40;
    }

    @Override
    public double prefWidth(double height) {
        Dimension d = null;
        if (!released && getMediaPlayer() != null) {
            d = getMediaPlayer().getVideoDimension();
        }
        if (d == null) return minWidth(height);
        return d.getWidth();
    }

    @Override
    public double maxWidth(double height) {
        return 16384;
    }

    @Override
    public double minHeight(double width) {
        return 40;
    }

    @Override
    public double prefHeight(double width) {
        Dimension d = null;
        if (!released && getMediaPlayer() != null) {
            d = getMediaPlayer().getVideoDimension();
        }
        if (d == null) return minHeight(width);
        return d.getHeight();
    }

    @Override
    public double maxHeight(double width) {
        return 16384;
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public void resize(double width, double height) {
        Dimension d = null;
        if (!released && getMediaPlayer() != null) {
            d = getMediaPlayer().getVideoDimension();
        }
        if (d == null) {
            setFitWidth(width);
            setFitHeight(height);
        } else {
            double scale = 1;
            if (scale * d.getWidth() > width) scale = width / d.getWidth();
            if (scale * d.getHeight() > height) scale = height / d.getHeight();

            setFitWidth(d.getWidth() * scale);
            setFitHeight(d.getHeight() * scale);
        }
    }

    public void setMute(boolean b) {
        if (!released && getMediaPlayer() != null) getMediaPlayer().mute(b);
    }

    public void setRepeat(boolean b) {
        if (!released && getMediaPlayer() != null) getMediaPlayer().setRepeat(b);
    }

    public boolean isPlaying() {
        return !released && getMediaPlayer() != null && getMediaPlayer().isPlaying();
    }

    public boolean isRepeating() {
        return !released && getMediaPlayer() != null && getMediaPlayer().getRepeat();
    }

    public boolean isMuted() {
        return !released && getMediaPlayer() != null && getMediaPlayer().isMute();
    }

    public void pause() {
        if (!released && getMediaPlayer() != null) getMediaPlayer().pause();
    }

    public void play() {
        if (!released && getMediaPlayer() != null) getMediaPlayer().play();
    }

    public void stop() {
        if (!released && getMediaPlayer() != null) getMediaPlayer().stop();
    }

    public void startMedia(String path) {
        if (!released && getMediaPlayer() != null) getMediaPlayer().startMedia(path);
    }

    /**
     * Class used to take VLCJ frame data and transfer it to the JFX ImageView.
     */
    private class CanvasPlayerComponent extends DirectMediaPlayerComponent {

        CanvasPlayerComponent() {
            super(new CanvasBufferFormatCallback());
        }

        PixelWriter pixelWriter = null;

        private PixelWriter getPW() {
            if (pixelWriter == null) {
                pixelWriter = writableImage.getPixelWriter();
            }
            return pixelWriter;
        }

        @Override
        public void display(DirectMediaPlayer mediaPlayer, Memory[] nativeBuffers, BufferFormat bufferFormat) {
            if (writableImage == null) {
                return;
            }
            Memory nativeBuffer = mediaPlayer.lock()[0];
            try {
                ByteBuffer byteBuffer = nativeBuffer.getByteBuffer(0, nativeBuffer.size());
                getPW().setPixels(0, 0, bufferFormat.getWidth(), bufferFormat.getHeight(), pixelFormat, byteBuffer, bufferFormat.getPitches()[0]);
            } finally {
                mediaPlayer.unlock();
            }
        }

    }

    private class CanvasBufferFormatCallback implements BufferFormatCallback {

        @Override
        public BufferFormat getBufferFormat(int sourceWidth, int sourceHeight) {
            Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
            Platform.runLater(() -> videoSourceRatioProperty.set((float) sourceHeight / (float) sourceWidth));
            return new RV32BufferFormat((int) visualBounds.getWidth(), (int) visualBounds.getHeight());
        }

    }

    public void releaseVLCJ() {
        if (!released && getMediaPlayer() != null) {
            getMediaPlayer().stop();
            getMediaPlayer().release();
            released = true;
        }
    }

}
