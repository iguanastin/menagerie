package menagerie.gui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import menagerie.gui.grid.ImageGridView;
import menagerie.gui.image.DynamicImageView;
import menagerie.gui.progress.ProgressLockThread;
import menagerie.gui.progress.ProgressLockThreadCancelListener;
import menagerie.gui.progress.ProgressLockThreadFinishListener;
import menagerie.model.SimilarPair;
import menagerie.model.db.DatabaseVersionUpdater;
import menagerie.model.menagerie.ImageInfo;
import menagerie.model.menagerie.Menagerie;
import menagerie.model.menagerie.Tag;
import menagerie.model.search.*;
import menagerie.model.settings.Settings;
import menagerie.util.Filters;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class MainController {

    public StackPane rootPane;

    public BorderPane explorerPane;
    public ToggleButton descendingToggleButton;
    public PredictiveTextField searchTextField;
    public ImageGridView imageGridView;
    public DynamicImageView previewImageView;
    public Label resultCountLabel;
    public Label imageInfoLabel;
    public ListView<Tag> tagListView;
    public PredictiveTextField editTagsTextfield;
    public MenuBar menuBar;

    public BorderPane settingsPane;
    public CheckBox computeMD5SettingCheckbox;
    public CheckBox computeHistSettingCheckbox;
    public CheckBox buildThumbSettingCheckbox;
    public CheckBox autoImportWebSettingCheckbox;
    public TextField lastFolderSettingTextField;
    public Button settingsCancelButton;
    public ChoiceBox<Integer> gridWidthChoiceBox;
    public TextField dbURLTextfield;
    public TextField dbUserTextfield;
    public TextField dbPassTextfield;
    public CheckBox duplicateComputeMD5SettingCheckbox;
    public CheckBox duplicateComputeHistSettingCheckbox;
    public TextField histConfidenceSettingTextField;
    public CheckBox duplicateConsolidateTagsSettingCheckbox;
    public CheckBox backupDatabaseSettingCheckBox;

    public BorderPane tagListPane;
    public ChoiceBox<String> tagListOrderChoiceBox;
    public ListView<Tag> tagListListView;
    public TextField searchTagsScreenTextField;

    public BorderPane helpPane;

    public BorderPane progressLockPane;
    public ProgressBar progressLockProgressBar;
    public Label progressLockTitleLabel;
    public Label progressLockMessageLabel;
    public Label progressLockCountLabel;

    public BorderPane duplicatePane;
    public Label duplicateSimilarityLabel;
    public Label duplicateLeftInfoLabel;
    public TextField duplicateLeftPathTextField;
    public DynamicImageView duplicateLeftImageView;
    public ListView<Tag> duplicateLeftTagListView;
    public Label duplicateRightInfoLabel;
    public TextField duplicateRightPathTextField;
    public DynamicImageView duplicateRightImageView;
    public ListView<Tag> duplicateRightTagListView;

    public BorderPane slideshowPane;
    public DynamicImageView slideshowImageView;


    private Menagerie menagerie;
    private Search currentSearch = null;

    private ProgressLockThread currentProgressLockThread;
    private ImageInfo currentlyPreviewing = null;
    private String lastTagString = null;
    private List<SimilarPair> currentSimilarPairs = null;
    private SimilarPair currentlyPreviewingPair = null;
    private List<ImageInfo> currentSlideshow = null;
    private ImageInfo currentSlideshowShowing = null;

    private final Settings settings = new Settings(new File("menagerie.settings"));


    // ---------------------------------- Initializers ------------------------------------

    @FXML
    public void initialize() {

        //Backup database
        try {
            backupDatabase();
        } catch (IOException e) {
            e.printStackTrace();
            Main.showErrorMessage("Error", "Error while trying to back up the database: " + settings.getDbUrl(), e.getLocalizedMessage());
        }

        //Initialize the menagerie
        initMenagerie();

        //Init window listeners
        initWindowListeners();

        //Init screens
        initExplorerScreen();
        initSettingsScreen();
        initTagListScreen();
        initDuplicateScreen();

        //Apply a default search
        searchOnAction();

        //Init window props from settings
        Platform.runLater(this::initWindowPropertiesFromSettings);
    }

    private void initMenagerie() {
        try {
            Connection db = DriverManager.getConnection("jdbc:h2:" + settings.getDbUrl(), settings.getDbUser(), settings.getDbPass());
            DatabaseVersionUpdater.updateDatabase(db);

            menagerie = new Menagerie(db);
        } catch (SQLException e) {
            e.printStackTrace();
            Main.showErrorMessage("Database Error", "Error when connecting to database or verifying it", e.getLocalizedMessage());
            Platform.exit();
        }
    }

    private void initSettingsScreen() {
        //Initialize grid width setting choicebox
        Integer[] elements = new Integer[Settings.MAX_IMAGE_GRID_WIDTH - Settings.MIN_IMAGE_GRID_WIDTH + 1];
        for (int i = 0; i < elements.length; i++) elements[i] = i + Settings.MIN_IMAGE_GRID_WIDTH;
        gridWidthChoiceBox.getItems().addAll(elements);
        gridWidthChoiceBox.getSelectionModel().clearAndSelect(0);
    }

    private void initWindowPropertiesFromSettings() {
        Stage stage = ((Stage) explorerPane.getScene().getWindow());
        stage.setMaximized(settings.isWindowMaximized());
        if (settings.getWindowWidth() > 0) stage.setWidth(settings.getWindowWidth());
        if (settings.getWindowHeight() > 0) stage.setHeight(settings.getWindowHeight());
        if (settings.getWindowX() >= 0) stage.setX(settings.getWindowX());
        if (settings.getWindowY() >= 0) stage.setY(settings.getWindowY());
    }

    private void initDuplicateScreen() {
        duplicateLeftTagListView.setCellFactory(param -> new TagListCell());
        duplicateRightTagListView.setCellFactory(param -> new TagListCell());
        histConfidenceSettingTextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                try {
                    double d = Double.parseDouble(histConfidenceSettingTextField.getText());
                    if (d <= 0 || d > 1) {
                        histConfidenceSettingTextField.setText("0.95");
                    }
                } catch (NullPointerException | NumberFormatException e) {
                    histConfidenceSettingTextField.setText("0.95");
                }
            }
        });
    }

    private void initTagListScreen() {
        //Initialize tagList order choicebox
        tagListOrderChoiceBox.getItems().addAll("Name", "ID", "Frequency");
        tagListOrderChoiceBox.getSelectionModel().clearAndSelect(0);
        tagListOrderChoiceBox.setOnAction(event -> updateTagListListViewOrder());

        tagListListView.setCellFactory(param -> {
            TagListCell c = new TagListCell();
            c.setOnContextMenuRequested(event -> {
                MenuItem i1 = new MenuItem("Search this tag");
                i1.setOnAction(event1 -> {
                    searchTextField.setText(c.getItem().getName());
                    searchTextField.positionCaret(searchTextField.getText().length());
                    closeTagListScreen();
                    searchOnAction();
                });
                ContextMenu m = new ContextMenu(i1);
                m.show(c, event.getScreenX(), event.getScreenY());
            });
            return c;
        });

        searchTagsScreenTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            tagListListView.getItems().clear();
            menagerie.getTags().forEach(tag -> {
                if (tag.getName().toLowerCase().startsWith(newValue.toLowerCase())) tagListListView.getItems().add(tag);
            });
            updateTagListListViewOrder();
        });
    }

    private void initExplorerScreen() {
        //Set image grid width from settings
        setImageGridWidth(settings.getImageGridWidth());

        //Init image grid
        imageGridView.setSelectionListener(this::previewImage);
        imageGridView.setProgressQueueListener(this::openProgressLockScreen);
        imageGridView.setDuplicateRequestListener(this::processAndShowDuplicates);
        imageGridView.setSlideshowRequestListener(this::openSlideshowScreen);

        //Init drag/drop handlers
        explorerPane.setOnDragOver(event -> {
            if (event.getGestureSource() == null && (event.getDragboard().hasFiles() || event.getDragboard().hasUrl())) {
                event.acceptTransferModes(TransferMode.ANY);
            }
            event.consume();
        });
        explorerPane.setOnDragDropped(event -> {
            List<File> files = event.getDragboard().getFiles();
            String url = event.getDragboard().getUrl();

            if (files != null && !files.isEmpty()) {
                List<Runnable> queue = new ArrayList<>();
                files.forEach(file -> queue.add(() -> menagerie.importImage(file, settings.isComputeMD5OnImport(), settings.isComputeHistogramOnImport(), settings.isBuildThumbnailOnImport())));

                if (!queue.isEmpty()) {
                    openProgressLockScreen("Importing files", "Importing " + queue.size() + " files...", queue, null, null);
                }
            } else if (url != null && !url.isEmpty()) {
                Platform.runLater(() -> {
                    String folder = settings.getLastFolder();
                    if (!folder.endsWith("/") && !folder.endsWith("\\")) folder += "/";
                    String filename = URI.create(url).getPath().replaceAll("^.*/", "");
                    File target = new File(folder + filename);

                    while (!settings.isAutoImportFromWeb() || !target.getParentFile().exists() || target.exists() || !Filters.IMAGE_FILTER.accept(target)) {
                        target = openSaveImageDialog(new File(settings.getLastFolder()), filename);
                        if (target == null) return;
                        if (target.exists())
                            Main.showErrorMessage("Error", "File already exists, cannot be overwritten", target.getAbsolutePath());
                    }

                    final File finalTarget = target;
                    new Thread(() -> {
                        try {
                            downloadAndSaveFile(url, finalTarget);
                            Platform.runLater(() -> {
                                ImageInfo img = menagerie.importImage(finalTarget, settings.isComputeMD5OnImport(), settings.isComputeHistogramOnImport(), settings.isBuildThumbnailOnImport());
                                if (img == null) {
                                    if (!finalTarget.delete())
                                        System.out.println("Tried to delete a downloaded file, as it couldn't be imported, but failed: " + finalTarget);
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                            Platform.runLater(() -> Main.showErrorMessage("Unexpected error", "Error while trying to download image", e.getLocalizedMessage()));
                        }
                    }).start();
                });
            }
            event.consume();
        });

        //Init tag list cell factory
        tagListView.setCellFactory(param -> {
            TagListCell c = new TagListCell();
            c.setOnContextMenuRequested(event -> {
                if (c.getItem() != null) {
                    MenuItem i1 = new MenuItem("Add to search");
                    i1.setOnAction(event1 -> {
                        searchTextField.setText(searchTextField.getText().trim() + " " + c.getItem().getName());
                        searchOnAction();
                    });
                    MenuItem i2 = new MenuItem("Exclude from search");
                    i2.setOnAction(event1 -> {
                        searchTextField.setText(searchTextField.getText().trim() + " -" + c.getItem().getName());
                        searchOnAction();
                    });
                    MenuItem i3 = new MenuItem("Remove from selected");
                    i3.setOnAction(event1 -> imageGridView.getSelected().forEach(img -> img.removeTag(c.getItem())));
                    ContextMenu m = new ContextMenu(i1, i2, new SeparatorMenuItem(), i3);
                    m.show(c, event.getScreenX(), event.getScreenY());
                }
            });
            return c;
        });

        editTagsTextfield.setOptionsListener(prefix -> {
            prefix = prefix.toLowerCase();
            boolean negative = prefix.startsWith("-");
            if (negative) prefix = prefix.substring(1);

            List<String> results = new ArrayList<>();

            List<Tag> tags = new ArrayList<>(menagerie.getTags());
            tags.sort((o1, o2) -> o2.getFrequency() - o1.getFrequency());
            for (Tag tag : tags) {
                if (tag.getName().toLowerCase().startsWith(prefix)) {
                    if (negative) results.add("-" + tag.getName());
                    else results.add(tag.getName());
                }

                if (results.size() >= 8) break;
            }

            return results;
        });

        searchTextField.setTop(false);
        searchTextField.setOptionsListener(editTagsTextfield.getOptionsListener());
    }

    private void initWindowListeners() {
        Platform.runLater(() -> {
            Stage stage = ((Stage) rootPane.getScene().getWindow());

            //Bind window properties to settings
            stage.maximizedProperty().addListener((observable, oldValue, newValue) -> settings.setWindowMaximized(newValue));
            stage.widthProperty().addListener((observable, oldValue, newValue) -> settings.setWindowWidth(newValue.intValue()));
            stage.heightProperty().addListener((observable, oldValue, newValue) -> settings.setWindowHeight(newValue.intValue()));
            stage.xProperty().addListener((observable, oldValue, newValue) -> settings.setWindowX(newValue.intValue()));
            stage.yProperty().addListener((observable, oldValue, newValue) -> settings.setWindowY(newValue.intValue()));
        });
    }

    // ---------------------------------- Screen/Dialog openers ------------------------------------

    private File openSaveImageDialog(File folder, String filename) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save file from web");
        if (filename != null) fc.setInitialFileName(filename);
        if (folder != null) fc.setInitialDirectory(folder);
        fc.setSelectedExtensionFilter(Filters.IMAGE_EXTENSION_FILTER);
        return fc.showSaveDialog(explorerPane.getScene().getWindow());
    }

    private void openSettingsScreen() {
        //Update settings fx nodes
        lastFolderSettingTextField.setText(settings.getLastFolder());
        dbURLTextfield.setText(settings.getDbUrl());
        dbUserTextfield.setText(settings.getDbUser());
        dbPassTextfield.setText(settings.getDbPass());

        autoImportWebSettingCheckbox.setSelected(settings.isAutoImportFromWeb());
        computeMD5SettingCheckbox.setSelected(settings.isComputeMD5OnImport());
        computeHistSettingCheckbox.setSelected(settings.isComputeHistogramOnImport());
        buildThumbSettingCheckbox.setSelected(settings.isBuildThumbnailOnImport());
        duplicateComputeMD5SettingCheckbox.setSelected(settings.isComputeMD5ForSimilarity());
        duplicateComputeHistSettingCheckbox.setSelected(settings.isComputeHistogramForSimilarity());
        duplicateConsolidateTagsSettingCheckbox.setSelected(settings.isConsolidateTags());
        backupDatabaseSettingCheckBox.setSelected(settings.isBackupDatabase());

        histConfidenceSettingTextField.setText("" + settings.getSimilarityThreshold());

        gridWidthChoiceBox.getSelectionModel().select((Integer) settings.getImageGridWidth());

        //Enable pane
        explorerPane.setDisable(true);
        settingsPane.setDisable(false);
        settingsPane.setOpacity(1);
        settingsCancelButton.requestFocus();
    }

    private void closeSettingsScreen(boolean saveChanges) {
        //Disable pane
        explorerPane.setDisable(false);
        settingsPane.setDisable(true);
        settingsPane.setOpacity(0);
        imageGridView.requestFocus();

        if (saveChanges) {
            //Save settings to settings object
            settings.setLastFolder(lastFolderSettingTextField.getText());
            settings.setDbUrl(dbURLTextfield.getText());
            settings.setDbUser(dbUserTextfield.getText());
            settings.setDbPass(dbPassTextfield.getText());

            settings.setAutoImportFromWeb(autoImportWebSettingCheckbox.isSelected());
            settings.setComputeMD5OnImport(computeMD5SettingCheckbox.isSelected());
            settings.setComputeHistogramOnImport(computeHistSettingCheckbox.isSelected());
            settings.setBuildThumbnailOnImport(buildThumbSettingCheckbox.isSelected());
            settings.setComputeMD5ForSimilarity(duplicateComputeMD5SettingCheckbox.isSelected());
            settings.setComputeHistogramForSimilarity(duplicateComputeHistSettingCheckbox.isSelected());
            settings.setConsolidateTags(duplicateConsolidateTagsSettingCheckbox.isSelected());
            settings.setBackupDatabase(backupDatabaseSettingCheckBox.isSelected());

            settings.setSimilarityThreshold(Double.parseDouble(histConfidenceSettingTextField.getText()));

            settings.setImageGridWidth(gridWidthChoiceBox.getValue());

            setImageGridWidth(gridWidthChoiceBox.getValue());
        }
    }

    private void openTagListScreen() {
        tagListListView.getItems().clear();
        tagListListView.getItems().addAll(menagerie.getTags());
        updateTagListListViewOrder();

        explorerPane.setDisable(true);
        tagListPane.setDisable(false);
        tagListPane.setOpacity(1);
        tagListPane.requestFocus();
    }

    private void closeTagListScreen() {
        explorerPane.setDisable(false);
        tagListPane.setDisable(true);
        tagListPane.setOpacity(0);
        imageGridView.requestFocus();
    }

    private void openHelpScreen() {
        explorerPane.setDisable(true);
        helpPane.setDisable(false);
        helpPane.setOpacity(1);
        helpPane.requestFocus();
    }

    private void closeHelpScreen() {
        explorerPane.setDisable(false);
        helpPane.setDisable(true);
        helpPane.setOpacity(0);
        imageGridView.requestFocus();
    }

    @SuppressWarnings("SameParameterValue")
    private void openProgressLockScreen(String title, String message, List<Runnable> queue, ProgressLockThreadFinishListener finishListener, ProgressLockThreadCancelListener cancelListener) {
        if (currentProgressLockThread != null) currentProgressLockThread.stopRunning();

        currentProgressLockThread = new ProgressLockThread(queue);
        currentProgressLockThread.setUpdateListener((num, total) -> Platform.runLater(() -> {
            final double progress = (double) num / total;
            progressLockProgressBar.setProgress(progress);
            progressLockCountLabel.setText((int) (progress * 100) + "% - " + (total - num) + " remaining...");
        }));
        currentProgressLockThread.setCancelListener((num, total) -> {
            Platform.runLater(this::closeProgressLockScreen);
            if (cancelListener != null) cancelListener.progressCanceled(num, total);
        });
        currentProgressLockThread.setFinishListener(total -> {
            Platform.runLater(this::closeProgressLockScreen);
            if (finishListener != null) finishListener.progressFinished(total);
        });
        currentProgressLockThread.start();

        progressLockTitleLabel.setText(title);
        progressLockMessageLabel.setText(message);
        progressLockProgressBar.setProgress(0);
        progressLockCountLabel.setText("0/" + queue.size());

        explorerPane.setDisable(true);
        progressLockPane.setDisable(false);
        progressLockPane.setOpacity(1);
        progressLockPane.requestFocus();

    }

    private void closeProgressLockScreen() {
        if (currentProgressLockThread != null) currentProgressLockThread.stopRunning();

        explorerPane.setDisable(false);
        progressLockPane.setDisable(true);
        progressLockPane.setOpacity(0);
        imageGridView.requestFocus();
    }

    private void openDuplicateScreen(List<ImageInfo> images) {
        currentSimilarPairs = new ArrayList<>();
        List<Runnable> queue = new ArrayList<>();

        for (int actualI = 0; actualI < images.size(); actualI++) {
            final int i = actualI;
            queue.add(() -> {
                ImageInfo i1 = images.get(i);
                for (int j = i + 1; j < images.size(); j++) {
                    ImageInfo i2 = images.get(j);

                    //Compare md5 hashes
                    if (i1.getMD5() != null && i1.getMD5().equals(i2.getMD5())) {
                        currentSimilarPairs.add(new SimilarPair(i1, i2, 1.0));
                        continue;
                    }

                    //Compare histograms
                    if (i1.getHistogram() != null && i2.getHistogram() != null) {
                        double similarity = i1.getHistogram().getSimilarity(i2.getHistogram());
                        if (similarity >= settings.getSimilarityThreshold()) {
                            currentSimilarPairs.add(new SimilarPair(i1, i2, similarity));
                        }
                    }
                }
            });
        }

        if (queue.size() > 5000) {
            openProgressLockScreen("Comparing images", "Checking comparisons for " + queue.size() + " images...", queue, total -> Platform.runLater(() -> {
                if (currentSimilarPairs.isEmpty()) return;

                previewSimilarPair(currentSimilarPairs.get(0));

                explorerPane.setDisable(true);
                duplicatePane.setDisable(false);
                duplicatePane.setOpacity(1);
                duplicatePane.requestFocus();
            }), null);
        } else {
            queue.forEach(Runnable::run);

            if (currentSimilarPairs.isEmpty()) return;

            previewSimilarPair(currentSimilarPairs.get(0));

            explorerPane.setDisable(true);
            duplicatePane.setDisable(false);
            duplicatePane.setOpacity(1);
            duplicatePane.requestFocus();
        }
    }

    private void closeDuplicateScreen() {
        previewSimilarPair(null);

        explorerPane.setDisable(false);
        duplicatePane.setDisable(true);
        duplicatePane.setOpacity(0);
        imageGridView.requestFocus();
    }

    private void openSlideshowScreen(List<ImageInfo> images) {
        if (images == null || images.isEmpty()) return;

        currentSlideshow = images;
        currentSlideshowShowing = images.get(0);
        slideshowImageView.setImage(currentSlideshowShowing.getImage());

        explorerPane.setDisable(true);
        slideshowPane.setDisable(false);
        slideshowPane.setOpacity(1);
        slideshowPane.requestFocus();
    }

    private void closeSlideshowScreen() {
        explorerPane.setDisable(false);
        slideshowPane.setDisable(true);
        slideshowPane.setOpacity(0);
        imageGridView.requestFocus();
    }

    private void requestImportFolder() {
        DirectoryChooser dc = new DirectoryChooser();
        if (settings.getLastFolder() != null && !settings.getLastFolder().isEmpty())
            dc.setInitialDirectory(new File(settings.getLastFolder()));
        File result = dc.showDialog(rootPane.getScene().getWindow());

        if (result != null) {
            List<Runnable> queue = new ArrayList<>();
            List<File> files = getFilesRecursive(result, Filters.IMAGE_FILTER);
            menagerie.getImages().forEach(img -> files.remove(img.getFile()));
            files.forEach(file -> queue.add(() -> menagerie.importImage(file, settings.isComputeMD5OnImport(), settings.isComputeHistogramOnImport(), settings.isBuildThumbnailOnImport())));

            if (!queue.isEmpty()) {
                openProgressLockScreen("Importing files", "Importing " + queue.size() + " files...", queue, null, null);
            }
        }
    }

    private void requestImportFiles() {
        FileChooser fc = new FileChooser();
        if (settings.getLastFolder() != null && !settings.getLastFolder().isEmpty())
            fc.setInitialDirectory(new File(settings.getLastFolder()));
        fc.setSelectedExtensionFilter(Filters.IMAGE_EXTENSION_FILTER);
        List<File> results = fc.showOpenMultipleDialog(rootPane.getScene().getWindow());

        if (results != null && !results.isEmpty()) {
            menagerie.getImages().forEach(img -> results.remove(img.getFile()));

            List<Runnable> queue = new ArrayList<>();
            results.forEach(file -> queue.add(() -> menagerie.importImage(file, settings.isComputeMD5OnImport(), settings.isComputeHistogramOnImport(), settings.isBuildThumbnailOnImport())));

            if (!queue.isEmpty()) {
                openProgressLockScreen("Importing files", "Importing " + queue.size() + " files...", queue, null, null);
            }
        }
    }

    // ---------------------------------- GUI Action Methods ------------------------------------

    @SuppressWarnings("SameParameterValue")
    private void previewImage(ImageInfo image) {
        if (currentlyPreviewing != null) currentlyPreviewing.setTagListener(null);
        currentlyPreviewing = image;

        if (image != null) {
            image.setTagListener(() -> updateTagListViewContents(image));

            previewImageView.setImage(image.getImage());

            updateTagListViewContents(image);

            updateImageInfoLabel(image, imageInfoLabel);
        } else {
            previewImageView.setImage(null);
            tagListView.getItems().clear();
            updateImageInfoLabel(null, imageInfoLabel);
        }
    }

    private void updateTagListViewContents(ImageInfo image) {
        tagListView.getItems().clear();
        tagListView.getItems().addAll(image.getTags());
        tagListView.getItems().sort(Comparator.comparing(Tag::getName));
    }

    private static void updateImageInfoLabel(ImageInfo image, Label label) {
        if (image == null) {
            label.setText("Size: N/A - Res: N/A");

            return;
        }

        if (image.getImage().isBackgroundLoading() && image.getImage().getProgress() != 1) {
            label.setText("Size: N/A - Res: N/A");

            image.getImage().progressProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue.doubleValue() == 1 && !image.getImage().isError()) updateImageInfoLabel(image, label);
            });
        } else {
            //Find size string
            double size = image.getFile().length();
            String sizeStr;
            if (size > 1024 * 1024 * 1024) sizeStr = String.format("%.2f", size / 1024 / 1024 / 1024) + "GB";
            else if (size > 1024 * 1024) sizeStr = String.format("%.2f", size / 1024 / 1024) + "MB";
            else if (size > 1024) sizeStr = String.format("%.2f", size / 1024) + "KB";
            else sizeStr = String.format("%.2f", size) + "B";

            label.setText("Size: " + sizeStr + " - Res: " + (int) image.getImage().getWidth() + "x" + (int) image.getImage().getHeight());
        }
    }

    private void searchOnAction() {
        previewImageView.setImage(null);

        final boolean descending = descendingToggleButton.isSelected();

        List<SearchRule> rules = new ArrayList<>();
        for (String arg : searchTextField.getText().split("\\s+")) {
            if (arg == null || arg.isEmpty()) continue;

            if (arg.startsWith("id:")) {
                String temp = arg.substring(arg.indexOf(':') + 1);
                IDRule.Type type = IDRule.Type.EQUAL_TO;
                if (temp.startsWith("<")) {
                    type = IDRule.Type.LESS_THAN;
                    temp = temp.substring(1);
                } else if (temp.startsWith(">")) {
                    type = IDRule.Type.GREATER_THAN;
                    temp = temp.substring(1);
                }
                try {
                    rules.add(new IDRule(type, Integer.parseInt(temp)));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    Main.showErrorMessage("Error", "Error converting int value for ID rule", e.getLocalizedMessage());
                }
            } else if (arg.startsWith("date:") || arg.startsWith("time:")) {
                String temp = arg.substring(arg.indexOf(':') + 1);
                DateAddedRule.Type type = DateAddedRule.Type.EQUAL_TO;
                if (temp.startsWith("<")) {
                    type = DateAddedRule.Type.LESS_THAN;
                    temp = temp.substring(1);
                } else if (temp.startsWith(">")) {
                    type = DateAddedRule.Type.GREATER_THAN;
                    temp = temp.substring(1);
                }
                try {
                    rules.add(new DateAddedRule(type, Long.parseLong(temp)));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    Main.showErrorMessage("Error", "Error converting long value for date added rule", e.getLocalizedMessage());
                }
            } else if (arg.startsWith("md5:")) {
                rules.add(new MD5Rule(arg.substring(arg.indexOf(':') + 1)));
            } else if (arg.startsWith("path:") || arg.startsWith("file:")) {
                rules.add(new FilePathRule(arg.substring(arg.indexOf(':') + 1)));
            } else {
                boolean exclude = false;
                if (arg.startsWith("-")) {
                    arg = arg.substring(1);
                    exclude = true;
                }

                Tag tag = menagerie.getTagByName(arg);
                if (tag == null) tag = new Tag(-1, arg);
                rules.add(new TagRule(tag, exclude));
            }
        }

        if (currentSearch != null) currentSearch.close();
        currentSearch = new Search(menagerie, rules, descending);
        currentSearch.setListener(new SearchUpdateListener() {
            @Override
            public void imageAdded(ImageInfo img) {
                currentSearch.sortResults();
                Platform.runLater(() -> imageGridView.getItems().add(currentSearch.getResults().indexOf(img), img));
            }

            @Override
            public void imageRemoved(ImageInfo img) {
                Platform.runLater(() -> {
                    int index = imageGridView.getItems().indexOf(img) + 1;
                    if (index < imageGridView.getItems().size())
                        imageGridView.setLastSelected(imageGridView.getItems().get(index));
                    else if (index - 1 >= 0) imageGridView.setLastSelected(imageGridView.getItems().get(index - 1));

                    if (img.equals(currentlyPreviewing)) previewImage(null);

                    imageGridView.deselect(img);
                    imageGridView.getItems().remove(img);
                });
            }
        });

//        Thread thread = new Thread(() -> {
//            long t = System.currentTimeMillis();
//            List<String> md5s = new ArrayList<>();
//            images.forEach(img -> md5s.add(img.getMD5()));
//            for (int i = 0; i < images.size() - 1; i++) {
//                String h1 = md5s.get(i);
//                for (int j = i + 1; j < images.size(); j++) {
//                    String h2 = md5s.get(j);
//                    if (h1 != null && h1.equals(h2)) {
//                        System.out.println(h1 + " duplicate pair (" + images.get(i).getId() + "," + images.get(j).getId() + ")");
//                    }
//                }
//            }
//            System.out.println((System.currentTimeMillis() - t) / 1000.0 + "s");
//            menagerie.getUpdateQueue().commit();
//        });
//        thread.setDaemon(true);
//        thread.start();

        resultCountLabel.setText("Results: " + currentSearch.getResults().size());
        imageGridView.clearSelection();
        imageGridView.getItems().clear();
        imageGridView.getItems().addAll(currentSearch.getResults());

        if (!imageGridView.getItems().isEmpty()) imageGridView.select(imageGridView.getItems().get(0), false, false);
    }

    private void setImageGridWidth(int n) {
        final double width = 18 + (ImageInfo.THUMBNAIL_SIZE + ImageGridView.CELL_BORDER * 2 + imageGridView.getHorizontalCellSpacing() * 2) * n;
        imageGridView.setMinWidth(width);
        imageGridView.setMaxWidth(width);
        imageGridView.setPrefWidth(width);
    }

    private void previewSimilarPair(SimilarPair pair) {
        if (pair == null) {
            currentlyPreviewingPair = null;

            duplicateLeftImageView.setImage(null);
            duplicateLeftTagListView.getItems().clear();
            duplicateLeftPathTextField.setText("N/A");
            updateImageInfoLabel(null, duplicateLeftInfoLabel);

            duplicateRightImageView.setImage(null);
            duplicateRightTagListView.getItems().clear();
            duplicateRightPathTextField.setText("N/A");
            updateImageInfoLabel(null, duplicateRightInfoLabel);

            duplicateSimilarityLabel.setText("N/A% Match");
        } else {
            currentlyPreviewingPair = pair;

            duplicateLeftImageView.setImage(pair.getImg1().getImage());
            duplicateLeftPathTextField.setText(pair.getImg1().getFile().toString());
            duplicateLeftTagListView.getItems().clear();
            duplicateLeftTagListView.getItems().addAll(pair.getImg1().getTags());
            duplicateLeftTagListView.getItems().sort(Comparator.comparing(Tag::getName));
            updateImageInfoLabel(pair.getImg1(), duplicateLeftInfoLabel);

            duplicateRightImageView.setImage(pair.getImg2().getImage());
            duplicateRightPathTextField.setText(pair.getImg2().getFile().toString());
            duplicateRightTagListView.getItems().clear();
            duplicateRightTagListView.getItems().addAll(pair.getImg2().getTags());
            duplicateRightTagListView.getItems().sort(Comparator.comparing(Tag::getName));
            updateImageInfoLabel(pair.getImg2(), duplicateRightInfoLabel);

            DecimalFormat df = new DecimalFormat("#.##");
            duplicateSimilarityLabel.setText((currentSimilarPairs.indexOf(pair) + 1) + "/" + currentSimilarPairs.size() + " - " + df.format(pair.getSimilarity() * 100) + "% Match");
        }
    }

    private void previewLastSimilarPair() {
        if (currentSimilarPairs == null || currentSimilarPairs.isEmpty()) return;

        if (currentlyPreviewingPair == null) {
            previewSimilarPair(currentSimilarPairs.get(0));
        } else {
            int i = currentSimilarPairs.indexOf(currentlyPreviewingPair);
            if (i > 0) {
                previewSimilarPair(currentSimilarPairs.get(i - 1));
            } else {
                previewSimilarPair(currentSimilarPairs.get(0));
            }
        }
    }

    private void previewNextSimilarPair() {
        if (currentSimilarPairs == null || currentSimilarPairs.isEmpty()) return;

        if (currentlyPreviewingPair == null) {
            previewSimilarPair(currentSimilarPairs.get(0));
        } else {
            int i = currentSimilarPairs.indexOf(currentlyPreviewingPair);
            if (i >= 0) {
                if (i + 1 < currentSimilarPairs.size()) previewSimilarPair(currentSimilarPairs.get(i + 1));
            } else {
                previewSimilarPair(currentSimilarPairs.get(0));
            }
        }
    }

    private void updateTagListListViewOrder() {
        switch (tagListOrderChoiceBox.getValue()) {
            case "ID":
                tagListListView.getItems().sort(Comparator.comparingInt(Tag::getId));
                break;
            case "Frequency":
                tagListListView.getItems().sort(Comparator.comparingInt(Tag::getFrequency).reversed());
                break;
            case "Name":
                tagListListView.getItems().sort(Comparator.comparing(Tag::getName));
                break;
        }
    }

    private void editTagsOfSelected(String input) {
        if (input == null || input.isEmpty() || imageGridView.getSelected().isEmpty()) return;
        lastTagString = input.trim();

        for (String text : input.split("\\s+")) {
            if (text.startsWith("-")) {
                Tag t = menagerie.getTagByName(text.substring(1));
                if (t != null) {
                    new ArrayList<>(imageGridView.getSelected()).forEach(img -> img.removeTag(t)); // New arraylist to avoid concurrent modification
                }
            } else {
                Tag t = menagerie.getTagByName(text);
                if (t == null) t = menagerie.createTag(text);
                for (ImageInfo img : new ArrayList<>(imageGridView.getSelected())) { // New arraylist to avoid concurrent modification
                    img.addTag(t);
                }
            }
        }
    }

    private void deleteDuplicateImageEvent(ImageInfo toDelete, ImageInfo toKeep) {
        int index = currentSimilarPairs.indexOf(currentlyPreviewingPair);
        toDelete.remove(true);

        //Consolidate tags
        if (settings.isConsolidateTags()) {
            toDelete.getTags().forEach(toKeep::addTag);
        }

        //Remove other pairs containing the deleted image
        for (SimilarPair pair : new ArrayList<>(currentSimilarPairs)) {
            if (toDelete.equals(pair.getImg1()) || toDelete.equals(pair.getImg2())) {
                int i = currentSimilarPairs.indexOf(pair);
                currentSimilarPairs.remove(pair);
                if (i < index) {
                    index--;
                }
            }
        }

        if (index > currentSimilarPairs.size() - 1) index = currentSimilarPairs.size() - 1;

        if (currentSimilarPairs.isEmpty()) {
            closeDuplicateScreen();
        } else {
            previewSimilarPair(currentSimilarPairs.get(index));
        }
    }

    private void processAndShowDuplicates(List<ImageInfo> images) {
        if (settings.isComputeMD5ForSimilarity()) {
            List<Runnable> queue = new ArrayList<>();

            images.forEach(i -> {
                if (i.getMD5() == null) queue.add(() -> {
                    i.initializeMD5();
                    i.commitMD5ToDatabase();
                });
            });

            openProgressLockScreen("Building MD5s", "Building MD5 hashes for " + queue.size() + " files...", queue, total -> {
                //TODO: Fix this. If md5 computing is disabled, histogram building won't happen
                if (settings.isComputeHistogramForSimilarity()) {
                    List<Runnable> queue2 = new ArrayList<>();

                    images.forEach(i -> {
                        if (i.getHistogram() == null) queue2.add(() -> {
                            i.initializeHistogram();
                            i.commitHistogramToDatabase();
                        });
                    });

                    Platform.runLater(() -> openProgressLockScreen("Building Histograms", "Building histograms for " + queue2.size() + " files...", queue2, total1 -> Platform.runLater(() -> openDuplicateScreen(images)), null));
                } else {
                    Platform.runLater(() -> openDuplicateScreen(images));
                }
            }, null);
        } else {
            openDuplicateScreen(images);
        }
    }

    private void slideshowShowNext() {
        int i = currentSlideshow.indexOf(currentSlideshowShowing);
        if (i + 1 < currentSlideshow.size()) currentSlideshowShowing = currentSlideshow.get(i + 1);
        slideshowImageView.setImage(currentSlideshowShowing.getImage());
    }

    private void slideshowShowPrevious() {
        int i = currentSlideshow.indexOf(currentSlideshowShowing);
        if (i - 1 >= 0) currentSlideshowShowing = currentSlideshow.get(i - 1);
        slideshowImageView.setImage(currentSlideshowShowing.getImage());
    }

    // ---------------------------------- Compute Utilities ------------------------------------

    private static void downloadAndSaveFile(String url, File target) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.addRequestProperty("User-Agent", "Mozilla/4.0");
        ReadableByteChannel rbc = Channels.newChannel(conn.getInputStream());
        FileOutputStream fos = new FileOutputStream(target);
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        conn.disconnect();
        rbc.close();
        fos.close();
    }

    private static List<File> getFilesRecursive(File folder, FileFilter filter) {
        List<File> results = new ArrayList<>();
        for (File file : Objects.requireNonNull(folder.listFiles())) {
            if (file.isDirectory()) {
                results.addAll(getFilesRecursive(file, filter));
            } else {
                if (filter.accept(file)) results.add(file);
            }
        }
        return results;
    }

    private void backupDatabase() throws IOException {
        if (!settings.isBackupDatabase()) return;

        String path = settings.getDbUrl() + ".mv.db";
        if (path.startsWith("~")) {
            String temp = System.getProperty("user.home");
            if (!temp.endsWith("/") && !temp.endsWith("\\")) temp += "/";
            path = path.substring(1);
            if (path.startsWith("/") || path.startsWith("\\")) path = path.substring(1);

            path = temp + path;
        }

        File currentDatabaseFile = new File(path);

        if (currentDatabaseFile.exists()) {
            System.out.println("Backing up database at: " + currentDatabaseFile);
            File backupFile = new File(path + ".bak");
            Files.copy(currentDatabaseFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Successfully backed up database to: " + backupFile);
        }
    }

    // ---------------------------------- Event Handlers ------------------------------------

    public void searchButtonOnAction(ActionEvent event) {
        searchOnAction();
        imageGridView.requestFocus();
        event.consume();
    }

    public void searchTextFieldOnAction(ActionEvent event) {
        searchOnAction();
        imageGridView.requestFocus();
        event.consume();
    }

    public void explorerPaneOnKeyPressed(KeyEvent event) {
        if (event.isControlDown()) {
            switch (event.getCode()) {
                case F:
                    searchTextField.requestFocus();
                    event.consume();
                    break;
                case E:
                    editTagsTextfield.setText(lastTagString);
                    editTagsTextfield.requestFocus();
                    event.consume();
                    break;
                case Q:
                    menagerie.getUpdateQueue().enqueueUpdate(Platform::exit);
                    menagerie.getUpdateQueue().commit();
                    event.consume();
                    break;
                case S:
                    openSettingsScreen();
                    event.consume();
                    break;
                case T:
                    openTagListScreen();
                    event.consume();
                    break;
                case I:
                    if (event.isShiftDown())
                        requestImportFolder();
                    else
                        requestImportFiles();
                    event.consume();
                    break;
                case H:
                    openHelpScreen();
                    event.consume();
                    break;
                case D:
                    processAndShowDuplicates(imageGridView.getSelected());
                    event.consume();
                    break;
            }
        }

        switch (event.getCode()) {
            case ESCAPE:
                imageGridView.requestFocus();
                event.consume();
                break;
            case ALT:
                event.consume();
                break;
        }
    }

    public void explorerPaneOnKeyReleased(KeyEvent event) {
        if (event.getCode() == KeyCode.ALT) {
            if (menuBar.isFocused()) {
                imageGridView.requestFocus();
            } else {
                menuBar.requestFocus();
            }
            event.consume();
        }
    }

    public void settingsAcceptButtonOnAction(ActionEvent event) {
        closeSettingsScreen(true);
        event.consume();
    }

    public void settingsCancelButtonOnAction(ActionEvent event) {
        closeSettingsScreen(false);
        event.consume();
    }

    public void lastFolderSettingBrowseButtonOnAction(ActionEvent event) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Choose default save folder");
        if (lastFolderSettingTextField.getText() != null && !lastFolderSettingTextField.getText().isEmpty())
            dc.setInitialDirectory(new File(lastFolderSettingTextField.getText()));
        File result = dc.showDialog(settingsPane.getScene().getWindow());

        if (result != null) {
            lastFolderSettingTextField.setText(result.getAbsolutePath());
        }

        event.consume();
    }

    public void settingsPaneKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case ESCAPE:
                closeSettingsScreen(false);
                event.consume();
                break;
            case ENTER:
                closeSettingsScreen(true);
                event.consume();
                break;
            case S:
                if (event.isControlDown()) {
                    closeSettingsScreen(false);
                    event.consume();
                }
                break;
        }
    }

    public void editTagsTextfieldOnKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case SPACE:
                editTagsOfSelected(editTagsTextfield.getText());
                Platform.runLater(() -> editTagsTextfield.setText(null));
                event.consume();
                break;
            case ENTER:
                editTagsOfSelected(editTagsTextfield.getText());
                editTagsTextfield.setText(null);
                imageGridView.requestFocus();
                event.consume();
                break;
            case ESCAPE:
                editTagsTextfield.setText(null);
                imageGridView.requestFocus();
                event.consume();
                break;
        }
    }

    public void tagListExitButtonOnAction(ActionEvent event) {
        closeTagListScreen();
        event.consume();
    }

    public void tagListPaneOnKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case ESCAPE:
                closeTagListScreen();
                event.consume();
                break;
            case T:
                if (event.isControlDown()) {
                    closeTagListScreen();
                    event.consume();
                }
                break;
        }
    }

    public void helpExitButtonOnAction(ActionEvent event) {
        closeHelpScreen();
        event.consume();
    }

    public void helpPaneOnKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case ESCAPE:
                closeHelpScreen();
                event.consume();
                break;
            case H:
                if (event.isControlDown()) {
                    closeHelpScreen();
                    event.consume();
                }
                break;
        }
    }

    public void progressLockStopButtonOnAction(ActionEvent event) {
        closeProgressLockScreen();
        event.consume();
    }

    public void progressLockPaneOnKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case ESCAPE:
                closeProgressLockScreen();
                event.consume();
                break;
        }
    }

    public void duplicateLeftDeleteButtonOnAction(ActionEvent event) {
        if (currentlyPreviewingPair != null)
            deleteDuplicateImageEvent(currentlyPreviewingPair.getImg1(), currentlyPreviewingPair.getImg2());

        event.consume();
    }

    public void duplicateRightDeleteButtonOnAction(ActionEvent event) {
        if (currentlyPreviewingPair != null)
            deleteDuplicateImageEvent(currentlyPreviewingPair.getImg2(), currentlyPreviewingPair.getImg1());

        event.consume();
    }

    public void duplicateCloseButtonOnAction(ActionEvent event) {
        closeDuplicateScreen();
        event.consume();
    }

    public void duplicatePaneOnKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case ESCAPE:
                closeDuplicateScreen();
                event.consume();
                break;
            case LEFT:
                previewLastSimilarPair();
                event.consume();
                break;
            case RIGHT:
                previewNextSimilarPair();
                event.consume();
                break;
        }
    }

    public void duplicatePrevPairButtonOnAction(ActionEvent event) {
        previewLastSimilarPair();
        event.consume();
    }

    public void duplicateNextPairButtonOnAction(ActionEvent event) {
        previewNextSimilarPair();
        event.consume();
    }

    public void duplicateImagesPaneMouseEntered(MouseEvent event) {
        duplicateLeftTagListView.setDisable(false);
        duplicateRightTagListView.setDisable(false);
        duplicateLeftTagListView.setOpacity(0.75);
        duplicateRightTagListView.setOpacity(0.75);
        event.consume();
    }

    public void duplicateImagesPaneMouseExited(MouseEvent event) {
        duplicateLeftTagListView.setDisable(true);
        duplicateRightTagListView.setDisable(true);
        duplicateLeftTagListView.setOpacity(0);
        duplicateRightTagListView.setOpacity(0);
        event.consume();
    }

    public void slideshowPaneOnKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case RIGHT:
                slideshowShowNext();
                event.consume();
                break;
            case LEFT:
                slideshowShowPrevious();
                event.consume();
                break;
            case ESCAPE:
                closeSlideshowScreen();
                event.consume();
                break;
        }
    }

    public void slideshowPreviousButtonOnAction(ActionEvent event) {
        slideshowShowPrevious();
        event.consume();
    }

    public void slideshowCloseButtonOnAction(ActionEvent event) {
        closeSlideshowScreen();
        event.consume();
    }

    public void slideshowNextButtonOnAction(ActionEvent event) {
        slideshowShowNext();
        event.consume();
    }

    public void importFilesMenuButtonOnAction(ActionEvent event) {
        requestImportFiles();
        event.consume();
    }

    public void importFolderMenuButtonOnAction(ActionEvent event) {
        requestImportFolder();
        event.consume();
    }

    public void settingsMenuButtonOnAction(ActionEvent event) {
        openSettingsScreen();
        event.consume();
    }

    public void helpMenuButtonOnAction(ActionEvent event) {
        openHelpScreen();
        event.consume();
    }

    public void slideshowSearchedMenuButtonOnAction(ActionEvent event) {
        openSlideshowScreen(currentSearch.getResults());
        event.consume();
    }

    public void slideshowSelectedMenuButtonOnAction(ActionEvent event) {
        openSlideshowScreen(imageGridView.getSelected());
        event.consume();
    }

    public void viewTagsMenuButtonOnAction(ActionEvent event) {
        openTagListScreen();
        event.consume();
    }

}
