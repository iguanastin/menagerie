package menagerie.util;


import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public abstract class ThumbnailBuilder {

//    private static final String[] VLC_ARGS = {"--intf", "dummy", "--vout", "dummy", "--no-audio", "--no-osd", "--no-spu", "--no-stats", "--no-sub-autodetect-file", "--no-disable-screensaver", "--no-snapshot-preview"};
//    private static final MediaPlayer thumbnailMediaPlayer = new MediaPlayerFactory(VLC_ARGS).newHeadlessMediaPlayer();


//    public static Image makeThumbnail(ImageInfo info) {
//        String extension = info.getPath().getName().toLowerCase();
//        extension = extension.substring(extension.indexOf('.') + 1);
//
//        if (extension.equals("png") || extension.equals("jpg") || extension.equals("jpeg") || extension.equals("bmp")) {
//            return new Image(info.getPath().toURI().toString(), ImageInfo.THUMBNAIL_SIZE, ImageInfo.THUMBNAIL_SIZE, true, true, true);
//        } else if (extension.equals("gif")) {
//            //TODO: Make thumbnail still and have "GIF" text applied
//            return new Image(info.getPath().toURI().toString(), ImageInfo.THUMBNAIL_SIZE, ImageInfo.THUMBNAIL_SIZE, true, true, true);
//        } else if (extension.equals("webm") || extension.equals("mp4") || extension.equals("avi")) {
//            //TODO: Load video into VLCJ player and take snapshot
//            return null;
//        } else {
//            return null;
//        }
//    }

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
