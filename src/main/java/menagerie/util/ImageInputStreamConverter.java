package menagerie.util;


import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public abstract class ImageInputStreamConverter {


    public static Image imageFromInputStream(InputStream is) {
        return new Image(is);
    }

    public static InputStream imageToInputStream(Image image) throws IOException {
        BufferedImage bi = SwingFXUtils.fromFXImage(image, null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (bi == null) return null;
        ImageIO.write(bi, "png", baos);

        return new ByteArrayInputStream(baos.toByteArray());
    }

}
