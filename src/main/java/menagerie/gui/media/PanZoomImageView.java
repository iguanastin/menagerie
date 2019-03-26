package menagerie.gui.media;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

public class PanZoomImageView extends DynamicImageView {

    private static final double MIN_SCALE = 0.05;
    private static final double MAX_SCALE = 8;
    private static final double SCALE_FACTOR = 300;

    private double deltaX = 0, deltaY = 0;
    private double scale = 1;

    private double clickX, clickY;
    private double clickImageX, clickImageY;

    private boolean draggedThisClick = false;


    public PanZoomImageView() {
        super();
        setPickOnBounds(true);

        addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            if (event.getButton().equals(MouseButton.PRIMARY)) {
                deltaX = clickImageX + (clickX - event.getX()) * scale;
                deltaY = clickImageY + (clickY - event.getY()) * scale;
                updateViewPort();

                draggedThisClick = true;

                event.consume();
            }
        });
        addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton().equals(MouseButton.PRIMARY)) {
                draggedThisClick = false;
                clickX = event.getX();
                clickY = event.getY();
                clickImageX = deltaX;
                clickImageY = deltaY;
            }
        });
        addEventHandler(MouseEvent.MOUSE_RELEASED, event -> {
            if (event.getButton().equals(MouseButton.PRIMARY) && getImage() != null && !draggedThisClick) {
                double w = getImage().getWidth() / scale;
                double h = getImage().getHeight() / scale;
                if (deltaX == 0 && deltaY == 0 && (Math.abs(getFitWidth() - w) < 5 || Math.abs(getFitHeight() - h) < 5)) {
                    scale = 1;
                    updateViewPort();
                } else {
                    fitImageToView();
                }
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