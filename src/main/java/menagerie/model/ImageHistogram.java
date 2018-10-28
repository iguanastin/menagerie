package menagerie.model;


import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;

public final class ImageHistogram {

    private final double[][] bins;


    public ImageHistogram(double[][] bins) {
        this.bins = bins;
    }

    public ImageHistogram(Image image) throws HistogramReadException {
        bins = new double[4][64];

        if (image.isBackgroundLoading() && image.getProgress() != 1)
            throw new HistogramReadException("Given image is not loaded yet");
        PixelReader pixelReader = image.getPixelReader();
        if (pixelReader == null) throw new HistogramReadException("Unable to get PixelReader");

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = pixelReader.getArgb(x, y);
                int a = (0xff & (argb >> 24));
                int r = (0xff & (argb >> 16));
                int g = (0xff & (argb >> 8));
                int b = (0xff & argb);

                bins[0][a / (256 / bins[0].length)]++;
                bins[1][r / (256 / bins[0].length)]++;
                bins[2][g / (256 / bins[0].length)]++;
                bins[3][b / (256 / bins[0].length)]++;
            }
        }

        final long pixelCount = (long) (image.getWidth() * image.getHeight());
        for (int i = 0; i < bins[0].length; i++) {
            bins[0][i] /= pixelCount;
            bins[1][i] /= pixelCount;
            bins[2][i] /= pixelCount;
            bins[3][i] /= pixelCount;
        }
    }

    public double[][] getBins() {
        return bins;
    }

    public double getSimilarity(ImageHistogram other) {
        double da = 0, dr = 0, dg = 0, db = 0;

        for (int i = 0; i < bins[0].length; i++) {
            da += Math.abs(bins[0][i] - other.bins[0][i]);
            dr += Math.abs(bins[1][i] - other.bins[1][i]);
            dg += Math.abs(bins[2][i] - other.bins[2][i]);
            db += Math.abs(bins[3][i] - other.bins[3][i]);
        }

        return 1 - (da + dr + dg + db) / 8;
    }

    public boolean isSimilar(ImageHistogram other, double confidence) {
        return getSimilarity(other) >= confidence;
    }

}
