package menagerie.model.menagerie.importer;

import menagerie.model.SimilarPair;
import menagerie.model.menagerie.ImageInfo;

import java.io.File;
import java.util.List;

public class ImportJobListener {

    public void downloadFailed(Exception e) {}
    public void downloadSucceeded(File file) {}
    public void downloadProgress(long bytesDown, long totalBytes) {}

    public void importFailed() {}
    public void importSucceeded(ImageInfo item) {}

    public void foundDuplicate(ImageInfo imported, ImageInfo existing) {}

    public void foundSimilar(List<SimilarPair> pairs) {}

    public void finishedJob(ImageInfo item) {}

}
