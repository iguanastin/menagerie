package menagerie.model;

import menagerie.model.menagerie.MediaItem;

public class SimilarPair {

    private final MediaItem img1, img2;
    private final double similarity;


    public SimilarPair(MediaItem img1, MediaItem img2, double similarity) {
        this.img1 = img1;
        this.img2 = img2;
        this.similarity = similarity;
    }

    public double getSimilarity() {
        return similarity;
    }

    public MediaItem getImg1() {
        return img1;
    }

    public MediaItem getImg2() {
        return img2;
    }

}
