package menagerie.model.menagerie.importer;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import menagerie.gui.Main;
import menagerie.gui.MainController;
import menagerie.model.Settings;
import menagerie.model.SimilarPair;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;
import menagerie.model.menagerie.Menagerie;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.*;
import java.util.logging.Level;

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
    private MediaItem item = null;
    private MediaItem duplicateOf = null;
    private List<SimilarPair<MediaItem>> similarTo = null;

    private volatile boolean needsDownload = false;
    private volatile boolean needsImport = true;
    private volatile boolean needsHash = true;
    private volatile boolean needsHist = true;
    private volatile boolean needsCheckDuplicate = true;
    private volatile boolean needsCheckSimilar = true;

    private final DoubleProperty progressProperty = new SimpleDoubleProperty(-1);
    private volatile Status status = Status.WAITING;
    private final Set<ImportJobStatusListener> statusListeners = new HashSet<>();


    public ImportJob(URL url) {
        this.url = url;
        needsDownload = true;
    }

    public ImportJob(File file) {
        this.file = file;
    }

    void runJob(Menagerie menagerie, Settings settings) {
        setStatus(Status.IMPORTING);

        if (tryDownload(settings)) {
            setStatus(Status.FAILED_IMPORT);
            return;
        }
        if (tryImport(menagerie)) {
            setStatus(Status.FAILED_IMPORT);
            return;
        }
        tryHashHist();
        if (tryDuplicate(menagerie)) {
            setStatus(Status.FAILED_DUPLICATE);
            return;
        }
        if (trySimilar(menagerie, settings)) {
            setStatus(Status.SUCCEEDED_SIMILAR);
            return;
        }

        setStatus(Status.SUCCEEDED);
    }

    private boolean trySimilar(Menagerie menagerie, Settings settings) {
        if (needsCheckSimilar && item.getHistogram() != null) {
            synchronized (this) {
                similarTo = new ArrayList<>();
            }
            for (Item i : menagerie.getItems()) {
                if (i instanceof MediaItem && !item.equals(i) && ((MediaItem) i).getHistogram() != null) {
                    double similarity = ((MediaItem) i).getSimilarityTo(item, settings.getBoolean(Settings.Key.COMPARE_GREYSCALE));
                    if (similarity > settings.getDouble(Settings.Key.CONFIDENCE)) {
                        synchronized (this) {
                            similarTo.add(new SimilarPair<>(item, (MediaItem) i, similarity));
                        }
                    }
                }
            }

            needsCheckSimilar = false;
            synchronized (this) {
                return !similarTo.isEmpty();
            }
        }
        return false;
    }

    private boolean tryDuplicate(Menagerie menagerie) {
        if (needsCheckDuplicate && item.getMD5() != null) {
            for (Item i : menagerie.getItems()) {
                if (i instanceof MediaItem && !i.equals(item) && ((MediaItem) i).getMD5() != null && ((MediaItem) i).getMD5().equalsIgnoreCase(item.getMD5())) {
                    synchronized (this) {
                        duplicateOf = (MediaItem) i;
                    }
                    menagerie.removeItems(Collections.singletonList(item), true);
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
            needsHash = false;
        }
        if (needsHist) {
            item.initializeHistogram();
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
                try (FileOutputStream fos = new FileOutputStream(target)) {
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
                    file = target;
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

    public synchronized MediaItem getItem() {
        return item;
    }

    public synchronized URL getUrl() {
        return url;
    }

    public synchronized File getFile() {
        return file;
    }

    public synchronized MediaItem getDuplicateOf() {
        return duplicateOf;
    }

    public synchronized List<SimilarPair<MediaItem>> getSimilarTo() {
        return similarTo;
    }

    public DoubleProperty getProgressProperty() {
        return progressProperty;
    }

    public double getProgress() {
        return progressProperty.doubleValue();
    }

    public Status getStatus() {
        synchronized (statusListeners) {
            return status;
        }
    }

    public void setStatus(Status status) {
        synchronized (statusListeners) {
            this.status = status;

            statusListeners.forEach(listener -> listener.changed(status));
        }
    }

    public void addStatusListener(ImportJobStatusListener listener) {
        synchronized (statusListeners) {
            statusListeners.add(listener);
        }
    }

    public void removeStatusListener(ImportJobStatusListener listener) {
        synchronized (statusListeners) {
            statusListeners.remove(listener);
        }
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
