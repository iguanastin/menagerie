package menagerie.gui.image;

import javafx.scene.layout.StackPane;
import menagerie.model.menagerie.ImageInfo;

public class DynamicMediaView extends StackPane {

    private DynamicVideoView videoView;
    private DynamicImageView imageView;


    public DynamicMediaView() {
        super();

//        videoView = new DynamicVideoView();
        imageView = new DynamicImageView();

        getChildren().addAll(imageView);
    }

    public boolean preview(ImageInfo info) {
        if (info == null) {
            getVideoView().getMediaPlayer().stop();
            getImageView().setImage(null);
            hideAllViews();
        } else if (info.isImage()) {
            getVideoView().getMediaPlayer().stop();
            getImageView().setImage(info.getImage());
            showImageView();
        } else if (info.isVideo()) {
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
        getVideoView().setDisable(true);
        getVideoView().setOpacity(0);
    }

    private void showImageView() {
        getImageView().setDisable(false);
        getImageView().setOpacity(1);
        getVideoView().setDisable(true);
        getVideoView().setOpacity(0);
    }

    private void showVideoView() {
        getVideoView().setDisable(false);
        getVideoView().setOpacity(1);
        getImageView().setDisable(true);
        getImageView().setOpacity(0);
    }

    private DynamicVideoView getVideoView() {
        if (videoView == null) {
            videoView = new DynamicVideoView();
            getChildren().add(videoView);
        }

        return videoView;
    }

    private DynamicImageView getImageView() {
        return imageView;
    }

    public void releaseMediaPlayer() {
        if (videoView != null) videoView.getMediaPlayer().release();
    }

    public void setMute(boolean mute) {
        getVideoView().getMediaPlayer().mute(mute);
    }

    public void setRepeat(boolean repeat) {
        getVideoView().getMediaPlayer().setRepeat(repeat);
    }

    public boolean isPlaying() {
        return getVideoView().getMediaPlayer().isPlaying();
    }

    public void pause() {
        getVideoView().getMediaPlayer().pause();
    }

    public void play() {
        getVideoView().getMediaPlayer().play();
    }

}
