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
    private static final String IMAGE_GRID_WIDTH_TAG = "imagegridwidth";
    private static final String WINDOW_WIDTH_TAG = "windowwidth";
    private static final String WINDOW_HEIGHT_TAG = "windowheight";
    private static final String WINDOW_MAXIMZED_TAG = "windowmaximized";

    private boolean autoImportFromWeb = false;
    private boolean computeMD5OnImport = true;
    private boolean computeHistogramOnImport = true;
    private boolean buildThumbnailOnImport = false;
    private boolean windowMaximized = false;
    private String lastFolder = null;
    private int imageGridWidth = 2;
    private int windowWidth = 800;
    private int windowHeight = 600;

    public static final int MIN_IMAGE_GRID_WIDTH = 2;
    public static final int MAX_IMAGE_GRID_WIDTH = 8;

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

    public int getImageGridWidth() {
        return imageGridWidth;
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public int getWindowHeight() {
        return windowHeight;
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

    public boolean isWindowMaximized() {
        return windowMaximized;
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

    public void setImageGridWidth(int imageGridWidth) {
        imageGridWidth = Math.max(MIN_IMAGE_GRID_WIDTH, Math.min(imageGridWidth, MAX_IMAGE_GRID_WIDTH));
        this.imageGridWidth = imageGridWidth;
        try {
            saveToFile();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void setWindowHeight(int windowHeight) {
        this.windowHeight = windowHeight;
        try {
            saveToFile();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void setWindowMaximized(boolean windowMaximized) {
        this.windowMaximized = windowMaximized;
        try {
            saveToFile();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void setWindowWidth(int windowWidth) {
        this.windowWidth = windowWidth;
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
        writer.println(IMAGE_GRID_WIDTH_TAG + "=" + imageGridWidth);
        writer.println(WINDOW_WIDTH_TAG + "=" + windowWidth);
        writer.println(WINDOW_HEIGHT_TAG + "=" + windowHeight);
        writer.println(WINDOW_MAXIMZED_TAG + "=" + windowMaximized);
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
                    case IMAGE_GRID_WIDTH_TAG:
                        imageGridWidth = Math.max(MIN_IMAGE_GRID_WIDTH, Math.min(Integer.parseInt(val), MAX_IMAGE_GRID_WIDTH));
                        break;
                    case WINDOW_WIDTH_TAG:
                        windowWidth = Integer.parseInt(val);
                        break;
                    case WINDOW_HEIGHT_TAG:
                        windowHeight = Integer.parseInt(val);
                        break;
                    case WINDOW_MAXIMZED_TAG:
                        windowMaximized = Boolean.parseBoolean(val);
                }
            }
        }

        scan.close();
    }

}
