package menagerie.util;


import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

public abstract class ThumbnailBuilder {

//    private static final String[] VLC_ARGS = {"--intf", "dummy", "--vout", "dummy", "--no-audio", "--no-osd", "--no-spu", "--no-stats", "--no-sub-autodetect-file", "--no-disable-screensaver", "--no-snapshot-preview"};
//    private static final MediaPlayer thumbnailMediaPlayer = new MediaPlayerFactory(VLC_ARGS).newHeadlessMediaPlayer();


    public static Image imageFromInputStream(InputStream is) throws IOException {
        BufferedImage bImage = ImageIO.read(is);
        return SwingFXUtils.toFXImage(bImage, null);
    }

    public static InputStream imageToInputStream(Image image) throws IOException {
        BufferedImage bi = SwingFXUtils.fromFXImage(image, null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bi, "png", baos);

        return new ByteArrayInputStream(baos.toByteArray());
    }

}
