package menagerie.gui.media;

import javafx.scene.layout.StackPane;
import menagerie.gui.Main;
import menagerie.model.menagerie.ImageInfo;

public class DynamicMediaView extends StackPane {

    private DynamicVideoView videoView;
    private PanZoomImageView imageView;


    public DynamicMediaView() {
        super();

//        videoView = new DynamicVideoView();
        imageView = new PanZoomImageView();

        getChildren().addAll(imageView);
    }

    public boolean preview(ImageInfo info) {
        if (info == null) {
            if (getVideoView() != null) getVideoView().getMediaPlayer().stop();
            getImageView().setImage(null);
            hideAllViews();
        } else if (info.isImage()) {
            if (getVideoView() != null) getVideoView().getMediaPlayer().stop();
            getImageView().setImage(info.getImage());
            showImageView();
        } else if (info.isVideo() && getVideoView() != null) {
            getImageView().setImage(null);
            getVideoView().getMediaPlayer().startMedia(info.getFile().getAbsolutePath());
            showVideoView();
        } else {
            return false; // Unknown file type, can't preview it
        }

        return true;
    }

    private void hideAllViews() {
        getImageView().setDisable(true);
        getImageView().setOpacity(0);
        if (getVideoView() != null) {
            getVideoView().setDisable(true);
            getVideoView().setOpacity(0);
        }
    }

    private void showImageView() {
        getImageView().setDisable(false);
        getImageView().setOpacity(1);
        if (getVideoView() != null) {
            getVideoView().setDisable(true);
            getVideoView().setOpacity(0);
        }
    }

    private void showVideoView() {
        if (getVideoView() != null) {
            getVideoView().setDisable(false);
            getVideoView().setOpacity(1);
        }
        getImageView().setDisable(true);
        getImageView().setOpacity(0);
    }

    private DynamicVideoView getVideoView() {
        if (!Main.VLCJ_LOADED) return null;

        if (videoView == null) {
            videoView = new DynamicVideoView();
            getChildren().add(videoView);
        }

        return videoView;
    }

    private PanZoomImageView getImageView() {
        return imageView;
    }

    public void releaseMediaPlayer() {
        if (videoView != null) videoView.getMediaPlayer().release();
    }

    public void setMute(boolean mute) {
        if (getVideoView() != null) getVideoView().getMediaPlayer().mute(mute);
    }

    public void setRepeat(boolean repeat) {
        if (getVideoView() != null) getVideoView().getMediaPlayer().setRepeat(repeat);
    }

    public boolean isPlaying() {
        return getVideoView() != null && getVideoView().getMediaPlayer().isPlaying();
    }

    public void pause() {
        if (getVideoView() != null) getVideoView().getMediaPlayer().pause();
    }

    public void play() {
        if (getVideoView() != null) getVideoView().getMediaPlayer().play();
    }

}
