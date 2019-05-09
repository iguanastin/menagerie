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
