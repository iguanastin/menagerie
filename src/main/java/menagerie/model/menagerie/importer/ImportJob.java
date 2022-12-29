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
import java.util.logging.Logger;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import menagerie.model.SimilarPair;
import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;
import menagerie.model.menagerie.Menagerie;
import menagerie.model.menagerie.Tag;
import menagerie.settings.MenagerieSettings;
import menagerie.util.FileUtil;

/**
 * A runnable job that will import a file.
 */
public class ImportJob {

  private static final Logger LOGGER = Logger.getLogger(ImportJob.class.getName());

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
   * Synchronously runs this job. Downloads, imports, creates hash, creates histogram, finds duplicates, and finds similar items.
   *
   * @param menagerie Menagerie to import into.
   * @param settings  Application settings to import with.
   */
  void runJob(Menagerie menagerie, MenagerieSettings settings) {
    setStatus(Status.IMPORTING);

    // REENG: unexpected return values of these try... methods!
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
            LOGGER.warning(() ->
                String.format("Default folder '%s' doesn't exist or isn't a folder", folder));
            return true;
          }
          String filename = url.getPath().replaceAll("^.*/", "");

          downloadTo = FileUtil.resolveDuplicateFilename(new File(folder, filename));
        }
        if (downloadTo.exists()) {
          LOGGER.warning(() ->
              String.format("Attempted to download '%s' into pre-existing file '%s'",
                  url.toString(), downloadTo.toString()));
          return true;
        }

        doDownload();
      } catch (RuntimeException e) {
        LOGGER.log(Level.WARNING, e, () ->
            "Unexpected exception while downloading from url: %s" + url.toString());
        return true;
      } catch (IOException e) {
        LOGGER.log(Level.WARNING, e, () ->
            "IOException while downloading file from url: %s" + url.toString());
        return true;
      }
    }
    progressProperty.set(-1);
    return false;
  }

  private void doDownload() throws IOException {
    LOGGER.info(() -> "Downloading: " + getUrl() + "\nTo local: " + downloadTo);
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
    LOGGER.info(() -> "Successfully downloaded: " + getUrl() + "\nTo local: " + downloadTo);

    synchronized (this) {
      file = downloadTo;
    }
    needsDownload = false;
  }

  /**
   * Tries to import the file into the Menagerie and store it in {@link #item}
   *
   * @param menagerie Menagerie to import into.
   * @return True if the import failed.
   */
  private boolean tryImport(Menagerie menagerie, MenagerieSettings settings) {
    if (!needsImport) {
      return false;
    }

    LOGGER.info(() -> "Importing file: " + file);
    synchronized (this) {
      item = menagerie.importFile(file);
    }

    if (item == null) {
      LOGGER.info(() -> "File failed to import: " + file);
      return true;
    }

    LOGGER.info(() -> "Successfully imported file: " + file + "\nWith ID: " + item.getId());
    needsImport = false;

    LOGGER.info("Applying auto-tags to imported item: " + item.getId());
    // Add tags
    if (settings.tagTagme.getValue()) {
      addTag(menagerie, "tagme");
    }
    if (settings.tagImages.getValue() && item.isImage()) {
      addTag(menagerie, "image");
    }
    if (settings.tagVideos.getValue() && item.isVideo()) {
      addTag(menagerie, "video");
    }

    return false;
  }

  private void addTag(Menagerie menagerie, String name) {
    Tag tag = menagerie.getTagByName(name);
    if (tag == null) {
      tag = menagerie.createTag(name);
    }
    item.addTag(tag);
  }

  /**
   * Tries to construct and store an MD5 hash and a histogram for the item.
   */
  private void tryHashHist() {
    if (needsHash) {
      LOGGER.info(() -> "Hashing file (ID: " + item.getId() + "): " + file);
      item.initializeMD5();
      needsHash = false;
    }
    if (needsHist) {
      if (item.initializeHistogram()) {
        LOGGER.info(
            () -> "Generated image histogram from file (ID: " + item.getId() + "): " + file);
      }
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
    if (!needsCheckDuplicate || item.getMD5() == null) {
      return false;
    }

    LOGGER.info("Checking for hash duplicates: " + item.getId());
    for (Item i : menagerie.getItems()) {
      if (i instanceof MediaItem mediaItem) {
        if (!i.equals(item) && mediaItem.getMD5() != null &&
            mediaItem.getMD5().equalsIgnoreCase(item.getMD5())) {
          synchronized (this) {
            duplicateOf = (MediaItem) i;
          }
          LOGGER.info("Found hash duplicate, cancelling import: " + item.getId());
          menagerie.deleteItem(item);
          needsCheckDuplicate = false;
          needsCheckSimilar = false;
          return true;
        }
      }
    }

    needsCheckDuplicate = false;
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
    if (!needsCheckSimilar || item.getHistogram() == null) {
      return;
    }

    LOGGER.info("Finding similar, existing items: " + item.getId());
    synchronized (this) {
      similarTo = new ArrayList<>();
    }
    final double confidence = settings.duplicatesConfidence.getValue();
    final double confidenceSquare = 1 - (1 - confidence) * (1 - confidence);
    boolean anyMinimallySimilar = false;
    for (Item i : menagerie.getItems()) {
      anyMinimallySimilar = trySimilarItem(confidence, confidenceSquare, i);
    }

    if (!anyMinimallySimilar) {
      LOGGER.info("None minimally similar to item: " + item.getId());
      item.setHasNoSimilar(true);
    }

    needsCheckSimilar = false;
  }

  private boolean trySimilarItem(double confidence, double confidenceSquare, Item i) {

    boolean anyMinimallySimilar = false;
    if (i instanceof MediaItem && !item.equals(i) && ((MediaItem) i).getHistogram() != null) {
      double similarity = ((MediaItem) i).getSimilarityTo(item);

      if (similarity > MediaItem.MIN_CONFIDENCE) {
        anyMinimallySimilar = true;
        if (((MediaItem) i).hasNoSimilar()) {
          ((MediaItem) i).setHasNoSimilar(false);
        }
      }

      if (similarity >= confidenceSquare ||
          (similarity >= confidence && item.getHistogram().isColorful() &&
           ((MediaItem) i).getHistogram().isColorful())) {
        synchronized (this) {
          LOGGER.info("Found similar item (To ID: " + item.getId() + "): " + i.getId());
          final var similarPair = new SimilarPair<>(item, (MediaItem) i, similarity);
          similarTo.add(similarPair);
        }
      }
    }
    return anyMinimallySimilar;
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
    if (getStatus() == Status.WAITING) {
      if (url != null) {
        LOGGER.info(() -> "Cancelling web import: " + url);
      } else {
        LOGGER.info(() -> "Cancelling local import: " + file);
      }

      getImporter().cancel(this);
    }
  }

}
