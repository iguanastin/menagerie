package menagerie.util;


import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility class containing methods for converting JavaFX images to input streams and vice versa.
 */
public abstract class ImageInputStreamConverter {


    /**
     * Constructs a JavaFX image from an input stream.
     *
     * @param is Input stream to convert.
     * @return The image that the input stream contains.
     */
    public static Image imageFromInputStream(InputStream is) {
        return new Image(is);
    }

    /**
     * Converts a JavaFX image into an input stream.
     *
     * @param image Image to convert.
     * @return An input stream of the image. Null if the image cannot be converted into a BufferedImage.
     * @throws IOException If an IO error occurred.
     */
    public static InputStream imageToInputStream(Image image) throws IOException {
        BufferedImage bi = SwingFXUtils.fromFXImage(image, null);
        if (bi == null) return null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bi, "png", baos);
        byte[] byteArray = baos.toByteArray();
        baos.close();

        return new ByteArrayInputStream(byteArray);
    }

}
