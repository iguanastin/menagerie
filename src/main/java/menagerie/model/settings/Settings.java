package menagerie.model.settings;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Scanner;

public class Settings {

    private static final String AUTO_IMPORT_TAG = "autoimportfromweb";
    private static final String COMPUTE_MD5_TAG = "computemd5onimport";
    private static final String COMPUTE_HIST_TAG = "computehistogramonimport";
    private static final String BUILD_THUMBNAIL_TAG = "buildthumbnailonimport";
    private static final String LAST_FOLDER_TAG = "lastfolder";

    private boolean autoImportFromWeb = false;
    private boolean computeMD5OnImport = true;
    private boolean computeHistogramOnImport = true;
    private boolean buildThumbnailOnImport = false;
    private String lastFolder = null;

    private final File file;


    public Settings(File file) {
        this.file = file;

        try {
            loadFromFile();
        } catch (FileNotFoundException e) {
            try {
                saveToFile();
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
            }
        }
    }

    public String getLastFolder() {
        return lastFolder;
    }

    public boolean isAutoImportFromWeb() {
        return autoImportFromWeb;
    }

    public boolean isComputeHistogramOnImport() {
        return computeHistogramOnImport;
    }

    public boolean isComputeMD5OnImport() {
        return computeMD5OnImport;
    }

    public boolean isBuildThumbnailOnImport() {
        return buildThumbnailOnImport;
    }

    public void setAutoImportFromWeb(boolean autoImportFromWeb) {
        this.autoImportFromWeb = autoImportFromWeb;
        try {
            saveToFile();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void setComputeHistogramOnImport(boolean computeHistogramOnImport) {
        this.computeHistogramOnImport = computeHistogramOnImport;
        try {
            saveToFile();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void setComputeMD5OnImport(boolean computeMD5OnImport) {
        this.computeMD5OnImport = computeMD5OnImport;
        try {
            saveToFile();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void setBuildThumbnailOnImport(boolean buildThumbnailOnImport) {
        this.buildThumbnailOnImport = buildThumbnailOnImport;
        try {
            saveToFile();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void setLastFolder(String lastFolder) {
        this.lastFolder = lastFolder;
        try {
            saveToFile();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void saveToFile() throws FileNotFoundException {
        PrintWriter writer = new PrintWriter(file);

        writer.println("# Settings file for Menagerie");
        writer.println(AUTO_IMPORT_TAG + "=" + autoImportFromWeb);
        writer.println(COMPUTE_MD5_TAG + "=" + computeMD5OnImport);
        writer.println(COMPUTE_HIST_TAG + "=" + computeHistogramOnImport);
        writer.println(BUILD_THUMBNAIL_TAG + "=" + buildThumbnailOnImport);
        if (lastFolder != null) writer.println(LAST_FOLDER_TAG + "=" + lastFolder);

        writer.close();
    }

    private void loadFromFile() throws FileNotFoundException {
        Scanner scan = new Scanner(file);

        while (scan.hasNextLine()) {
            String line = scan.nextLine();
            if (line != null && !line.isEmpty() && !line.startsWith("#")) {
                String tag = line.substring(0, line.indexOf('='));
                String val = line.substring(line.indexOf('=') + 1);
                switch (tag) {
                    case AUTO_IMPORT_TAG:
                        autoImportFromWeb = Boolean.parseBoolean(val);
                        break;
                    case COMPUTE_MD5_TAG:
                        computeMD5OnImport = Boolean.parseBoolean(val);
                        break;
                    case COMPUTE_HIST_TAG:
                        computeHistogramOnImport = Boolean.parseBoolean(val);
                        break;
                    case LAST_FOLDER_TAG:
                        lastFolder = val;
                        break;
                    case BUILD_THUMBNAIL_TAG:
                        buildThumbnailOnImport = Boolean.parseBoolean(val);
                        break;
                }
            }
        }

        scan.close();
    }

}
