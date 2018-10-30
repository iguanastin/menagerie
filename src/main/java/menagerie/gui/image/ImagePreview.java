package menagerie.gui.image;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;


public class ImagePreview extends ImageView {

    private static final int MIN_PIXELS = 100;


    public ImagePreview(Image image) {
        super(image);

        initialize();
    }

    public ImagePreview() {
        super();

        initialize();
    }

    private void initialize() {
        ObjectProperty<Point2D> mouseDown = new SimpleObjectProperty<>();

        imageProperty().addListener((observable, oldValue, newValue) -> {
            reset(newValue.getWidth(), newValue.getHeight());
        });
        setOnMousePressed(e -> {
            Point2D mousePress = imageViewToImage(new Point2D(e.getX(), e.getY()));
            mouseDown.set(mousePress);
        });
        setOnMouseDragged(e -> {
            Point2D dragPoint = imageViewToImage(new Point2D(e.getX(), e.getY()));
            shift(dragPoint.subtract(mouseDown.get()));
            mouseDown.set(imageViewToImage(new Point2D(e.getX(), e.getY())));
        });
        setOnScroll(e -> {
            double delta = -e.getDeltaY();
            Rectangle2D viewport = getViewport();

            double scale = clamp(Math.pow(1.01, delta),
                    // don't scale so we're zoomed in to fewer than MIN_PIXELS in any direction:
                    Math.min(MIN_PIXELS / viewport.getWidth(), MIN_PIXELS / viewport.getHeight()),

                    // don't scale so that we're bigger than image dimensions:
                    Math.max(getImage().getWidth() / viewport.getWidth(), getImage().getHeight() / viewport.getHeight())
            );

            Point2D mouse = imageViewToImage(new Point2D(e.getX(), e.getY()));

            double newWidth = viewport.getWidth() * scale;
            double newHeight = viewport.getHeight() * scale;

            // To keep the visual point under the mouse from moving, we need
            // (x - newViewportMinX) / (x - currentViewportMinX) = scale
            // where x is the mouse X coordinate in the image

            // solving this for newViewportMinX gives

            // newViewportMinX = x - (x - currentViewportMinX) * scale

            // we then clamp this value so the image never scrolls out
            // of the imageview:

            double newMinX = clamp(mouse.getX() - (mouse.getX() - viewport.getMinX()) * scale, 0, getImage().getWidth() - newWidth);
            double newMinY = clamp(mouse.getY() - (mouse.getY() - viewport.getMinY()) * scale, 0, getImage().getHeight() - newHeight);

            setViewport(new Rectangle2D(newMinX, newMinY, newWidth, newHeight));
        });
        setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && getImage() != null) {
                reset(getImage().getWidth(), getImage().getHeight());
            }
        });
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
        if (getImage() == null) {
            setFitWidth(width);
            setFitHeight(height);
        } else {
            double scale = 1;
            if (scale * getImage().getWidth() > width) scale = width / getImage().getWidth();
            if (scale * getImage().getHeight() > height) scale = height / getImage().getHeight();

            setFitWidth(getImage().getWidth() * scale);
            setFitHeight(getImage().getHeight() * scale);
        }
    }

    // reset to the top left:
    private void reset(double width, double height) {
        setViewport(new Rectangle2D(0, 0, width, height));
    }

    // shift the viewport of the imageView by the specified delta, clamping so
    // the viewport does not move off the actual image:
    private void shift(Point2D delta) {
        Rectangle2D viewport = getViewport();

        double width = getImage().getWidth();
        double height = getImage().getHeight();

        double maxX = width - viewport.getWidth();
        double maxY = height - viewport.getHeight();

        double minX = clamp(viewport.getMinX() - delta.getX(), 0, maxX);
        double minY = clamp(viewport.getMinY() - delta.getY(), 0, maxY);

        setViewport(new Rectangle2D(minX, minY, viewport.getWidth(), viewport.getHeight()));
    }

    private double clamp(double value, double min, double max) {
        if (value < min)
            return min;
        if (value > max)
            return max;
        return value;
    }

    // convert mouse coordinates in the imageView to coordinates in the actual image:
    private Point2D imageViewToImage(Point2D imageViewCoordinates) {
        double xProportion = imageViewCoordinates.getX() / getBoundsInLocal().getWidth();
        double yProportion = imageViewCoordinates.getY() / getBoundsInLocal().getHeight();

        Rectangle2D viewport = getViewport();
        return new Point2D(viewport.getMinX() + xProportion * viewport.getWidth(), viewport.getMinY() + yProportion * viewport.getHeight());
    }

}
