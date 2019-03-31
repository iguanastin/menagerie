package menagerie.model.menagerie.histogram;


import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public final class ImageHistogram {

    private static final int BIN_SIZE = 32;
    private static final double BLACK_AND_WHITE_CONFIDENCE = 0.25;

    private final double[] alpha;
    private final double[] red;
    private final double[] green;
    private final double[] blue;

    private Boolean blackAndWhite = null;


    public ImageHistogram(InputStream a, InputStream r, InputStream g, InputStream b) throws HistogramReadException {
        try {
            this.alpha = inputStreamAsArray(a);
            this.red = inputStreamAsArray(r);
            this.green = inputStreamAsArray(g);
            this.blue = inputStreamAsArray(b);
        } catch (IOException e) {
            throw new HistogramReadException("InputStream of invalid length");
        }
    }

    public ImageHistogram(Image image) throws HistogramReadException {
        alpha = new double[BIN_SIZE];
        red = new double[BIN_SIZE];
        green = new double[BIN_SIZE];
        blue = new double[BIN_SIZE];

        if (image.isBackgroundLoading() && image.getProgress() != 1)
            throw new HistogramReadException("Given media is not loaded yet");
        PixelReader pixelReader = image.getPixelReader();
        if (pixelReader == null) throw new HistogramReadException("Unable to get PixelReader");

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int color = pixelReader.getArgb(x, y);
                int a = (0xff & (color >> 24));
                int r = (0xff & (color >> 16));
                int g = (0xff & (color >> 8));
                int b = (0xff & color);

                alpha[a / (256 / BIN_SIZE)]++;
                red[r / (256 / BIN_SIZE)]++;
                green[g / (256 / BIN_SIZE)]++;
                blue[b / (256 / BIN_SIZE)]++;
            }
        }

        final long pixelCount = (long) (image.getWidth() * image.getHeight());
        for (int i = 0; i < BIN_SIZE; i++) {
            alpha[i] /= pixelCount;
            red[i] /= pixelCount;
            green[i] /= pixelCount;
            blue[i] /= pixelCount;
        }
    }

    public ByteArrayInputStream getAlphaAsInputStream() {
        return arrayAsInputStream(alpha);
    }

    public ByteArrayInputStream getRedAsInputStream() {
        return arrayAsInputStream(red);
    }

    public ByteArrayInputStream getGreenAsInputStream() {
        return arrayAsInputStream(green);
    }

    public ByteArrayInputStream getBlueAsInputStream() {
        return arrayAsInputStream(blue);
    }

    public boolean isColorful() {
        if (blackAndWhite == null) {
            double d = 0;

            for (int i = 0; i < BIN_SIZE; i++) {
                d += Math.max(Math.max(red[i], green[i]), blue[i]) - Math.min(Math.min(red[i], green[i]), blue[i]);
            }

            blackAndWhite = d < BLACK_AND_WHITE_CONFIDENCE;
        }

        return blackAndWhite;
    }

    public double getSimilarity(ImageHistogram other) {
        double da = 0, dr = 0, dg = 0, db = 0;

        for (int i = 0; i < BIN_SIZE; i++) {
            da += Math.abs(alpha[i] - other.alpha[i]);
            dr += Math.abs(red[i] - other.red[i]);
            dg += Math.abs(green[i] - other.green[i]);
            db += Math.abs(blue[i] - other.blue[i]);
        }

        return 1 - (da + dr + dg + db) / 8;
    }

    private static ByteArrayInputStream arrayAsInputStream(double[] array) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[BIN_SIZE * 8]);

        for (double d : array) {
            bb.putDouble(d);
        }

        return new ByteArrayInputStream(bb.array());
    }

    private static double[] inputStreamAsArray(InputStream in) throws IOException {
        byte[] b = new byte[BIN_SIZE * 8];
        if (in.read(b) != BIN_SIZE * 8) return null;

        ByteBuffer bb = ByteBuffer.wrap(b);
        double[] result = new double[BIN_SIZE];

        for (int i = 0; i < BIN_SIZE; i++) {
            result[i] = bb.getDouble();
        }

        return result;
    }

}
