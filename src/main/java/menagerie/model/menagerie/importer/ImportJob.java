package menagerie.model.menagerie.importer;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import menagerie.gui.MainController;
import menagerie.model.SimilarPair;
import menagerie.model.menagerie.ImageInfo;
import menagerie.model.menagerie.Menagerie;
import menagerie.model.Settings;

import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ImportJob {

    public enum Status {
        WAITING,
        IMPORTING,
        SUCCEEDED,
        SUCCEEDED_SIMILAR,
        FAILED_DUPLICATE,
        FAILED_IMPORT,

    }

    private ImporterThread importer = null;

    private URL url = null;
    private File file = null;
    private ImageInfo item = null;
    private ImageInfo duplicateOf = null;
    private List<SimilarPair> similarTo = null;

    private volatile boolean needsDownload = false;
    private volatile boolean needsImport = true;
    private volatile boolean needsHash = true;
    private volatile boolean needsHist = true;
    private volatile boolean needsCheckDuplicate;
    private volatile boolean needsCheckSimilar;

    private final ObjectProperty<Status> statusProperty = new SimpleObjectProperty<>(Status.WAITING);
    private final DoubleProperty progressProperty = new SimpleDoubleProperty(-1);


    public ImportJob(URL url, boolean checkForDupes, boolean checkForSimilar) {
        this(checkForDupes, checkForSimilar);
        this.url = url;
        needsDownload = true;
    }

    public ImportJob(File file, boolean checkForDupes, boolean checkForSimilar) {
        this(checkForDupes, checkForSimilar);
        this.file = file;
    }

    private ImportJob(boolean checkForDupes, boolean checkForSimilar) {
        this.needsCheckDuplicate = checkForDupes;
        this.needsCheckSimilar = checkForSimilar;
    }

    void runJob(Menagerie menagerie, Settings settings) {
        statusProperty.set(Status.IMPORTING);

        if (tryDownload(settings)) {
            statusProperty.set(Status.FAILED_IMPORT);
            return;
        }
        if (tryImport(menagerie)) {
            statusProperty.set(Status.FAILED_IMPORT);
            return;
        }
        tryHashHist();
        if (tryDuplicate(menagerie)) {
            statusProperty.set(Status.FAILED_DUPLICATE);
            return;
        }
        if (trySimilar(menagerie, settings)) {
            statusProperty.set(Status.SUCCEEDED_SIMILAR);
            return;
        }

        statusProperty.set(Status.SUCCEEDED);
    }

    private boolean trySimilar(Menagerie menagerie, Settings settings) {
        if (needsCheckSimilar && item.getHistogram() != null) {
            synchronized (this) {
                similarTo = new ArrayList<>();
            }
            for (ImageInfo i : menagerie.getItems()) {
                if (!item.equals(i) && i.getHistogram() != null) {
                    double similarity = i.getSimilarityTo(item, settings.getBoolean(Settings.Key.COMPARE_GREYSCALE));
                    if (similarity > settings.getDouble(Settings.Key.CONFIDENCE)) {
                        similarTo.add(new SimilarPair(item, i, similarity));
                    }
                }
            }

            needsCheckSimilar = false;
            return !similarTo.isEmpty();
        }
        return false;
    }

    private boolean tryDuplicate(Menagerie menagerie) {
        if (needsCheckDuplicate && item.getMD5() != null) {
            for (ImageInfo i : menagerie.getItems()) {
                if (!i.equals(item) && i.getMD5() != null && i.getMD5().equalsIgnoreCase(item.getMD5())) {
                    synchronized (this) {
                        duplicateOf = i;
                    }
                    menagerie.removeImages(Collections.singletonList(item), true);
                    needsCheckDuplicate = false;
                    needsCheckSimilar = false;
                    return true;
                }
            }

            needsCheckDuplicate = false;
        }
        return false;
    }

    private void tryHashHist() {
        if (needsHash) {
            item.initializeMD5();
            item.commitMD5ToDatabase();

            needsHash = false;
        }
        if (needsHist) {
            item.initializeHistogram();
            item.commitHistogramToDatabase();

            needsHist = false;
        }
    }

    private boolean tryImport(Menagerie menagerie) {
        if (needsImport) {
            synchronized (this) {
                item = menagerie.importFile(file);
            }

            if (item == null) {
                return true;
            } else {
                needsImport = false;
            }
        }
        return false;
    }

    private boolean tryDownload(Settings settings) {
        if (needsDownload) {
            try {
                String folder = settings.getString(Settings.Key.DEFAULT_FOLDER);
                if (!folder.endsWith("/") && !folder.endsWith("\\")) folder += "/";
                String filename = url.getPath().replaceAll("^.*/", "");
                File target = MainController.resolveDuplicateFilename(new File(folder + filename));
                //TODO: Deal with case where user input is required for saving the file

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.addRequestProperty("User-Agent", "Mozilla/4.0");
                ReadableByteChannel rbc = Channels.newChannel(conn.getInputStream());
                FileOutputStream fos = new FileOutputStream(target);

                final long size = conn.getContentLengthLong();
                final int chunkSize = 4096;
                for (int i = 0; i < size; i += chunkSize) {
                    fos.getChannel().transferFrom(rbc, i, chunkSize);

                    progressProperty.set((double) i / size);
                }

                conn.disconnect();
                rbc.close();
                fos.close();

                synchronized (this) {
                    file = target;
                }
                needsDownload = false;
            } catch (Exception e) {
                return true;
            }
        }
        progressProperty.set(-1);
        return false;
    }

    public synchronized ImageInfo getItem() {
        return item;
    }

    public synchronized URL getUrl() {
        return url;
    }

    public synchronized File getFile() {
        return file;
    }

    public synchronized ImageInfo getDuplicateOf() {
        return duplicateOf;
    }

    public synchronized List<SimilarPair> getSimilarTo() {
        return similarTo;
    }

    public DoubleProperty getProgressProperty() {
        return progressProperty;
    }

    public double getProgress() {
        return progressProperty.doubleValue();
    }

    public ObjectProperty<Status> getStatusProperty() {
        return statusProperty;
    }

    public Status getStatus() {
        return statusProperty.get();
    }

    synchronized void setImporter(ImporterThread importer) {
        this.importer = importer;
    }

    private synchronized ImporterThread getImporter() {
        return importer;
    }

    public void cancel() {
        if (getStatus() == Status.WAITING) getImporter().cancel(this);
    }

}
