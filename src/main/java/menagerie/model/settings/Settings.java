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
    private static final String COMPUTE_MD5_FOR_SIMILARITY_TAG = "computemd5forsimilarity";
    private static final String COMPUTE_HIST_FOR_SIMILARITY_TAG = "computehistforsimilarity";
    private static final String BACKUP_DATABASE_TAG = "backupdatabase";
    private static final String LAST_FOLDER_TAG = "lastfolder";
    private static final String IMAGE_GRID_WIDTH_TAG = "imagegridwidth";
    private static final String WINDOW_WIDTH_TAG = "windowwidth";
    private static final String WINDOW_HEIGHT_TAG = "windowheight";
    private static final String WINDOW_MAXIMZED_TAG = "windowmaximized";
    private static final String WINDOW_X_TAG = "windowx";
    private static final String WINDOW_Y_TAG = "windowy";
    private static final String DB_USER_TAG = "dbuser";
    private static final String DB_PASS_TAG = "dbpass";
    private static final String DB_URL_TAG = "dburl";
    private static final String SIMILARITY_THRESHOLD_TAG = "similaritythreshold";
    private static final String CONSOLIDATE_TAGS_TAG = "consolidatetags";
    private static final String AUTO_IMPORT_FROM_FOLDER_TAG = "importfromfolder";
    private static final String IMPORT_FROM_FOLDER_PATH_TAG = "importfromfolderpath";
    private static final String AUTO_IMPORT_FROM_FOLDER_TO_DEFAULT = "autoimportfromfoldertodefault";
    private static final String COMPARE_BLACK_AND_WHITE_HISTS_TAG = "compareblackandwhitehists";
    private static final String COMPARE_BLACK_AND_WHITE_CONFIDENCE_TAG = "compareblackandwhiteconfidence";

    private boolean autoImportFromWeb = false;
    private boolean computeMD5OnImport = true;
    private boolean computeHistogramOnImport = true;
    private boolean buildThumbnailOnImport = false;
    private boolean windowMaximized = false;
    private boolean computeMD5ForSimilarity = true;
    private boolean computeHistogramForSimilarity = false;
    private boolean consolidateTags = true;
    private boolean backupDatabase = true;
    private boolean autoImportFromFolder = false;
    private boolean autoImportFromFolderToDefault = true;
    private boolean compareBlackAndWhiteHists = false;
    private String lastFolder = null;
    private String importFromFolderPath = null;
    private String dbUser = "sa";
    private String dbPass = "";
    private String dbUrl = "~/menagerie";
    private int imageGridWidth = 2;
    private int windowWidth = -1;
    private int windowHeight = -1;
    private int windowX = -1;
    private int windowY = -1;
    private double similarityThreshold = 0.95;
    private double compareBlackAndWhiteConfidence = 0.25;

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

    public String getDbUser() {
        return dbUser;
    }

    public String getDbPass() {
        return dbPass;
    }

    public String getDbUrl() {
        return dbUrl;
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

    public int getWindowX() {
        return windowX;
    }

    public int getWindowY() {
        return windowY;
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public String getImportFromFolderPath() {
        return importFromFolderPath;
    }

    public double getCompareBlackAndWhiteConfidence() {
        return compareBlackAndWhiteConfidence;
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

    public boolean isComputeMD5ForSimilarity() {
        return computeMD5ForSimilarity;
    }

    public boolean isComputeHistogramForSimilarity() {
        return computeHistogramForSimilarity;
    }

    public boolean isConsolidateTags() {
        return consolidateTags;
    }

    public boolean isBackupDatabase() {
        return backupDatabase;
    }

    public boolean isAutoImportFromFolder() {
        return autoImportFromFolder;
    }

    public boolean isAutoImportFromFolderToDefault() {
        return autoImportFromFolderToDefault;
    }

    public boolean isCompareBlackAndWhiteHists() {
        return compareBlackAndWhiteHists;
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

    public void setWindowX(int windowX) {
        this.windowX = windowX;
        try {
            saveToFile();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void setWindowY(int windowY) {
        this.windowY = windowY;
        try {
            saveToFile();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void setDbUser(String dbUser) {
        this.dbUser = dbUser;
        try {
            saveToFile();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void setDbPass(String dbPass) {
        this.dbPass = dbPass;
        try {
            saveToFile();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void setDbUrl(String dbUrl) {
        this.dbUrl = dbUrl;
        try {
            saveToFile();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void setComputeMD5ForSimilarity(boolean computeMD5ForSimilarity) {
        this.computeMD5ForSimilarity = computeMD5ForSimilarity;
        try {
            saveToFile();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void setComputeHistogramForSimilarity(boolean computeHistogramForSimilarity) {
        this.computeHistogramForSimilarity = computeHistogramForSimilarity;
        try {
            saveToFile();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void setSimilarityThreshold(double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
        try {
            saveToFile();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void setConsolidateTags(boolean consolidateTags) {
        this.consolidateTags = consolidateTags;
        try {
            saveToFile();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void setBackupDatabase(boolean backupDatabase) {
        this.backupDatabase = backupDatabase;
        try {
            saveToFile();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void setAutoImportFromFolder(boolean autoImportFromFolder) {
        this.autoImportFromFolder = autoImportFromFolder;
        try {
            saveToFile();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void setImportFromFolderPath(String importFromFolderPath) {
        this.importFromFolderPath = importFromFolderPath;
        try {
            saveToFile();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void setAutoImportFromFolderToDefault(boolean autoImportFromFolderToDefault) {
        this.autoImportFromFolderToDefault = autoImportFromFolderToDefault;
        try {
            saveToFile();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void setCompareBlackAndWhiteHists(boolean compareBlackAndWhiteHists) {
        this.compareBlackAndWhiteHists = compareBlackAndWhiteHists;
        try {
            saveToFile();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void setCompareBlackAndWhiteConfidence(double compareBlackAndWhiteConfidence) {
        this.compareBlackAndWhiteConfidence = compareBlackAndWhiteConfidence;
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
        writer.println(COMPUTE_MD5_FOR_SIMILARITY_TAG + "=" + computeMD5ForSimilarity);
        writer.println(COMPUTE_HIST_FOR_SIMILARITY_TAG + "=" + computeHistogramForSimilarity);
        writer.println(BACKUP_DATABASE_TAG + "=" + backupDatabase);
        writer.println(CONSOLIDATE_TAGS_TAG + "=" + consolidateTags);
        writer.println(AUTO_IMPORT_FROM_FOLDER_TAG + "=" + autoImportFromFolder);
        writer.println(AUTO_IMPORT_FROM_FOLDER_TO_DEFAULT + "=" + autoImportFromFolderToDefault);
        writer.println(COMPARE_BLACK_AND_WHITE_HISTS_TAG + "=" + compareBlackAndWhiteHists);
        writer.println(IMAGE_GRID_WIDTH_TAG + "=" + imageGridWidth);
        writer.println(WINDOW_MAXIMZED_TAG + "=" + windowMaximized);
        writer.println(WINDOW_WIDTH_TAG + "=" + windowWidth);
        writer.println(WINDOW_HEIGHT_TAG + "=" + windowHeight);
        writer.println(WINDOW_X_TAG + "=" + windowX);
        writer.println(WINDOW_Y_TAG + "=" + windowY);
        writer.println(DB_URL_TAG + "=" + dbUrl);
        writer.println(DB_USER_TAG + "=" + dbUser);
        writer.println(DB_PASS_TAG + "=" + dbPass);
        writer.println(SIMILARITY_THRESHOLD_TAG + "=" + similarityThreshold);
        writer.println(COMPARE_BLACK_AND_WHITE_CONFIDENCE_TAG + "=" + compareBlackAndWhiteConfidence);
        if (lastFolder != null) writer.println(LAST_FOLDER_TAG + "=" + lastFolder);
        if (importFromFolderPath != null) writer.println(IMPORT_FROM_FOLDER_PATH_TAG + "=" + importFromFolderPath);

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
                        if (!val.isEmpty()) lastFolder = val;
                        break;
                    case BUILD_THUMBNAIL_TAG:
                        buildThumbnailOnImport = Boolean.parseBoolean(val);
                        break;
                    case COMPUTE_MD5_FOR_SIMILARITY_TAG:
                        computeMD5ForSimilarity = Boolean.parseBoolean(val);
                        break;
                    case COMPUTE_HIST_FOR_SIMILARITY_TAG:
                        computeHistogramForSimilarity = Boolean.parseBoolean(val);
                        break;
                    case CONSOLIDATE_TAGS_TAG:
                        consolidateTags = Boolean.parseBoolean(val);
                        break;
                    case BACKUP_DATABASE_TAG:
                        backupDatabase = Boolean.parseBoolean(val);
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
                        break;
                    case WINDOW_X_TAG:
                        windowX = Integer.parseInt(val);
                        break;
                    case WINDOW_Y_TAG:
                        windowY = Integer.parseInt(val);
                        break;
                    case DB_URL_TAG:
                        dbUrl = val;
                        break;
                    case DB_USER_TAG:
                        dbUser = val;
                        break;
                    case DB_PASS_TAG:
                        dbPass = val;
                        break;
                    case SIMILARITY_THRESHOLD_TAG:
                        similarityThreshold = Double.parseDouble(val);
                        break;
                    case IMPORT_FROM_FOLDER_PATH_TAG:
                        importFromFolderPath = val;
                        break;
                    case AUTO_IMPORT_FROM_FOLDER_TAG:
                        autoImportFromFolder = Boolean.parseBoolean(val);
                        break;
                    case AUTO_IMPORT_FROM_FOLDER_TO_DEFAULT:
                        autoImportFromFolderToDefault = Boolean.parseBoolean(val);
                        break;
                    case COMPARE_BLACK_AND_WHITE_HISTS_TAG:
                        compareBlackAndWhiteHists = Boolean.parseBoolean(val);
                        break;
                    case COMPARE_BLACK_AND_WHITE_CONFIDENCE_TAG:
                        compareBlackAndWhiteConfidence = Double.parseDouble(val);
                        break;
                }
            }
        }

        scan.close();
    }

}
