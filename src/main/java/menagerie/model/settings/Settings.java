package menagerie.model.settings;

import java.io.*;
import java.util.Properties;

public class Settings extends Properties {

    private static final String AUTO_IMPORT_FROM_WEB_TAG = "auto_import_from_web";
    private static final String COMPUTE_MD5_ON_IMPORT_TAG = "compute_md5_on_import";
    private static final String COMPUTE_HIST_ON_IMPORT_TAG = "compute_histogram_on_import";
    private static final String BUILD_THUMBNAIL_ON_IMPORT_TAG = "build_thumbnail_on_import";
    private static final String COMPUTE_MD5_FOR_SIMILARITY_TAG = "compute_md5_for_similarity";
    private static final String COMPUTE_HIST_FOR_SIMILARITY_TAG = "compute_hist_for_similarity";
    private static final String BACKUP_DATABASE_TAG = "backup_database";
    private static final String DEFAULT_FOLDER_TAG = "default_folder";
    private static final String IMAGE_GRID_WIDTH_TAG = "image_grid_width";
    private static final String WINDOW_WIDTH_TAG = "window_width";
    private static final String WINDOW_HEIGHT_TAG = "window_height";
    private static final String WINDOW_MAXIMZED_TAG = "window_maximized";
    private static final String WINDOW_X_TAG = "window_x";
    private static final String WINDOW_Y_TAG = "window_y";
    private static final String DB_USER_TAG = "db_user";
    private static final String DB_PASS_TAG = "db_pass";
    private static final String DB_URL_TAG = "db_url";
    private static final String SIMILARITY_THRESHOLD_TAG = "similarity_threshold";
    private static final String CONSOLIDATE_TAGS_TAG = "consolidate_tags";
    private static final String AUTO_IMPORT_FROM_FOLDER_TAG = "import_from_folder";
    private static final String IMPORT_FROM_FOLDER_PATH_TAG = "import_from_folder_path";
    private static final String AUTO_IMPORT_FROM_FOLDER_TO_DEFAULT = "auto_import_from_folder_to_default";
    private static final String COMPARE_BLACK_AND_WHITE_HISTS_TAG = "compare_black_and_white_hists";
    private static final String MUTE_VIDEO_PREVIEW_TAG = "mute_video_preview";
    private static final String REPEAT_VIDEO_PREVIEW_TAG = "repeat_video_preview";

    public static final int MIN_IMAGE_GRID_WIDTH = 2;
    public static final int MAX_IMAGE_GRID_WIDTH = 8;

    private final File file;


    public Settings(File file) {
        this.file = file;
    }

    public void loadFromFile() throws IOException {
        System.out.println("Attempting to load settings from file: " + file);
        loadFromXML(new FileInputStream(file));
    }

    public void saveToFile() throws IOException {
        System.out.println("Attempting to save settings to file: " + file);
        storeToXML(new FileOutputStream(file), "Menagerie properties file");
    }

    public String getDefaultFolder() {
        return getProperty(DEFAULT_FOLDER_TAG, null);
    }

    public String getDbUser() {
        return getProperty(DB_USER_TAG, "sa");
    }

    public String getDbPass() {
        return getProperty(DB_PASS_TAG, "");
    }

    public String getDbUrl() {
        return getProperty(DB_URL_TAG, "~/menagerie");
    }

    public int getImageGridWidth() {
        return Integer.parseInt(getProperty(IMAGE_GRID_WIDTH_TAG, "2"));
    }

    public int getWindowWidth() {
        return Integer.parseInt(getProperty(WINDOW_WIDTH_TAG, "-1"));
    }

    public int getWindowHeight() {
        return Integer.parseInt(getProperty(WINDOW_HEIGHT_TAG, "-1"));
    }

    public int getWindowX() {
        return Integer.parseInt(getProperty(WINDOW_X_TAG, "-1"));
    }

    public int getWindowY() {
        return Integer.parseInt(getProperty(WINDOW_Y_TAG, "-1"));
    }

    public double getSimilarityThreshold() {
        return Double.parseDouble(getProperty(SIMILARITY_THRESHOLD_TAG, "0.95"));
    }

    public String getImportFromFolderPath() {
        return getProperty(IMPORT_FROM_FOLDER_PATH_TAG, null);
    }

    public boolean isAutoImportFromWeb() {
        return Boolean.parseBoolean(getProperty(AUTO_IMPORT_FROM_WEB_TAG, "false"));
    }

    public boolean isComputeHistogramOnImport() {
        return Boolean.parseBoolean(getProperty(COMPUTE_HIST_ON_IMPORT_TAG, "true"));
    }

    public boolean isComputeMD5OnImport() {
        return Boolean.parseBoolean(getProperty(COMPUTE_MD5_ON_IMPORT_TAG, "true"));
    }

    public boolean isBuildThumbnailOnImport() {
        return Boolean.parseBoolean(getProperty(BUILD_THUMBNAIL_ON_IMPORT_TAG, "false"));
    }

    public boolean isWindowMaximized() {
        return Boolean.parseBoolean(getProperty(WINDOW_MAXIMZED_TAG, "false"));
    }

    public boolean isComputeMD5ForSimilarity() {
        return Boolean.parseBoolean(getProperty(COMPUTE_MD5_FOR_SIMILARITY_TAG, "true"));
    }

    public boolean isComputeHistogramForSimilarity() {
        return Boolean.parseBoolean(getProperty(COMPUTE_HIST_FOR_SIMILARITY_TAG, "false"));
    }

    public boolean isConsolidateTags() {
        return Boolean.parseBoolean(getProperty(CONSOLIDATE_TAGS_TAG, "true"));
    }

    public boolean isBackupDatabase() {
        return Boolean.parseBoolean(getProperty(BACKUP_DATABASE_TAG, "true"));
    }

    public boolean isAutoImportFromFolder() {
        return Boolean.parseBoolean(getProperty(AUTO_IMPORT_FROM_FOLDER_TAG, "false"));
    }

    public boolean isAutoImportFromFolderToDefault() {
        return Boolean.parseBoolean(getProperty(AUTO_IMPORT_FROM_FOLDER_TO_DEFAULT, "true"));
    }

    public boolean isCompareBlackAndWhiteHists() {
        return Boolean.parseBoolean(getProperty(COMPARE_BLACK_AND_WHITE_HISTS_TAG, "false"));
    }

    public boolean isMuteVideoPreview() {
        return Boolean.parseBoolean(getProperty(MUTE_VIDEO_PREVIEW_TAG, "false"));
    }

    public boolean isRepeatVideoPreview() {
        return Boolean.parseBoolean(getProperty(REPEAT_VIDEO_PREVIEW_TAG, "true"));
    }

    public void setImageGridWidth(int imageGridWidth) {
        setProperty(IMAGE_GRID_WIDTH_TAG, imageGridWidth + "");
    }

    public void setWindowHeight(int windowHeight) {
        setProperty(WINDOW_HEIGHT_TAG, windowHeight + "");
    }

    public void setWindowWidth(int windowWidth) {
        setProperty(WINDOW_WIDTH_TAG, windowWidth + "");
    }

    public void setWindowX(int windowX) {
        setProperty(WINDOW_X_TAG, windowX + "");
    }

    public void setWindowY(int windowY) {
        setProperty(WINDOW_Y_TAG, windowY + "");
    }

    public void setDefaultFolder(String defaultFolder) {
        if (defaultFolder != null) {
            setProperty(DEFAULT_FOLDER_TAG, defaultFolder);
        } else {
            remove(DEFAULT_FOLDER_TAG);
        }
    }

    public void setDbUser(String dbUser) {
        if (dbUser != null) {
            setProperty(DB_USER_TAG, dbUser);
        } else {
            remove(DB_USER_TAG);
        }
    }

    public void setDbPass(String dbPass) {
        if (dbPass != null) {
            setProperty(DB_PASS_TAG, dbPass);
        } else {
            remove(DB_PASS_TAG);
        }
    }

    public void setDbUrl(String dbUrl) {
        if (dbUrl != null) {
            setProperty(DB_URL_TAG, dbUrl);
        } else {
            remove(DB_URL_TAG);
        }
    }

    public void setImportFromFolderPath(String importFromFolderPath) {
        if (importFromFolderPath != null) {
            setProperty(IMPORT_FROM_FOLDER_PATH_TAG, importFromFolderPath);
        } else {
            remove(IMPORT_FROM_FOLDER_PATH_TAG);
        }
    }

    public void setComputeMD5ForSimilarity(boolean computeMD5ForSimilarity) {
        setProperty(COMPUTE_MD5_FOR_SIMILARITY_TAG, computeMD5ForSimilarity + "");
    }

    public void setComputeHistogramForSimilarity(boolean computeHistogramForSimilarity) {
        setProperty(COMPUTE_HIST_FOR_SIMILARITY_TAG, computeHistogramForSimilarity + "");
    }

    public void setConsolidateTags(boolean consolidateTags) {
        setProperty(CONSOLIDATE_TAGS_TAG, consolidateTags + "");
    }

    public void setBackupDatabase(boolean backupDatabase) {
        setProperty(BACKUP_DATABASE_TAG, backupDatabase + "");
    }

    public void setAutoImportFromFolder(boolean autoImportFromFolder) {
        setProperty(AUTO_IMPORT_FROM_FOLDER_TAG, autoImportFromFolder + "");
    }

    public void setAutoImportFromFolderToDefault(boolean autoImportFromFolderToDefault) {
        setProperty(AUTO_IMPORT_FROM_FOLDER_TO_DEFAULT, autoImportFromFolderToDefault + "");
    }

    public void setCompareBlackAndWhiteHists(boolean compareBlackAndWhiteHists) {
        setProperty(COMPARE_BLACK_AND_WHITE_HISTS_TAG, compareBlackAndWhiteHists + "");
    }

    public void setWindowMaximized(boolean windowMaximized) {
        setProperty(WINDOW_MAXIMZED_TAG, windowMaximized + "");
    }

    public void setAutoImportFromWeb(boolean autoImportFromWeb) {
        setProperty(AUTO_IMPORT_FROM_WEB_TAG, autoImportFromWeb + "");
    }

    public void setComputeHistogramOnImport(boolean computeHistogramOnImport) {
        setProperty(COMPUTE_HIST_ON_IMPORT_TAG, computeHistogramOnImport + "");
    }

    public void setComputeMD5OnImport(boolean computeMD5OnImport) {
        setProperty(COMPUTE_MD5_ON_IMPORT_TAG, computeMD5OnImport + "");
    }

    public void setBuildThumbnailOnImport(boolean buildThumbnailOnImport) {
        setProperty(BUILD_THUMBNAIL_ON_IMPORT_TAG, buildThumbnailOnImport + "");
    }

    public void setRepeatVideoPreview(boolean repeatVideoPreview) {
        setProperty(REPEAT_VIDEO_PREVIEW_TAG, repeatVideoPreview + "");
    }

    public void setMuteVideoPreview(boolean muteVideoPreview) {
        setProperty(MUTE_VIDEO_PREVIEW_TAG, muteVideoPreview + "");
    }

    public void setSimilarityThreshold(double similarityThreshold) {
        setProperty(SIMILARITY_THRESHOLD_TAG, similarityThreshold + "");
    }

}
