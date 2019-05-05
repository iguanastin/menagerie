package menagerie.gui.media;

import javafx.application.Platform;
import javafx.scene.layout.StackPane;
import menagerie.gui.Main;
import menagerie.model.menagerie.MediaItem;

/**
 * Dynamically sized view that can display images or videos.
 */
public class DynamicMediaView extends StackPane {

    private DynamicVideoView videoView;
    private final PanZoomImageView imageView;


    public DynamicMediaView() {
        super();

        //        videoView = new DynamicVideoView();
        imageView = new PanZoomImageView();

        getChildren().addAll(imageView);
    }

    /**
     * Attempts to display a media item. If media item is a video and VLCJ is not loaded, nothing will be displayed.
     *
     * @param item Item to display.
     * @return True if successful, false otherwise.
     */
    public boolean preview(MediaItem item) {
        if (item == null) {
            if (getVideoView() != null) getVideoView().stop();
            getMediaView().setImage(null);
            hideAllViews();
        } else if (item.isImage()) {
            if (getVideoView() != null) getVideoView().stop();
            getMediaView().setImage(item.getImage());
            showImageView();
        } else if (item.isVideo() && getVideoView() != null) {
            getMediaView().setImage(null);
            getVideoView().startMedia(item.getFile().getAbsolutePath());
            showVideoView();
        } else {
            if (getVideoView() != null) getVideoView().stop();
            getMediaView().setImage(null);
            hideAllViews();
            return false; // Unknown file type, can't preview it
        }

        return true;
    }

    /**
     * Hides both the video and the image views, if they exist.
     */
    private void hideAllViews() {
        getMediaView().setDisable(true);
        getMediaView().setOpacity(0);
        if (getVideoView() != null) {
            getVideoView().setDisable(true);
            getVideoView().setOpacity(0);
        }
    }

    /**
     * Shows the image view.
     */
    private void showImageView() {
        getMediaView().setDisable(false);
        getMediaView().setOpacity(1);
        if (getVideoView() != null) {
            getVideoView().setDisable(true);
            getVideoView().setOpacity(0);
        }
    }

    /**
     * Shows the video view, if VLCJ is loaded.
     */
    private void showVideoView() {
        if (getVideoView() != null) {
            getVideoView().setDisable(false);
            getVideoView().setOpacity(1);
        }
        getMediaView().setDisable(true);
        getMediaView().setOpacity(0);
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
    public PanZoomImageView getMediaView() {
        return imageView;
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
