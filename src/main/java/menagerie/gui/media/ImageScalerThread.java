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

import com.mortennobel.imagescaling.AdvancedResizeOp;
import com.mortennobel.imagescaling.ResampleFilters;
import com.mortennobel.imagescaling.ResampleOp;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import menagerie.util.CancellableThread;
import menagerie.util.listeners.ObjectListener;

import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ImageScalerThread extends CancellableThread {

    private static final Logger LOGGER = Logger.getLogger(ImageScalerThread.class.getName());

    private Image source = null;
    private double scale = 1;
    private ObjectListener<Image> callback = null;


    @Override
    public void run() {
        while (running) {
            Image source;
            double scale;
            ObjectListener<Image> callback;

            while (true) {
                synchronized (this) {
                    source = this.source;
                    scale = this.scale;
                    callback = this.callback;
                    clear();

                    if (source == null || scale < 0 || callback == null) {
                        try {
                            wait();
                        } catch (InterruptedException ignore) {
                        }
                    } else {
                        break;
                    }
                }
            }

            try {
                BufferedImage bimg = SwingFXUtils.fromFXImage(source, null);

                ResampleOp resizeOp = new ResampleOp((int) (bimg.getWidth() / scale), (int) (bimg.getHeight() / scale));
                resizeOp.setUnsharpenMask(AdvancedResizeOp.UnsharpenMask.Normal);
                resizeOp.setFilter(ResampleFilters.getLanczos3Filter());
                BufferedImage scaledImage = resizeOp.filter(bimg, bimg);

                callback.pass(SwingFXUtils.toFXImage(scaledImage, null));
            } catch (Throwable e) {
                LOGGER.log(Level.SEVERE, "Unexpected exception while scaling image (scale:" + scale + ", width:" + source.getWidth() + ", height:" + source.getHeight() + ", callback:" + callback + ")", e);
            }
        }
    }

    public synchronized void clear() {
        source = null;
        scale = 1;
        callback = null;
    }

    public synchronized void enqueue(Image source, double scale, ObjectListener<Image> callback) {
        this.source = source;
        this.scale = scale;
        this.callback = callback;
        this.notifyAll();
    }

}
