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
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import menagerie.gui.Main;
import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;
import menagerie.util.Filters;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.logging.Level;
import java.util.zip.ZipFile;

/**
 * Dynamically sized view that can display images or videos.
 */
public class DynamicMediaView extends StackPane {

    private DynamicVideoView videoView;
    private final PanZoomImageView imageView = new PanZoomImageView();
    private final TextArea textView = new TextArea();


    public DynamicMediaView() {
        super();

        textView.setEditable(false);
        textView.setFocusTraversable(false);
        textView.setWrapText(true);
        getChildren().addAll(imageView, textView);
    }

    /**
     * Attempts to display a media item. If media item is a video and VLCJ is not loaded, nothing will be displayed.
     *
     * @param item Item to display.
     * @return True if successful, false otherwise.
     */
    public boolean preview(Item item) {
        if (getVideoView() != null) getVideoView().stop();
        getImageView().setImage(null);
        hideAllViews();

        if (item instanceof MediaItem) {
            try {
                if (((MediaItem) item).isImage()) {
                    if (getVideoView() != null) getVideoView().stop();
                    getImageView().setImage(((MediaItem) item).getImage());
                    showImageView();
                } else if (((MediaItem) item).isVideo() && getVideoView() != null) {
                    getImageView().setImage(null);
                    getVideoView().startMedia(((MediaItem) item).getFile().getAbsolutePath());
                    showVideoView();
                } else if (Filters.RAR_NAME_FILTER.accept(((MediaItem) item).getFile())) {
                    try (Archive a = new Archive(new FileInputStream(((MediaItem) item).getFile()))) {
                        List<FileHeader> fileHeaders = a.getFileHeaders();
                        if (!fileHeaders.isEmpty()) {
                            try (InputStream is = a.getInputStream(fileHeaders.get(0))) {
                                getImageView().setImage(new Image(is));
                            }
                        }
                    } catch (RarException | IOException e) {
                        Main.log.log(Level.SEVERE, "Failed to preview RAR: " + ((MediaItem) item).getFile(), e);
                    }
                    showImageView();
                } else if (Filters.ZIP_NAME_FILTER.accept(((MediaItem) item).getFile())) {
                    try (ZipFile zip = new ZipFile(((MediaItem) item).getFile())) {
                        if (zip.entries().hasMoreElements()) {
                            try (InputStream is = zip.getInputStream(zip.entries().nextElement())) {
                                getImageView().setImage(new Image(is));
                            }
                        }
                    } catch (IOException e) {
                        Main.log.log(Level.SEVERE, "Failed to preview ZIP: " + ((MediaItem) item).getFile(), e);
                    }
                } else if (Files.probeContentType(((MediaItem) item).getFile().toPath()).equalsIgnoreCase("text/plain")) {
                    textView.setText(String.join("\n", Files.readAllLines(((MediaItem) item).getFile().toPath())));
                    showTextView();
                } else {
                    return false; // Unknown file type, can't preview it
                }
            } catch (IOException e) {
                Main.log.log(Level.SEVERE, "Error previewing media: " + item, e);
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
        getImageView().setDisable(true);
        getImageView().setOpacity(0);
        if (getVideoView() != null) {
            getVideoView().setDisable(true);
            getVideoView().setOpacity(0);
        }
        getTextView().setDisable(true);
        getTextView().setOpacity(0);
    }

    /**
     * Shows the image view.
     */
    private void showImageView() {
        hideAllViews();
        getImageView().setDisable(false);
        getImageView().setOpacity(1);
    }

    /**
     * Shows the video view, if VLCJ is loaded.
     */
    private void showVideoView() {
        hideAllViews();
        if (getVideoView() != null) {
            getVideoView().setDisable(false);
            getVideoView().setOpacity(1);
        }
    }

    /**
     * Shows the text view
     */
    private void showTextView() {
        hideAllViews();
        textView.setOpacity(1);
        textView.setDisable(false);
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
            getChildren().add(videoView);
        }

        return videoView;
    }

    /**
     * @return The image view.
     */
    public PanZoomImageView getImageView() {
        return imageView;
    }

    /**
     * @return The text view.
     */
    public TextArea getTextView() {
        return textView;
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
