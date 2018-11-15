package menagerie.gui.image;

import com.sun.jna.Memory;
import javafx.application.Platform;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Screen;
import uk.co.caprica.vlcj.component.DirectMediaPlayerComponent;
import uk.co.caprica.vlcj.player.direct.BufferFormat;
import uk.co.caprica.vlcj.player.direct.BufferFormatCallback;
import uk.co.caprica.vlcj.player.direct.DirectMediaPlayer;
import uk.co.caprica.vlcj.player.direct.format.RV32BufferFormat;

import java.nio.ByteBuffer;

public class DynamicVLCJView extends BorderPane {


    private WritablePixelFormat<ByteBuffer> pixelFormat;
    private FloatProperty videoSourceRatioProperty;
    private WritableImage writableImage;
    private ImageView imageView;
    private DirectMediaPlayerComponent mediaPlayerComponent;


    public DynamicVLCJView() {
        super();

        videoSourceRatioProperty = new SimpleFloatProperty(0.4f);
        pixelFormat = PixelFormat.getByteBgraPreInstance();
        mediaPlayerComponent = new CanvasPlayerComponent();
        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        writableImage = new WritableImage((int) visualBounds.getWidth(), (int) visualBounds.getHeight());
        imageView = new ImageView(writableImage);
        setCenter(imageView);

        widthProperty().addListener((observable, oldValue, newValue) -> fitImageViewSize(newValue.floatValue(), (float) getHeight()));
        heightProperty().addListener((observable, oldValue, newValue) -> fitImageViewSize((float) getWidth(), newValue.floatValue()));
        videoSourceRatioProperty.addListener((observable, oldValue, newValue) -> fitImageViewSize((float) getWidth(), (float) getHeight()));
    }

    public DirectMediaPlayer getMediaPlayer() {
        return mediaPlayerComponent.getMediaPlayer();
    }

    private void fitImageViewSize(float width, float height) {
        float fitHeight = videoSourceRatioProperty.get() * width;
        if (fitHeight > height) {
            imageView.setFitHeight(height);
            double fitWidth = height / videoSourceRatioProperty.get();
            imageView.setFitWidth(fitWidth);
            imageView.setX((width - fitWidth) / 2);
            imageView.setY(0);
        } else {
            imageView.setFitWidth(width);
            imageView.setFitHeight(fitHeight);
            imageView.setY((height - fitHeight) / 2);
            imageView.setX(0);
        }
    }

    private class CanvasPlayerComponent extends DirectMediaPlayerComponent {

        public CanvasPlayerComponent() {
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

}
