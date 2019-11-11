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

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import menagerie.gui.Main;
import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;
import menagerie.util.Filters;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipFile;

/**
 * Dynamically sized view that can display images or videos.
 */
public class DynamicMediaView extends StackPane {

    private static final Logger LOGGER = Logger.getLogger(DynamicMediaView.class.getName());

    private DynamicVideoView videoView;
    private final PanZoomImageView imageView = new PanZoomImageView();
    private final TextArea textView = new TextArea();

    private final BorderPane pdfControlsPane;
    private final Label pdfPageLabel = new Label("0/0");
    private PDDocument currentPDF = null;
    private int currentPDFPageIndex = 0;


    public DynamicMediaView() {
        super();

        Button pdfLeftButton = new Button("<-");
        pdfLeftButton.setOnAction(event -> {
            if (currentPDFPageIndex > 0) {
                setPDFPage(currentPDFPageIndex - 1);
            }
        });
        Button pdfRightButton = new Button("->");
        pdfRightButton.setOnAction(event -> {
            if (currentPDFPageIndex < currentPDF.getNumberOfPages() - 1) {
                setPDFPage(currentPDFPageIndex + 1);
            }
        });
        HBox bottomHBox = new HBox(5, pdfLeftButton, pdfPageLabel, pdfRightButton);
        bottomHBox.setAlignment(Pos.CENTER);
        bottomHBox.setPadding(new Insets(5));
        pdfControlsPane = new BorderPane(null, null, null, bottomHBox, null);
        pdfControlsPane.setPickOnBounds(false);

        textView.setEditable(false);
        textView.setFocusTraversable(false);
        textView.setWrapText(true);
    }

    /**
     * Attempts to display a media item. If media item is a video and VLCJ is not loaded, nothing will be displayed.
     *
     * @param item Item to display.
     * @return True if successful, false otherwise.
     */
    public boolean preview(Item item) {
        if (getVideoView() != null) getVideoView().stop();
        imageView.setImage(null);
        hideAllViews();

        if (item instanceof MediaItem) {
            try {
                if (((MediaItem) item).isImage()) {
                    if (getVideoView() != null) getVideoView().stop();
                    imageView.setImage(((MediaItem) item).getImage());
                    showImageView();
                } else if (((MediaItem) item).isVideo() && getVideoView() != null) {
                    imageView.setImage(null);
                    getVideoView().startMedia(((MediaItem) item).getFile().getAbsolutePath());
                    showVideoView();
                } else if (Filters.RAR_NAME_FILTER.accept(((MediaItem) item).getFile())) {
                    try (Archive a = new Archive(new FileInputStream(((MediaItem) item).getFile()))) {
                        List<FileHeader> fileHeaders = a.getFileHeaders();
                        if (!fileHeaders.isEmpty()) {
                            try (InputStream is = a.getInputStream(fileHeaders.get(0))) {
                                imageView.setImage(new Image(is));
                            }
                        }
                    } catch (RarException | IOException | NullPointerException e) {
                        LOGGER.log(Level.INFO, "Failed to preview RAR: " + ((MediaItem) item).getFile());
                    }
                    showImageView();
                } else if (Filters.ZIP_NAME_FILTER.accept(((MediaItem) item).getFile())) {
                    try (ZipFile zip = new ZipFile(((MediaItem) item).getFile())) {
                        if (zip.entries().hasMoreElements()) {
                            try (InputStream is = zip.getInputStream(zip.entries().nextElement())) {
                                imageView.setImage(new Image(is));
                            }
                        }
                    } catch (IOException e) {
                        LOGGER.log(Level.INFO, "Failed to preview ZIP: " + ((MediaItem) item).getFile());
                    }
                } else if (Filters.PDF_NAME_FILTER.accept(((MediaItem) item).getFile())) {
                    if (currentPDF != null) currentPDF.close();
                    currentPDF = PDDocument.load(((MediaItem) item).getFile());
                    setPDFPage(0);
                    showPDFView();
                } else if (Files.probeContentType(((MediaItem) item).getFile().toPath()).equalsIgnoreCase("text/plain")) {
                    textView.setText(String.join("\n", Files.readAllLines(((MediaItem) item).getFile().toPath())));
                    showTextView();
                } else {
                    return false; // Unknown file type, can't preview it
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error previewing media: " + item, e);
            }
        } else if (item instanceof GroupItem) {
            if (!((GroupItem) item).getElements().isEmpty()) {
                preview(((GroupItem) item).getElements().get(0));
            }
        }

        return true;
    }

    /**
     * Hides both the video and the image views, if they exist.
     */
    private void hideAllViews() {
        getChildren().removeAll(getImageView(), textView, pdfControlsPane);
        if (getVideoView() != null) {
            getChildren().remove(getVideoView());
        }
    }

    /**
     * Shows the image view.
     */
    private void showImageView() {
        hideAllViews();
        getChildren().add(getImageView());
    }

    /**
     * Shows the video view, if VLCJ is loaded.
     */
    private void showVideoView() {
        hideAllViews();
        if (getVideoView() != null) {
            getChildren().add(getVideoView());
        }
    }

    /**
     * Shows the text view
     */
    private void showTextView() {
        hideAllViews();
        getChildren().add(textView);
    }

    private void showPDFView() {
        hideAllViews();
        getChildren().addAll(getImageView(), pdfControlsPane);
    }

    /**
     * Attempts to get the video view. If VLCJ not loaded and this is the first call to this method, video view will be constructed.
     *
     * @return The video view, or null if VLCJ is not loaded.
     */
    public DynamicVideoView getVideoView() {
        if (!Main.isVlcjLoaded()) return null;

        if (videoView == null) {
            videoView = new DynamicVideoView();
        }

        return videoView;
    }

    public PanZoomImageView getImageView() {
        return imageView;
    }

    private void setPDFPage(int page) {
        if (currentPDF == null || page < 0 || page >= currentPDF.getNumberOfPages()) return;

        currentPDFPageIndex = page;

        try {
            BufferedImage img = new PDFRenderer(currentPDF).renderImageWithDPI(page, 300);
            imageView.setImage(SwingFXUtils.toFXImage(img, null));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to render PDF page: " + page, e);
        }

        pdfPageLabel.setText((page + 1) + "/" + currentPDF.getNumberOfPages());
    }

    /**
     * @param mute The mute property for the media player, if the video view exists.
     */
    public void setMute(boolean mute) {
        if (getVideoView() != null) getVideoView().setMute(mute);
    }

    /**
     * @param repeat The repeat property for the media player, if the video view exists.
     */
    public void setRepeat(boolean repeat) {
        if (getVideoView() != null) getVideoView().setRepeat(repeat);
    }

    /**
     * @return True if the video is currently playing.
     */
    public boolean isPlaying() {
        return getVideoView() != null && getVideoView().isPlaying();
    }

    /**
     * Pauses the media player. No effect if not playing.
     */
    public void pause() {
        if (getVideoView() != null) getVideoView().pause();
    }

    /**
     * Plays the media player. No effect if already playing.
     */
    public void play() {
        if (getVideoView() != null) getVideoView().play();
    }

    /**
     * Releases VLCJ resources and invalidates the video view.
     */
    public void releaseVLCJ() {
        if (videoView != null) {
            Platform.runLater(() -> getChildren().remove(videoView));
            videoView.releaseVLCJ();
            videoView = null;
        }
    }

    public void stop() {
        if (getVideoView() != null) getVideoView().stop();
    }

}
