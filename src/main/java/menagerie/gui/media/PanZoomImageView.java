package menagerie.gui.media;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;

public class PanZoomImageView extends ImageView {

    private static final double MIN_SCALE = 0.05;
    private static final double MAX_SCALE = 8;
    private static final double SCALE_FACTOR = 300;

    private double deltaX = 0, deltaY = 0;
    private double scale = 1;

    private double clickX, clickY;
    private double clickImageX, clickImageY;


    public PanZoomImageView() {
        super();
        setPickOnBounds(true);

        addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            deltaX = clickImageX + (clickX - event.getX()) * scale;
            deltaY = clickImageY + (clickY - event.getY()) * scale;
            updateViewPort();

            event.consume();
        });
        addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton().equals(MouseButton.PRIMARY)) {
                clickX = event.getX();
                clickY = event.getY();
                clickImageX = deltaX;
                clickImageY = deltaY;
            } else if (event.getButton() == MouseButton.SECONDARY) {
                fitImageToView();
            }
        });
        addEventHandler(ScrollEvent.SCROLL, event -> {
            scale -= event.getDeltaY() / (SCALE_FACTOR / scale);
            scale = Math.max(MIN_SCALE, Math.min(scale, MAX_SCALE));
            updateViewPort();

            event.consume();
        });

        imageProperty().addListener((observable, oldValue, image) -> {
            if (image != null) {
                if (image.isBackgroundLoading() && image.getProgress() != 1.0) {
                    image.progressProperty().addListener((observable1, oldValue1, newValue) -> {
                        if (!image.isError() && newValue.doubleValue() == 1.0) {
                            fitImageToView();
                        }
                    });
                } else if (getFitWidth() != 0 && getFitHeight() != 0) {
                    fitImageToView();
                } else {
                    Platform.runLater(this::fitImageToView);
                }

                updateViewPort();
            }
        });
    }

    private void fitImageToView() {
        deltaX = deltaY = 0;
        scale = 1;

        updateViewPort();

        if (getImage() == null) return;

        scale = getImage().getWidth() / getFitWidth();
        if (getImage().getHeight() / getFitHeight() > scale) scale = getImage().getHeight() / getFitHeight();
        if (scale < 1) scale = 1;

        updateViewPort();
    }

    @Override
    public double minWidth(double height) {
        return 40;
    }

    @Override
    public double prefWidth(double height) {
        Image I = getImage();
        if (I == null) return minWidth(height);
        return I.getWidth();
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
        Image I = getImage();
        if (I == null) return minHeight(width);
        return I.getHeight();
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
        setFitWidth(width);
        setFitHeight(height);

        if (getImage() != null) {
            updateViewPort();
        }
    }

    private void updateViewPort() {
        if (getImage() == null || getFitWidth() == 0 || getFitHeight() == 0) return;

        final double fitWidth = getFitWidth() * scale;
        final double fitHeight = getFitHeight() * scale;
        final double imageWidth = getImage().getWidth();
        final double imageHeight = getImage().getHeight();

        deltaX = Math.max(Math.min(deltaX, imageWidth / 2), -imageWidth / 2);
        deltaY = Math.max(Math.min(deltaY, imageHeight / 2), -imageHeight / 2);

        final double viewportX = (imageWidth - fitWidth) / 2 + deltaX;
        final double viewportY = (imageHeight - fitHeight) / 2 + deltaY;

        setViewport(new Rectangle2D(viewportX, viewportY, fitWidth, fitHeight));
    }

}