package menagerie.gui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
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

import java.io.File;
import java.io.FileFilter;
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
import java.util.*;

public class MainController {

    public StackPane rootPane;

    public BorderPane explorerPane;
    public ToggleButton descendingToggleButton;
    public TextField searchTextField;
    public ImageGridView imageGridView;
    public DynamicImageView previewImageView;
    public Label resultCountLabel;
    public Label imageInfoLabel;
    public ListView<Tag> tagListView;
    public TextField editTagsTextfield;

    private ContextMenu autoCompleteContextMenu;

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

    public BorderPane tagListPane;
    public ChoiceBox<String> tagListOrderChoiceBox;
    public ListView<Tag> tagListListView;

    private Menagerie menagerie;
    private Search currentSearch = null;

    private ImageInfo currentlyPreviewing = null;
    private String lastTagString = null;

    private Settings settings = new Settings(new File("menagerie.settings"));


    @FXML
    public void initialize() {
        initMenagerie();

        initFX();
        initListeners();

        Platform.runLater(this::searchOnAction);
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
        //Init window props from settings
        Platform.runLater(this::initWindowPropertiesFromSettings);

        //Ensure image preview is cleared
        previewImage(null);

        //Ensure two columns for grid
        setImageGridWidth(settings.getImageGridWidth());

        //Init editTagsTextfield autocomplete context menu
        autoCompleteContextMenu = new ContextMenu();

        //Initialize grid width setting choicebox
        Integer[] elements = new Integer[Settings.MAX_IMAGE_GRID_WIDTH - Settings.MIN_IMAGE_GRID_WIDTH + 1];
        for (int i = 0; i < elements.length; i++) elements[i] = i + Settings.MIN_IMAGE_GRID_WIDTH;
        gridWidthChoiceBox.getItems().addAll(elements);
        gridWidthChoiceBox.getSelectionModel().clearAndSelect(0);

        //Initialize tagList order choicebox
        tagListOrderChoiceBox.getItems().addAll("Name", "ID", "Frequency");
        tagListOrderChoiceBox.getSelectionModel().clearAndSelect(0);
    }

    private void initWindowPropertiesFromSettings() {
        Stage stage = ((Stage) explorerPane.getScene().getWindow());
        stage.setMaximized(settings.isWindowMaximized());
        if (settings.getWindowWidth() > 0) stage.setWidth(settings.getWindowWidth());
        if (settings.getWindowHeight() > 0) stage.setHeight(settings.getWindowHeight());
        if (settings.getWindowX() >= 0) stage.setX(settings.getWindowX());
        if (settings.getWindowY() >= 0) stage.setY(settings.getWindowY());
    }

    private void initListeners() {
        initWindowListeners();
        initTagListViewListeners();
        initExplorerPaneListeners();
        initTagListScreenListeners();
        imageGridView.setSelectionListener(this::previewImage);
        //TODO: Fix event passthrough. Pressing enter or space doesn't get caught by the onKeyPressed handler that's pushing tag changes into the model
//        initEditTagsAutoComplete();
    }

    private void initTagListScreenListeners() {
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
    }

    private void initEditTagsAutoComplete() {
        editTagsTextfield.textProperty().addListener((observable, oldValue, str) -> {
            if (str != null && !str.isEmpty()) {
                str = str.trim();
                boolean subtract = str.startsWith("-");
                if (subtract) str = str.substring(1);

                if (autoCompleteContextMenu.isShowing()) autoCompleteContextMenu.hide();

                autoCompleteContextMenu.getItems().clear();
                int i = 0;
                for (Tag t : menagerie.getTags()) {
                    if (i >= 10) break;

                    if (t.getName().startsWith(str)) {
                        MenuItem m = new MenuItem(t.getName());
                        m.setMnemonicParsing(false);
                        m.setOnAction(event -> {
                            if (subtract) editTagsTextfield.setText("-" + t.getName());
                            else editTagsTextfield.setText(t.getName());
                            editTagsTextfield.positionCaret(editTagsTextfield.getText().length());
                            event.consume();
                        });
                        autoCompleteContextMenu.getItems().add(m);
                        i++;
                    }
                }

                if (i > 0) {
                    Bounds b = editTagsTextfield.localToScreen(editTagsTextfield.getBoundsInLocal());
                    if (!autoCompleteContextMenu.isShowing())
                        autoCompleteContextMenu.show(editTagsTextfield, 0, 0);
                    autoCompleteContextMenu.setX(b.getMinX() - 10);
                    autoCompleteContextMenu.setY(b.getMinY() - autoCompleteContextMenu.getHeight() + 20);
                }
            } else if (autoCompleteContextMenu.isShowing()) {
                autoCompleteContextMenu.hide();
            }
        });
    }

    private void initExplorerPaneListeners() {
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

                    //TODO: Ensure filename has a valid image extension

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
                                if (img == null) {
                                    if (!target.delete())
                                        System.out.println("Tried to delete a downloaded file, as it couldn't be imported, but failed: " + target);
                                }
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

    private void initTagListViewListeners() {
        tagListView.setCellFactory(param -> {
            TagListCell c = new TagListCell();
            c.setOnContextMenuRequested(event -> {
                if (c.getItem() != null) {
                    MenuItem i1 = new MenuItem("Add to search");
                    i1.setOnAction(event1 -> {
                        if (searchTextField.getText().trim().isEmpty()) {
                            searchTextField.setText(c.getItem().getName());
                        } else {
                            searchTextField.setText(searchTextField.getText().trim() + " " + c.getItem().getName());
                        }
                        searchTextField.requestFocus();
                    });
                    MenuItem i2 = new MenuItem("Subtract from search");
                    i2.setOnAction(event1 -> {
                        if (searchTextField.getText().trim().isEmpty()) {
                            searchTextField.setText("-" + c.getItem().getName());
                        } else {
                            searchTextField.setText(searchTextField.getText().trim() + " -" + c.getItem().getName());
                        }
                        searchTextField.requestFocus();
                    });
                    MenuItem i3 = new MenuItem("Remove from selected");
                    i3.setOnAction(event1 -> imageGridView.getSelected().forEach(img -> img.removeTag(c.getItem())));
                    ContextMenu m = new ContextMenu(i1, i2, new SeparatorMenuItem(), i3);
                    m.show(c, event.getScreenX(), event.getScreenY());
                }
            });
            return c;
        });
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

    private void previewImage(ImageInfo image) {
        if (currentlyPreviewing != null) currentlyPreviewing.setTagListener(null);
        currentlyPreviewing = image;

        if (image != null) {
            image.setTagListener(() -> updateTagListViewContents(image));

            previewImageView.setImage(image.getImage());

            updateTagListViewContents(image);

            if (!image.getImage().isBackgroundLoading() || image.getImage().getProgress() == 1) {
                updateImageInfoLabel(image);
            } else {
                updateImageInfoLabel(null);
                image.getImage().progressProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue.doubleValue() == 1 && !image.getImage().isError()) updateImageInfoLabel(image);
                });
            }
        } else {
            previewImageView.setImage(null);
            tagListView.getItems().clear();
            updateImageInfoLabel(null);
        }
    }

    private void updateTagListViewContents(ImageInfo image) {
        tagListView.getItems().clear();
        image.getTags().forEach(tag -> tagListView.getItems().add(tag));
        tagListView.getItems().sort(Comparator.comparing(Tag::getName));
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
                if (tag == null) tag = new Tag(menagerie, -1, arg.substring(1));
                rules.add(new TagRule(tag, true));
            } else {
                Tag tag = menagerie.getTagByName(arg);
                if (tag == null) tag = new Tag(menagerie, -1, arg);
                rules.add(new TagRule(tag, false));
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

                    imageGridView.unselect(img);
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
        dbURLTextfield.setText(settings.getDbUrl());
        dbUserTextfield.setText(settings.getDbUser());
        dbPassTextfield.setText(settings.getDbPass());

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
            settings.setDbUrl(dbURLTextfield.getText());
            settings.setDbUser(dbUserTextfield.getText());
            settings.setDbPass(dbPassTextfield.getText());

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

    private void openTagListScreen() {
        tagListListView.getItems().clear();
        tagListListView.getItems().addAll(menagerie.getTags());
        updateTagListListViewOrder();

        explorerPane.setDisable(true);
        tagListPane.setDisable(false);
        tagListPane.setOpacity(1);
        tagListListView.requestFocus();
    }

    private void closeTagListScreen() {
        explorerPane.setDisable(false);
        tagListPane.setDisable(true);
        tagListPane.setOpacity(0);
        imageGridView.requestFocus();
    }

    private void updateTagListListViewOrder() {
        switch (tagListOrderChoiceBox.getValue()) {
            case "ID":
                tagListListView.getItems().sort(Comparator.comparingInt(Tag::getId));
                break;
            case "Frequency":
                HashMap<Integer, Integer> m = new HashMap<>();
                tagListListView.getItems().sort((o1, o2) -> {
                    int f1 = m.getOrDefault(o1.getId(), -1);
                    if (f1 == -1) {
                        f1 = o1.computeFrequency();
                        m.put(o1.getId(), f1);
                    }
                    int f2 = m.getOrDefault(o2.getId(), -1);
                    if (f2 == -1) {
                        f2 = o2.computeFrequency();
                        m.put(o2.getId(), f2);
                    }

                    return f2 - f1;
                });
                break;
            case "Name":
                tagListListView.getItems().sort(Comparator.comparing(Tag::getName));
                break;
        }
    }

    private void editTagsOfSelected(String input) {
        if (input == null || input.isEmpty() || imageGridView.getSelected().isEmpty()) return;
        lastTagString = input;

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

    private List<File> getFilesRecursive(File folder, FileFilter filter) {
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

    private void requestImportFolder() {
        DirectoryChooser dc = new DirectoryChooser();
        if (settings.getLastFolder() != null && !settings.getLastFolder().isEmpty())
            dc.setInitialDirectory(new File(settings.getLastFolder()));
        File result = dc.showDialog(rootPane.getScene().getWindow());

        if (result != null) {
            getFilesRecursive(result, Filters.IMAGE_FILTER).forEach(file -> menagerie.importImage(file, settings.isComputeMD5OnImport(), settings.isComputeHistogramOnImport(), settings.isBuildThumbnailOnImport()));
        }
    }

    private void requestImportFiles() {
        FileChooser fc = new FileChooser();
        if (settings.getLastFolder() != null && !settings.getLastFolder().isEmpty())
            fc.setInitialDirectory(new File(settings.getLastFolder()));
        fc.setSelectedExtensionFilter(Filters.IMAGE_EXTENSION_FILTER);
        List<File> results = fc.showOpenMultipleDialog(rootPane.getScene().getWindow());

        if (results != null)
            results.forEach(file -> menagerie.importImage(file, settings.isComputeMD5OnImport(), settings.isComputeHistogramOnImport(), settings.isBuildThumbnailOnImport()));
    }

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
                case O:
                    if (event.isShiftDown())
                        requestImportFolder();
                    else
                        requestImportFiles();
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
        }
    }

    public void settingsMenuItemOnAction(ActionEvent event) {
        openSettingsScreen();
        event.consume();
    }

    public void tagsMenuItemOnAction(ActionEvent event) {
        openTagListScreen();
        event.consume();
    }

    public void importFilesMenuItemOnAction(ActionEvent event) {
        requestImportFiles();

        event.consume();
    }

    public void importFolderMenuItemOnAction(ActionEvent event) {
        requestImportFolder();

        event.consume();
    }

    public void hotkeysMenuItemOnAction(ActionEvent event) {
        //TODO:
        event.consume();
    }

    public void aboutMenuItemOnAction(ActionEvent event) {
        //TODO:
        event.consume();
    }

}
