package menagerie.gui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.stage.FileChooser;
import menagerie.model.ImageInfo;
import menagerie.model.Menagerie;
import menagerie.model.Tag;
import menagerie.model.db.DatabaseVersionUpdater;
import menagerie.model.search.DateAddedRule;
import menagerie.model.search.IDRule;
import menagerie.model.search.SearchRule;
import menagerie.model.search.TagRule;
import menagerie.model.settings.Settings;
import menagerie.util.Filters;

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

public class MainController {

    public ToggleButton descendingToggleButton;
    public TextField searchTextField;
    public ImageGridView imageGridView;
    public DynamicImageView previewImageView;
    public Label resultsLabel;
    public SplitPane rootPane;

    private Menagerie menagerie;

    private Settings settings = new Settings(new File("menagerie.settings"));

    private String dbPath = "jdbc:h2:~/test", dbUser = "sa", dbPass = "";


    @FXML
    public void initialize() {
        try {
            Connection db = DriverManager.getConnection(dbPath, dbUser, dbPass);
            if (!DatabaseVersionUpdater.upToDate(db)) {
                DatabaseVersionUpdater.updateDatabase(db);
            }

            menagerie = new Menagerie(db);
        } catch (SQLException e) {
            e.printStackTrace();
            Main.showErrorMessage("Database Error", "Error when connecting to database or verifying it", e.getLocalizedMessage());
            Platform.exit();
        }

        //Ensure two columns for grid
        imageGridView.setMinWidth(18 + (ImageInfo.THUMBNAIL_SIZE + ImageGridView.CELL_BORDER * 2 + imageGridView.getHorizontalCellSpacing() * 2) * 2);

        imageGridView.setSelectionListener(image -> {
            previewImageView.setImage(image.getImage());
        });
        imageGridView.setOnDragOver(event -> {
            if (event.getGestureSource() == null && (event.getDragboard().hasFiles() || event.getDragboard().hasUrl())) {
                event.acceptTransferModes(TransferMode.ANY);
            }
            event.consume();
        });
        rootPane.setOnDragDropped(event -> {
            List<File> files = event.getDragboard().getFiles();
            String url = event.getDragboard().getUrl();

            if (files != null && !files.isEmpty()) {
                files.forEach(file -> menagerie.importImage(file, settings.isComputeMD5OnImport(), settings.isComputeHistogramOnImport()));
            } else if (url != null && !url.isEmpty()) {
                String folder = settings.getLastFolder();
                String filename = URI.create(url).getPath().replaceAll("^.*/", "");
                if (!settings.isAutoImportFromWeb() || folder == null || !new File(folder).exists()) {
                    FileChooser fc = new FileChooser();
                    fc.setTitle("Save file from web");
                    fc.setInitialFileName(filename);
                    fc.setSelectedExtensionFilter(Filters.IMAGE_EXTENSION_FILTER);
                    File result = fc.showSaveDialog(rootPane.getScene().getWindow());

                    settings.setLastFolder(result.getParent());
                    folder = settings.getLastFolder();
                    filename = result.getName();
                }

                if (!folder.endsWith("\\") && !folder.endsWith("/")) folder = folder + "/";
                File target = new File(folder + filename);

                new Thread(() -> {
                    try {
                        downloadAndSaveFile(url, target);
                        Platform.runLater(() -> {
                            if (!menagerie.importImage(target, settings.isComputeMD5OnImport(), settings.isComputeHistogramOnImport())) target.delete();
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            }
            event.consume();
        });
    }

    private void searchOnAction() {
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

        List<ImageInfo> images = menagerie.searchImages(rules, descending);

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

        resultsLabel.setText("Results: " + images.size());
        imageGridView.clearSelection();
        imageGridView.getItems().clear();
        imageGridView.getItems().addAll(images);
    }

    public void searchButtonOnAction(ActionEvent event) {
        searchOnAction();
        imageGridView.requestFocus();
    }

    public void searchTextFieldOnAction(ActionEvent event) {
        searchOnAction();
        imageGridView.requestFocus();
    }

    public void rootPaneOnKeyPressed(KeyEvent event) {
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
    }

}
