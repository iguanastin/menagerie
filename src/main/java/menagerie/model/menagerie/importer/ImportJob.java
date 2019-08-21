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

package menagerie.model.menagerie.importer;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import menagerie.gui.Main;
import menagerie.gui.MainController;
import menagerie.model.SimilarPair;
import menagerie.model.menagerie.*;
import menagerie.settings.MenagerieSettings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * A runnable job that will import a file.
 */
public class ImportJob {

    public enum Status {
        WAITING, IMPORTING, SUCCEEDED, FAILED_DUPLICATE, FAILED_IMPORT,

    }

    private ImporterThread importer = null;

    private URL url = null;
    private File downloadTo = null;
    private File file = null;
    private MediaItem item = null;
    private MediaItem duplicateOf = null;
    private GroupItem addToGroup = null;
    private List<SimilarPair<MediaItem>> similarTo = null;

    private volatile boolean needsDownload = false;
    private volatile boolean needsImport = true;
    private volatile boolean needsHash = true;
    private volatile boolean needsHist = true;
    private volatile boolean needsCheckDuplicate = true;
    private volatile boolean needsCheckSimilar = true;

    private final DoubleProperty progressProperty = new SimpleDoubleProperty(-1);
    private final ObjectProperty<Status> status = new SimpleObjectProperty<>(Status.WAITING);


    /**
     * Constructs a job that will download and import a file from the web.
     *
     * @param url URL of file to download.
     */
    public ImportJob(URL url, GroupItem addToGroup) {
        this.url = url;
        needsDownload = true;
        this.addToGroup = addToGroup;
    }

    /**
     * Constructs a job that will download and import a file from the web into a specified file.
     *
     * @param url        URL of file to download.
     * @param downloadTo File to download URL into.
     */
    public ImportJob(URL url, File downloadTo, GroupItem addToGroup) {
        this(url, addToGroup);
        this.downloadTo = downloadTo;
    }

    /**
     * Constructs a job that will import a local file.
     *
     * @param file File to import.
     */
    public ImportJob(File file, GroupItem addToGroup) {
        this.file = file;
        this.addToGroup = addToGroup;
    }

    /**
     * Synchronously runs this job. Downloads, imports, creates hash, creates historgram, finds duplicates, and finds similar items.
     *
     * @param menagerie Menagerie to import into.
     * @param settings  Application settings to import with.
     */
    void runJob(Menagerie menagerie, MenagerieSettings settings) {
        setStatus(Status.IMPORTING);

        if (tryDownload(settings)) {
            setStatus(Status.FAILED_IMPORT);
            return;
        }
        if (tryImport(menagerie, settings)) {
            setStatus(Status.FAILED_IMPORT);
            return;
        }
        tryHashHist();
        if (tryDuplicate(menagerie)) {
            setStatus(Status.FAILED_DUPLICATE);
            return;
        }
        if (addToGroup != null) {
            tryAddToGroup(addToGroup);
        }

        trySimilar(menagerie, settings);

        setStatus(Status.SUCCEEDED);
    }

    /**
     * Tries to download the file from the web and save it to {@link #file}
     *
     * @param settings Application settings to use.
     * @return True if the download fails.
     */
    private boolean tryDownload(MenagerieSettings settings) {
        if (needsDownload) {
            try {
                if (downloadTo == null) {
                    String folder = settings.defaultFolder.getValue();
                    if (folder == null || folder.isEmpty() || !Files.isDirectory(Paths.get(folder))) {
                        Main.log.warning(String.format("Default folder '%s' doesn't exist or isn't a folder", folder));
                        return true;
                    }
                    String filename = url.getPath().replaceAll("^.*/", "");

                    downloadTo = MainController.resolveDuplicateFilename(new File(folder, filename));
                }
                if (downloadTo.exists()) {
                    Main.log.warning(String.format("Attempted to download '%s' into pre-existing file '%s'", url.toString(), downloadTo.toString()));
                    return true;
                }

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.addRequestProperty("User-Agent", "Mozilla/4.0");
                ReadableByteChannel rbc = Channels.newChannel(conn.getInputStream());
                try (FileOutputStream fos = new FileOutputStream(downloadTo)) {
                    final long size = conn.getContentLengthLong();
                    final int chunkSize = 4096;
                    for (int i = 0; i < size; i += chunkSize) {
                        fos.getChannel().transferFrom(rbc, i, chunkSize);

                        progressProperty.set((double) i / size);
                    }

                }
                rbc.close();
                conn.disconnect();

                synchronized (this) {
                    file = downloadTo;
                }
                needsDownload = false;
            } catch (RuntimeException e) {
                Main.log.log(Level.WARNING, String.format("Unexpected exception while downloading from url: %s", url.toString()), e);
                return true;
            } catch (IOException e) {
                Main.log.log(Level.WARNING, String.format("Failed to download file from url: %s", url.toString()), e);
                return true;
            }
        }
        progressProperty.set(-1);
        return false;
    }

    /**
     * Tries to import the file into the Menagerie and store it in {@link #item}
     *
     * @param menagerie Menagerie to import into.
     * @return True if the import failed.
     */
    private boolean tryImport(Menagerie menagerie, MenagerieSettings settings) {
        if (needsImport) {
            synchronized (this) {
                item = menagerie.importFile(file);
            }

            if (item == null) {
                return true;
            } else {
                needsImport = false;

                // Add tags
                if (settings.tagTagme.getValue()) {
                    Tag tagme = menagerie.getTagByName("tagme");
                    if (tagme == null) tagme = menagerie.createTag("tagme");
                    item.addTag(tagme);
                }
                if (settings.tagImages.getValue() && item.isImage()) {
                    Tag image = menagerie.getTagByName("image");
                    if (image == null) image = menagerie.createTag("image");
                    item.addTag(image);
                }
                if (settings.tagVideos.getValue() && item.isVideo()) {
                    Tag video = menagerie.getTagByName("video");
                    if (video == null) video = menagerie.createTag("video");
                    item.addTag(video);
                }
            }
        }
        return false;
    }

    /**
     * Tries to construct and store an MD5 hash and a histogram for the item.
     */
    private void tryHashHist() {
        if (needsHash) {
            item.initializeMD5();
            needsHash = false;
        }
        if (needsHist) {
            item.initializeHistogram();
            needsHist = false;
        }
    }

    /**
     * Tries to find a duplicate item in the Menagerie and stores the existing duplicate in {@link #duplicateOf}
     *
     * @param menagerie Menagerie to search.
     * @return True if a duplicate exists.
     */
    private boolean tryDuplicate(Menagerie menagerie) {
        if (needsCheckDuplicate && item.getMD5() != null) {
            for (Item i : menagerie.getItems()) {
                if (i instanceof MediaItem && !i.equals(item) && ((MediaItem) i).getMD5() != null && ((MediaItem) i).getMD5().equalsIgnoreCase(item.getMD5())) {
                    synchronized (this) {
                        duplicateOf = (MediaItem) i;
                    }
                    menagerie.deleteItem(item);
                    needsCheckDuplicate = false;
                    needsCheckSimilar = false;
                    return true;
                }
            }

            needsCheckDuplicate = false;
        }
        return false;
    }

    private void tryAddToGroup(GroupItem group) {
        group.addItem(item);
    }

    /**
     * Tries to find similar items already imported and stores similar pairs in {@link #similarTo}
     *
     * @param menagerie Menagerie to find similar items in.
     * @param settings  Application settings to use.
     */
    private void trySimilar(Menagerie menagerie, MenagerieSettings settings) {
        if (needsCheckSimilar && item.getHistogram() != null) {
            synchronized (this) {
                similarTo = new ArrayList<>();
            }
            final double confidence = settings.duplicateConfidence.getValue();
            final double confidenceSquare = 1 - (1 - confidence) * (1 - confidence);
            boolean anyMinimallySimilar = false;
            for (Item i : menagerie.getItems()) {
                if (i instanceof MediaItem && !item.equals(i) && ((MediaItem) i).getHistogram() != null) {
                    double similarity = ((MediaItem) i).getSimilarityTo(item);

                    if (!anyMinimallySimilar && similarity > MediaItem.MIN_CONFIDENCE) {
                        anyMinimallySimilar = true;
                        if (((MediaItem) i).hasNoSimilar()) ((MediaItem) i).setHasNoSimilar(false);
                    }

                    if (similarity >= confidenceSquare || (similarity >= confidence && item.getHistogram().isColorful() && ((MediaItem) i).getHistogram().isColorful())) {
                        synchronized (this) {
                            similarTo.add(new SimilarPair<>(item, (MediaItem) i, similarity));
                        }
                    }
                }
            }

            if (!anyMinimallySimilar) item.setHasNoSimilar(true);

            needsCheckSimilar = false;
        }
    }

    /**
     * @return The imported item. Null if not yet imported.
     */
    public synchronized MediaItem getItem() {
        return item;
    }

    /**
     * @return The web URL. Null if not imported from web.
     */
    public synchronized URL getUrl() {
        return url;
    }

    /**
     * @return The file. Null if not yet downloaded.
     */
    public synchronized File getFile() {
        return file;
    }

    /**
     * @return The pre-existing duplicate. Null if not yet checked, or no duplicate found.
     */
    public synchronized MediaItem getDuplicateOf() {
        return duplicateOf;
    }

    /**
     * @return The list of similar pairs. Null if not checked.
     */
    public synchronized List<SimilarPair<MediaItem>> getSimilarTo() {
        return similarTo;
    }

    /**
     * @return The progress JavaFX Property.
     */
    public DoubleProperty progressProperty() {
        return progressProperty;
    }

    /**
     * @return The current progress.
     */
    public double getProgress() {
        return progressProperty.doubleValue();
    }

    /**
     * @return The status of this job.
     */
    public Status getStatus() {
        return status.get();
    }

    /**
     * @param status The new status to set this job as.
     */
    public void setStatus(Status status) {
        this.status.set(status);
    }

    public ObjectProperty<Status> statusProperty() {
        return status;
    }

    /**
     * @return Importer that this job will use.
     */
    private synchronized ImporterThread getImporter() {
        return importer;
    }

    /**
     * @param importer Importer to import with.
     */
    synchronized void setImporter(ImporterThread importer) {
        this.importer = importer;
    }

    /**
     * Cancels this job if it has not already been started.
     */
    public void cancel() {
        if (getStatus() == Status.WAITING) getImporter().cancel(this);
    }

}
