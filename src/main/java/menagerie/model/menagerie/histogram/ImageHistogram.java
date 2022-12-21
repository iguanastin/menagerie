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

package menagerie.model.menagerie.histogram;


import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * A 4 channel histogram of an image that can be used to find similarity between two images.
 */
public class ImageHistogram {

    public static final int BIN_SIZE = 32;
    public static final int NUM_CHANNELS = 4;
    private static final double BLACK_AND_WHITE_CONFIDENCE = 0.25;

    private final double[] alpha;
    private final double[] red;
    private final double[] green;
    private final double[] blue;

    private Boolean colorful = null;


    /**
     * Constructs a histogram from streams as created by {@link #arrayAsInputStream(double[])}
     *
     * @param a Alpha channel stream.
     * @param r Red channel stream.
     * @param g Green channel stream.
     * @param b Blue channel stream.
     * @throws HistogramReadException If any error reading from streams.
     */
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

    /**
     * Constructs a histogram from a JavaFX image.
     *
     * @param image Image to create histogram of.
     * @throws HistogramReadException If image is not loaded yet, or image cannot retrieve pixel reader.
     */
    public ImageHistogram(Image image) throws HistogramReadException {
        alpha = new double[BIN_SIZE];
        red = new double[BIN_SIZE];
        green = new double[BIN_SIZE];
        blue = new double[BIN_SIZE];

        if (image.isBackgroundLoading() && image.getProgress() != 1) throw new HistogramReadException("Given media is not loaded yet");
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

    public double[] getAlpha() {
        return alpha;
    }

    public double[] getRed() {
        return red;
    }

    public double[] getGreen() {
        return green;
    }

    public double[] getBlue() {
        return blue;
    }

    /**
     * @return Alpha buckets as a stream.
     */
    public ByteArrayInputStream getAlphaAsInputStream() {
        return arrayAsInputStream(alpha);
    }

    /**
     * @return Red buckets as a stream.
     */
    public ByteArrayInputStream getRedAsInputStream() {
        return arrayAsInputStream(red);
    }

    /**
     * @return Green buckets as a stream.
     */
    public ByteArrayInputStream getGreenAsInputStream() {
        return arrayAsInputStream(green);
    }

    /**
     * @return Blue buckets as a stream.
     */
    public ByteArrayInputStream getBlueAsInputStream() {
        return arrayAsInputStream(blue);
    }

    /**
     * Parses the image and attempts to determine if the image is colorful, or greyscale.
     *
     * @return True if image has color variance within confidence {@link #BLACK_AND_WHITE_CONFIDENCE}
     */
    public boolean isColorful() {
        if (colorful == null) {
            double d = 0;

            for (int i = 0; i < BIN_SIZE; i++) {
                d += Math.max(Math.max(red[i], green[i]), blue[i]) - Math.min(Math.min(red[i], green[i]), blue[i]);
            }

            colorful = d > BLACK_AND_WHITE_CONFIDENCE;
        }

        return colorful;
    }

    /**
     * Compares two histograms and gets a percentage similarity.
     *
     * @param other Other histogram to compare to.
     * @return Percent similarity [0.0-1.0]. 1.0 being a perfect pixel-per-pixel match. 0.0 being the perfect opposite of each other.
     */
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

    /**
     * Converts an array into a stream.
     *
     * @param array Array to convert.
     * @return Stream containing array in byte format.
     */
    private static ByteArrayInputStream arrayAsInputStream(double[] array) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[BIN_SIZE * 8]);

        for (double d : array) {
            bb.putDouble(d);
        }

        return new ByteArrayInputStream(bb.array());
    }

    /**
     * Converts a stream into an array.
     *
     * @param in Stream to convert.
     * @return Array as represented by the bytes in the stream.
     * @throws IOException If stream does not have the right number of bytes.
     */
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
