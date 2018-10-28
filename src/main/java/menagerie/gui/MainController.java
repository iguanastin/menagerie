package menagerie.gui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import menagerie.model.ImageInfo;
import menagerie.model.Menagerie;
import menagerie.model.Tag;
import menagerie.model.db.DatabaseVersionUpdater;
import menagerie.model.search.*;
import menagerie.model.settings.Settings;
import menagerie.util.Filters;
import org.controlsfx.control.ToggleSwitch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MainController {

    public StackPane rootPane;

    public BorderPane explorerPane;
    public ToggleButton descendingToggleButton;
    public TextField searchTextField;
    public ImageGridView imageGridView;
    public DynamicImageView previewImageView;
    public Label resultCountLabel;
    public Label imageInfoLabel;
    public ListView<String> tagListView;

    public BorderPane settingsPane;
    public ToggleSwitch computeMD5SettingCheckbox;
    public ToggleSwitch computeHistSettingCheckbox;
    public ToggleSwitch buildThumbSettingCheckbox;
    public ToggleSwitch autoImportWebSettingCheckbox;
    public TextField lastFolderSettingTextField;
    public Button settingsCancelButton;
    public ChoiceBox<Integer> gridWidthChoiceBox;

    private Menagerie menagerie;
    private Search currentSearch = null;

    private Settings settings = new Settings(new File("menagerie.settings"));


    @FXML
    public void initialize() {
        initMenagerie();

        initFX();
        initListeners();
    }

    private void initMenagerie() {
        try {
            Connection db = DriverManager.getConnection("jdbc:h2:" + settings.getDbUrl(), settings.getDbUser(), settings.getDbPass());
            if (!DatabaseVersionUpdater.upToDate(db)) {
                DatabaseVersionUpdater.updateDatabase(db);
            }

            menagerie = new Menagerie(db);
        } catch (SQLException e) {
            e.printStackTrace();
            Main.showErrorMessage("Database Error", "Error when connecting to database or verifying it", e.getLocalizedMessage());
            Platform.exit();
        }
    }

    private void initFX() {
        Platform.runLater(() -> {
            Stage stage = ((Stage) explorerPane.getScene().getWindow());
            stage.setMaximized(settings.isWindowMaximized());
            if (settings.getWindowWidth() > 0) stage.setWidth(settings.getWindowWidth());
            if (settings.getWindowHeight() > 0) stage.setHeight(settings.getWindowHeight());
            if (settings.getWindowX() >= 0) stage.setX(settings.getWindowX());
            if (settings.getWindowY() >= 0) stage.setY(settings.getWindowY());
        });

        updateImageInfoLabel(null);

        //Ensure two columns for grid
        setImageGridWidth(settings.getImageGridWidth());

        Integer[] elements = new Integer[Settings.MAX_IMAGE_GRID_WIDTH - Settings.MIN_IMAGE_GRID_WIDTH + 1];
        for (int i = 0; i < elements.length; i++) elements[i] = i + Settings.MIN_IMAGE_GRID_WIDTH;
        gridWidthChoiceBox.getItems().addAll(elements);
        gridWidthChoiceBox.getSelectionModel().clearAndSelect(0);
    }

    private void initListeners() {
        Platform.runLater(() -> {
            Stage stage = ((Stage) rootPane.getScene().getWindow());

            //Bind window properties to settings
            stage.maximizedProperty().addListener((observable, oldValue, newValue) -> settings.setWindowMaximized(newValue));
            stage.widthProperty().addListener((observable, oldValue, newValue) -> settings.setWindowWidth(newValue.intValue()));
            stage.heightProperty().addListener((observable, oldValue, newValue) -> settings.setWindowHeight(newValue.intValue()));
            stage.xProperty().addListener((observable, oldValue, newValue) -> settings.setWindowX(newValue.intValue()));
            stage.yProperty().addListener((observable, oldValue, newValue) -> settings.setWindowY(newValue.intValue()));
        });

        imageGridView.setSelectionListener(image -> {
            previewImageView.setImage(image.getImage());
            tagListView.getItems().clear();
            image.getTags().forEach(tag -> tagListView.getItems().add(tag.getName()));
            tagListView.getItems().sort(null);

            if (!image.getImage().isBackgroundLoading() || image.getImage().getProgress() == 1) {
                updateImageInfoLabel(image);
            } else {
                updateImageInfoLabel(null);
                image.getImage().progressProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue.doubleValue() == 1 && !image.getImage().isError()) updateImageInfoLabel(image);
                });
            }
        });

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
                files.forEach(file -> menagerie.importImage(file, settings.isComputeMD5OnImport(), settings.isComputeHistogramOnImport(), settings.isBuildThumbnailOnImport()));
            } else if (url != null && !url.isEmpty()) {
                Platform.runLater(() -> {
                    String folder = settings.getLastFolder();
                    // Regex removes everything up through the last slash of the url's path
                    String filename = URI.create(url).getPath().replaceAll("^.*/", "");

                    if (!settings.isAutoImportFromWeb() || folder == null || !new File(folder).exists()) {
                        FileChooser fc = new FileChooser();
                        fc.setTitle("Save file from web");
                        fc.setInitialFileName(filename);
                        fc.setSelectedExtensionFilter(Filters.IMAGE_EXTENSION_FILTER);
                        File result = fc.showSaveDialog(explorerPane.getScene().getWindow());

                        if (result == null) return;

                        settings.setLastFolder(result.getParent());
                        folder = result.getParent();
                        filename = result.getName();
                    }

                    if (!folder.endsWith("\\") && !folder.endsWith("/")) folder = folder + "/";
                    File target = new File(folder + filename);

                    if (target.exists()) {
                        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
                        a.setTitle("Replace file?");
                        a.setHeaderText(target.toString());
                        a.setContentText("File already exists, replace?");
                        Optional r = a.showAndWait();
                        if (r.isPresent() && r.get() != ButtonType.OK) {
                            return;
                        }
                    }

                    new Thread(() -> {
                        try {
                            downloadAndSaveFile(url, target);
                            Platform.runLater(() -> {
                                ImageInfo img = menagerie.importImage(target, settings.isComputeMD5OnImport(), settings.isComputeHistogramOnImport(), settings.isBuildThumbnailOnImport());
                                if (img == null) target.delete();
                                else if (settings.isBuildThumbnailOnImport()) img.getThumbnail();
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }).start();
                });
            }
            event.consume();
        });
    }

    private void updateImageInfoLabel(ImageInfo image) {
        if (image == null) {
            imageInfoLabel.setText("Size: N/A - Res: N/A");

            return;
        }

        //Find size string
        double size = image.getFile().length();
        String sizeStr;
        if (size > 1024 * 1024 * 1024) sizeStr = String.format("%.2f", size / 1024 / 1024 / 1024) + "GB";
        else if (size > 1024 * 1024) sizeStr = String.format("%.2f", size / 1024 / 1024) + "MB";
        else if (size > 1024) sizeStr = String.format("%.2f", size / 1024) + "KB";
        else sizeStr = String.format("%.2f", size) + "B";

        imageInfoLabel.setText("Size: " + sizeStr + " - Res: " + image.getImage().getWidth() + "x" + image.getImage().getHeight());
    }

    private void searchOnAction() {
        previewImageView.setImage(null);

        final boolean descending = descendingToggleButton.isSelected();

        List<SearchRule> rules = new ArrayList<>();
        for (String arg : searchTextField.getText().split("\\s+")) {
            if (arg == null || arg.isEmpty()) continue;

            if (arg.startsWith("id:")) {
                String temp = arg.substring(3);
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
                String temp = arg.substring(5);
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
            } else if (arg.startsWith("-")) {
                Tag tag = menagerie.getTagByName(arg.substring(1));
                if (tag == null) tag = new Tag(-1, arg.substring(1));
                rules.add(new TagRule(tag, true));
            } else {
                Tag tag = menagerie.getTagByName(arg);
                if (tag == null) tag = new Tag(-1, arg);
                rules.add(new TagRule(tag, false));
            }
        }

        if (currentSearch != null) currentSearch.close();
        currentSearch = new Search(menagerie, rules, descending);
        currentSearch.setListener(new SearchUpdateListener() {
            @Override
            public void imageAdded(ImageInfo img) {
                currentSearch.sortResults();
                imageGridView.getItems().add(currentSearch.getResults().indexOf(img), img);
            }

            @Override
            public void imageRemoved(ImageInfo img) {
                imageGridView.getItems().remove(img);
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
    }

    private void setImageGridWidth(int n) {
        final double width = 18 + (ImageInfo.THUMBNAIL_SIZE + ImageGridView.CELL_BORDER * 2 + imageGridView.getHorizontalCellSpacing() * 2) * n;
        imageGridView.setMinWidth(width);
        imageGridView.setMaxWidth(width);
        imageGridView.setPrefWidth(width);
    }

    public void searchButtonOnAction(ActionEvent event) {
        searchOnAction();
        imageGridView.requestFocus();
    }

    public void searchTextFieldOnAction(ActionEvent event) {
        searchOnAction();
        imageGridView.requestFocus();
    }

    public void explorerPaneOnKeyPressed(KeyEvent event) {
        if (event.isControlDown()) {
            switch (event.getCode()) {
                case F:
                    searchTextField.requestFocus();
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
            }
        }

        switch (event.getCode()) {
            case ESCAPE:
                imageGridView.requestFocus();
                event.consume();
                break;
        }
    }

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

    private void openSettingsScreen() {
        //Update settings fx nodes
        lastFolderSettingTextField.setText(settings.getLastFolder());
        autoImportWebSettingCheckbox.setSelected(settings.isAutoImportFromWeb());
        computeMD5SettingCheckbox.setSelected(settings.isComputeMD5OnImport());
        computeHistSettingCheckbox.setSelected(settings.isComputeHistogramOnImport());
        buildThumbSettingCheckbox.setSelected(settings.isBuildThumbnailOnImport());
        gridWidthChoiceBox.getSelectionModel().select((Integer) settings.getImageGridWidth());

        //Enable pane
        explorerPane.setDisable(true);
        settingsPane.setDisable(false);
        settingsPane.setOpacity(1);
        settingsCancelButton.requestFocus();
    }

    private void closeSettingsScreen(boolean saveChanges) {
        if (saveChanges) {
            //Save settings to settings object
            settings.setLastFolder(lastFolderSettingTextField.getText());
            settings.setAutoImportFromWeb(autoImportWebSettingCheckbox.isSelected());
            settings.setComputeMD5OnImport(computeMD5SettingCheckbox.isSelected());
            settings.setComputeHistogramOnImport(computeHistSettingCheckbox.isSelected());
            settings.setBuildThumbnailOnImport(buildThumbSettingCheckbox.isSelected());
            settings.setImageGridWidth(gridWidthChoiceBox.getValue());
            setImageGridWidth(gridWidthChoiceBox.getValue());
        }

        //Disable pane
        explorerPane.setDisable(false);
        settingsPane.setDisable(true);
        settingsPane.setOpacity(0);
        imageGridView.requestFocus();
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
        }
    }

}
