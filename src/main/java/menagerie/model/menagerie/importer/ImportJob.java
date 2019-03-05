package menagerie.model.menagerie.importer;

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

    private URL url = null;
    private File file = null;
    private ImageInfo item = null;

    private volatile boolean needsDownload = false;
    private volatile boolean needsImport = true;
    private volatile boolean needsHash = true;
    private volatile boolean needsHist = true;
    private volatile boolean needsCheckDuplicate;
    private volatile boolean needsCheckSimilar;

    private volatile boolean finished = false;

    private List<ImportJobListener> listeners = new ArrayList<>();


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
        listeners.forEach(ImportJobListener::jobStarted);

        if (tryDownload(settings)) return;
        if (tryImport(menagerie)) return;
        tryHashHist();
        tryDuplicate(menagerie);
        trySimilar(menagerie, settings);

        listeners.forEach(listener -> listener.finishedJob(item));
        finished = true;
    }

    private void trySimilar(Menagerie menagerie, Settings settings) {
        if (needsCheckSimilar && item.getHistogram() != null) {
            List<SimilarPair> similar = new ArrayList<>();
            for (ImageInfo i : menagerie.getItems()) {
                if (i.getHistogram() != null) {
                    double similarity = i.getSimilarityTo(item, settings.getBoolean(Settings.Key.COMPARE_GREYSCALE));
                    if (similarity > settings.getDouble(Settings.Key.CONFIDENCE)) {
                        similar.add(new SimilarPair(item, i, similarity));
                    }
                }
            }

            needsCheckSimilar = false;
            listeners.forEach(listener -> listener.foundSimilar(similar));
        }
    }

    private void tryDuplicate(Menagerie menagerie) {
        if (needsCheckDuplicate && item.getMD5() != null) {
            for (ImageInfo i : menagerie.getItems()) {
                if (!i.equals(item) && i.getMD5() != null && i.getMD5().equalsIgnoreCase(item.getMD5())) {
                    menagerie.removeImages(Collections.singletonList(item), true);
                    listeners.forEach(listener -> listener.foundDuplicate(item, i));
                    break;
                }
            }

            needsCheckDuplicate = false;
            needsCheckSimilar = false;
        }
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
            //TODO: change import method to not create md5s or histograms
            setItem(menagerie.importFile(file));

            if (item == null) {
                listeners.forEach(ImportJobListener::importFailed);
                return true;
            } else {
                needsImport = false;
                listeners.forEach(listener -> listener.importSucceeded(item));
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

                    final int finalI = i + chunkSize; // Lambda workaround
                    listeners.forEach(listener -> listener.downloadProgress(finalI, size));
                }

                conn.disconnect();
                rbc.close();
                fos.close();

                file = target;
                needsDownload = false;
                listeners.forEach(listener -> listener.downloadSucceeded(file));
            } catch (Exception e) {
                listeners.forEach(listener -> listener.downloadFailed(e));
                return true;
            }
        }
        return false;
    }

    public void addListener(ImportJobListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ImportJobListener listener) {
        listeners.remove(listener);
    }

    public synchronized ImageInfo getItem() {
        return item;
    }

    public synchronized void setItem(ImageInfo item) {
        this.item = item;
    }

    public boolean isFinished() {
        return finished;
    }

}
