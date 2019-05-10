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

package menagerie.gui.media;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Affine;
import menagerie.util.NanoTimer;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent;
import uk.co.caprica.vlcj.player.component.MediaPlayerComponent;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.CallbackVideoSurface;
import uk.co.caprica.vlcj.player.embedded.videosurface.VideoSurfaceAdapters;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat;

import java.nio.ByteBuffer;
import java.util.concurrent.Semaphore;

/**
 * Dynamically sized view that shows a video using VLCJ.
 */
public class DynamicVideoView extends StackPane {

    private final Slider slider = new Slider(0, 1, 0);
    private final Label durationLabel = new Label("0:00/0:00");
    private final ImageView muteImageView = new ImageView(getClass().getResource("/misc/mute.png").toString());
    private final ImageView pauseImageView = new ImageView(getClass().getResource("/misc/pause.png").toString());
    private final Canvas canvas = new Canvas(100, 50);
    private MediaPlayerComponent mediaPlayerComponent = null;
    private EmbeddedMediaPlayer mediaPlayer = null;
    private WritableImage img;
    private PixelWriter pixelWriter;
    private final WritablePixelFormat<ByteBuffer> pixelFormat = PixelFormat.getByteBgraPreInstance();

    private final double timerPeriod = 1000.0 / 60;
    private NanoTimer timer = new NanoTimer(timerPeriod) {
        @Override
        protected void onSucceeded() {
            renderFrame();
        }
    };

    private boolean released = false;


    public DynamicVideoView() {
        super();
        //        NativeLibrary.addSearchPath("vlclib", new DefaultWindowsNativeDiscoveryStrategy().discover());

        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());
        getChildren().add(canvas);
        HBox bottomBarHBox = new HBox(5, durationLabel, slider, muteImageView);
        bottomBarHBox.setAlignment(Pos.BOTTOM_RIGHT);
        BorderPane bp = new BorderPane(null, null, null, bottomBarHBox, null);
        HBox.setHgrow(slider, Priority.ALWAYS);
        slider.setOpacity(0.75);
        slider.setFocusTraversable(false);
        slider.valueChangingProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue && !released && getMediaPlayer() != null)
                getMediaPlayer().controls().setPosition((float) slider.getValue());
        });
        slider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (!released && getMediaPlayer() != null) {
                final long len = getMediaPlayer().status().length();
                final long cur = (long) (len * newValue.doubleValue());
                final long totalSeconds = (len / 1000) % 60;
                final long totalMinutes = len / 1000 / 60;
                final long seconds = (cur / 1000) % 60;
                final long minutes = cur / 1000 / 60;
                durationLabel.setText(String.format("%d:%02d/%d:%02d", minutes, seconds, totalMinutes, totalSeconds));
            }
        });
        bp.setPadding(new Insets(5));
        BorderPane.setAlignment(muteImageView, Pos.BOTTOM_RIGHT);
        getChildren().add(bp);
        getChildren().add(pauseImageView);
        StackPane.setAlignment(pauseImageView, Pos.CENTER);
        pixelWriter = canvas.getGraphicsContext2D().getPixelWriter();

        addEventHandler(MouseEvent.MOUSE_ENTERED, event -> {
            bottomBarHBox.setOpacity(1);
            event.consume();
        });
        addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
            bottomBarHBox.setOpacity(0);
            event.consume();
        });
        addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                if (isPlaying()) {
                    pause();
                } else {
                    play();
                }
            } else if (event.getButton() == MouseButton.SECONDARY) {
                setMute(!isMuted());
            }
            event.consume();
        });
        addEventHandler(ScrollEvent.SCROLL, event -> {
            if (!released && getMediaPlayer() != null) {
                long duration = getMediaPlayer().media().info().duration();
                float delta;
                if (duration < 10000) {
                    delta = 0.25f;
                } else if (duration < 30000) {
                    delta = 5000f / duration;
                } else {
                    delta = 10000f / duration;
                }
                if (event.getDeltaY() < 0) delta = -delta;
                getMediaPlayer().controls().setPosition(Math.min(0.9999f, Math.max(getMediaPlayer().status().position() + delta, 0)));
            }
        });
    }

    /**
     * @return The VLCJ media player backing this view.
     */
    private MediaPlayer getMediaPlayer() {
        if (!released) {
            if (mediaPlayer == null) {
                mediaPlayerComponent = new EmbeddedMediaPlayerComponent();
                mediaPlayer = mediaPlayerComponent.mediaPlayerFactory().mediaPlayers().newEmbeddedMediaPlayer();
                mediaPlayer.videoSurface().set(new JavaFxVideoSurface());
                mediaPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
                    @Override
                    public void positionChanged(MediaPlayer mediaPlayer, float newPosition) {
                        Platform.runLater(() -> {
                            if (!slider.isValueChanging()) slider.setValue(newPosition);
                        });
                    }
                });
            }

            return mediaPlayer;
        } else {
            return null;
        }
    }

    @Override
    protected double computeMinWidth(double height) {
        return 40;
    }

    @Override
    protected double computeMinHeight(double width) {
        return 40;
    }

    @Override
    public void resize(double width, double height) {
        if (img == null) {
            setWidth(width);
            setHeight(height);
        } else {
            double scale = 1;
            if (img.getWidth() > width) scale = width / img.getWidth();
            if (scale * img.getHeight() > height) scale = height / img.getHeight();

            setWidth(img.getWidth() * scale);
            setHeight(img.getHeight() * scale);
        }
    }

    public void setMute(boolean b) {
        if (!released && getMediaPlayer() != null) {
            getMediaPlayer().audio().setMute(b);
            muteImageView.setOpacity(b ? 1 : 0);
        }
    }

    public void setRepeat(boolean b) {
        if (!released && getMediaPlayer() != null) getMediaPlayer().controls().setRepeat(b);
    }

    public boolean isPlaying() {
        return !released && getMediaPlayer() != null && getMediaPlayer().status().isPlaying();
    }

    public boolean isRepeating() {
        return !released && getMediaPlayer() != null && getMediaPlayer().controls().getRepeat();
    }

    public boolean isMuted() {
        return !released && getMediaPlayer() != null && getMediaPlayer().audio().isMute();
    }

    public void pause() {
        if (!released && getMediaPlayer() != null) {
            getMediaPlayer().controls().pause();
            timer.cancel();
            pauseImageView.setOpacity(1);
        }
    }

    public void play() {
        if (!released && getMediaPlayer() != null) {
            getMediaPlayer().controls().play();
            if (!timer.getState().equals(Worker.State.READY)) {
                timer = new NanoTimer(timerPeriod) {
                    @Override
                    protected void onSucceeded() {
                        renderFrame();
                    }
                };
            }
            timer.start();
            pauseImageView.setOpacity(0);
        }
    }

    public void stop() {
        if (!released && getMediaPlayer() != null) {
            getMediaPlayer().controls().stop();
            timer.cancel();
            pauseImageView.setOpacity(1);
        }
    }

    public void startMedia(String path) {
        if (!released && getMediaPlayer() != null) {
            getMediaPlayer().media().start(path);
            play();
        }
    }

    public void releaseVLCJ() {
        if (!released && getMediaPlayer() != null) {
            if (mediaPlayerComponent != null) mediaPlayerComponent.mediaPlayerFactory().release();
            getMediaPlayer().controls().stop();
            getMediaPlayer().release();
            released = true;
        }
    }

    private class JavaFxVideoSurface extends CallbackVideoSurface {

        JavaFxVideoSurface() {
            super(new JavaFxBufferFormatCallback(), new JavaFxRenderCallback(), true, VideoSurfaceAdapters.getVideoSurfaceAdapter());
        }

    }

    private class JavaFxBufferFormatCallback implements BufferFormatCallback {

        @Override
        public BufferFormat getBufferFormat(int sourceWidth, int sourceHeight) {
            DynamicVideoView.this.img = new WritableImage(sourceWidth, sourceHeight);
            DynamicVideoView.this.pixelWriter = img.getPixelWriter();

            Platform.runLater(() -> {
                DynamicVideoView.this.setWidth(sourceWidth);
                DynamicVideoView.this.setHeight(sourceHeight);
            });
            return new RV32BufferFormat(sourceWidth, sourceHeight);
        }

    }

    private final Semaphore semaphore = new Semaphore(1);

    private class JavaFxRenderCallback implements RenderCallback {

        @Override
        public void display(MediaPlayer mediaPlayer, ByteBuffer[] nativeBuffers, BufferFormat bufferFormat) {
            try {
                semaphore.acquire();
                pixelWriter.setPixels(0, 0, bufferFormat.getWidth(), bufferFormat.getHeight(), pixelFormat, nativeBuffers[0], bufferFormat.getPitches()[0]);
                semaphore.release();
            } catch (InterruptedException ignore) {
            }
        }

    }

    private void renderFrame() {
        GraphicsContext g = canvas.getGraphicsContext2D();

        double width = canvas.getWidth();
        double height = canvas.getHeight();

        //        g.setFill(new Color(0, 0, 0, 1));
        //        g.fillRect(0, 0, width, height);

        if (img != null) {
            double imageWidth = img.getWidth();
            double imageHeight = img.getHeight();

            double sx = width / imageWidth;
            double sy = height / imageHeight;

            double sf = Math.min(sx, sy);

            double scaledW = imageWidth * sf;
            double scaledH = imageHeight * sf;

            Affine ax = g.getTransform();

            g.translate((width - scaledW) / 2, (height - scaledH) / 2);

            if (sf != 1.0) {
                g.scale(sf, sf);
            }

            try {
                semaphore.acquire();
                g.drawImage(img, 0, 0);
                semaphore.release();
            } catch (InterruptedException ignore) {
            }

            g.setTransform(ax);
        }
    }

}
