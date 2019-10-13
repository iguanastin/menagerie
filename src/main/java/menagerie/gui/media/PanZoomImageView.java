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
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An ImageView that dynamically fits to the parent node and implements zooming and panning with the mouse.
 */
public class PanZoomImageView extends DynamicImageView {

    private static final double[] SCALES = {0.1, 0.13, 0.18, 0.24, 0.32, 0.42, 0.56, 0.75, 1, 1.25, 1.56, 1.95, 2.44, 3.05, 3.81, 4.76, 5.95, 7.44, 9.3};

    private double deltaX = 0, deltaY = 0;
    private DoubleProperty scale = new SimpleDoubleProperty(1);

    private double clickX, clickY;
    private double clickImageX, clickImageY;

    private boolean draggedThisClick = false;


    /**
     * Empty image view with panning and zooming functionality.
     */
    public PanZoomImageView() {
        super();
        setPickOnBounds(true);

        addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            if (event.getButton().equals(MouseButton.PRIMARY)) {
                deltaX = clickImageX + (clickX - event.getX()) * scale.get();
                deltaY = clickImageY + (clickY - event.getY()) * scale.get();
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
                double w = getImage().getWidth() / scale.get();
                double h = getImage().getHeight() / scale.get();
                if (deltaX == 0 && deltaY == 0 && (Math.abs(getFitWidth() - w) < 5 || Math.abs(getFitHeight() - h) < 5)) {
                    scale.set(1);
                    updateViewPort();
                } else {
                    fitImageToView();
                }
            }
        });
        addEventHandler(ScrollEvent.SCROLL, event -> {
            final double fitScale = getFitScale();
            final List<Double> work = new ArrayList<>();
            for (double v : SCALES) {
                work.add(v);
            }
            work.add(fitScale);
            Collections.sort(work);

            if (event.getDeltaY() < 0) {
                for (double d : work) {
                    if (d > scale.get()) {
                        scale.set(d);
                        break;
                    }
                }
            } else {
                Collections.reverse(work);
                for (double d : work) {
                    if (d < scale.get()) {
                        scale.set(d);
                        break;
                    }
                }
            }

            updateViewPort();
            event.consume();
        });

        addEventHandler(MouseEvent.MOUSE_ENTERED, event -> getScene().setCursor(Cursor.MOVE));
        addEventHandler(MouseEvent.MOUSE_EXITED, event -> getScene().setCursor(Cursor.DEFAULT));

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
            } else {
                fitImageToView();
            }
        });
    }

    /**
     * Fits the image to the view at 100% scale if possible. If image is larger than view, scales down to fit whole image in view.
     */
    public void fitImageToView() {
        deltaX = deltaY = 0;
        scale.set(1);

        updateViewPort();

        if (getImage() != null) {
            scale.set(getFitScale());
            updateViewPort();
        }
    }

    private double getFitScale() {
        double s = getImage().getWidth() / getFitWidth();
        if (getImage().getHeight() / getFitHeight() > s) s = getImage().getHeight() / getFitHeight();
        if (s < 1) s = 1;
        return s;
    }

    public DoubleProperty getScale() {
        return scale;
    }

    @Override
    public void resize(double width, double height) {
        setFitWidth(width);
        setFitHeight(height);

        if (getImage() != null) {
            updateViewPort();
        }
    }

    /**
     * Recalculates image viewport.
     */
    private void updateViewPort() {
        if (getImage() == null || getFitWidth() == 0 || getFitHeight() == 0) return;

        final double fitWidth = getFitWidth() * scale.get();
        final double fitHeight = getFitHeight() * scale.get();
        final double imageWidth = getImage().getWidth();
        final double imageHeight = getImage().getHeight();

        deltaX = Math.max(Math.min(deltaX, imageWidth / 2), -imageWidth / 2);
        deltaY = Math.max(Math.min(deltaY, imageHeight / 2), -imageHeight / 2);

        final double viewportX = (imageWidth - fitWidth) / 2 + deltaX;
        final double viewportY = (imageHeight - fitHeight) / 2 + deltaY;

        setViewport(new Rectangle2D(viewportX, viewportY, fitWidth, fitHeight));
    }

}