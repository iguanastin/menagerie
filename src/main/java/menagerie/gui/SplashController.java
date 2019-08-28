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

package menagerie.gui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import menagerie.model.menagerie.Menagerie;
import menagerie.model.menagerie.db.DatabaseManager;
import menagerie.model.menagerie.db.DatabaseVersionUpdater;
import menagerie.model.menagerie.db.MenagerieDatabaseLoadListener;
import menagerie.settings.MenagerieSettings;
import menagerie.settings.OldSettings;
import menagerie.settings.SettingsException;
import org.json.JSONException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;

public class SplashController {

    private static final int PROGRESS_UPDATE_INTERVAL = 16;

    public StackPane rootPane;
    public ImageView backgroundImageView;
    public Label titleLabel;
    public Label statusLabel;
    public ProgressBar progressBar;

    private final List<Image> icons;
    private final Image splashBackground;

    private long lastProgressUpdate = 0;


    public SplashController(List<Image> icons, Image splashBackground) {
        this.icons = icons;
        this.splashBackground = splashBackground;
    }

    @FXML
    public void initialize() {
        backgroundImageView.setImage(splashBackground);

        // Set graphic
        titleLabel.setGraphicTextGap(10);
        for (Image icon : icons) {
            if (icon.getWidth() == 64) {
                titleLabel.setGraphic(new ImageView(icon));
            }
        }

        // ------------------------------------------ Startup thread ---------------------------------------------------
        new Thread(() -> {
            final MenagerieSettings settings = new MenagerieSettings();
            try {
                settings.load(new File(Main.SETTINGS_PATH));
            } catch (FileNotFoundException e) {
                Main.log.warning("Settings file does not exist");
            } catch (IOException e) {
                Main.log.log(Level.SEVERE, "Error reading settings file", e);
            } catch (JSONException e) {
                Main.log.warning("JSON error, attempting to read old style settings");
                settings.loadFrom(new OldSettings(new File(Main.SETTINGS_PATH)));
            } catch (SettingsException e) {
                Main.log.log(Level.SEVERE, "Invalid settings file", e);
            }

            // --------------------------------------------- Load VLCJ -------------------------------------------------
            String vlcj = settings.vlcFolder.getValue();
            if (vlcj != null && vlcj.isEmpty()) vlcj = null;
            Main.loadVLCJ(vlcj);

            // ----------------------------------------- Back up database ----------------------------------------------
            if (settings.dbBackup.getValue()) {
                Platform.runLater(() -> statusLabel.setText("Backing up database..."));
                try {
                    backupDatabase(settings.dbUrl.getValue());
                } catch (IOException e) {
                    Main.log.log(Level.SEVERE, "Failed to backup database. Unexpected error occurred.", e);
                    Main.log.info("DB URL: " + settings.dbUrl.getValue());

                    Platform.runLater(() -> {
                        Main.showErrorMessage("Error while backing up database", "See log for more details", e.getLocalizedMessage());
                        Platform.exit();
                        System.exit(1);
                    });
                    return;
                }
            }

            // ---------------------------------------- Connect to database --------------------------------------------
            Platform.runLater(() -> statusLabel.setText("Connecting to database: " + settings.dbUrl.getValue() + "..."));
            Connection database;
            try {
                database = DriverManager.getConnection("jdbc:h2:" + settings.dbUrl.getValue(), settings.dbUser.getValue(), settings.dbPass.getValue());
            } catch (SQLException e) {
                Main.log.log(Level.SEVERE, "Error connecting to database: " + settings.dbUrl.getValue(), e);
                Platform.runLater(() -> {
                    Main.showErrorMessage("Error connecting to database", "Database is most likely open in another application", e.getLocalizedMessage());
                    Platform.exit();
                    System.exit(1);
                });
                return;
            }

            // -------------------------------------- Verify/upgrade database ------------------------------------------
            Platform.runLater(() -> statusLabel.setText("Verifying and upgrading database: " + settings.dbUrl.getValue() + "..."));
            try {
                DatabaseVersionUpdater.updateDatabase(database);
            } catch (SQLException e) {
                Main.log.log(Level.SEVERE, "Unexpected error while attempting to verify or upgrade database", e);
                Platform.runLater(() -> {
                    Main.showErrorMessage("Error while verifying or upgrading database", "See log for more details", e.getLocalizedMessage());
                    Platform.exit();
                    System.exit(1);
                });
                return;
            }

            // ----------------------------------- Connect database manager --------------------------------------------
            Platform.runLater(() -> statusLabel.setText("Plugging in database manager..."));
            DatabaseManager databaseManager;
            try {
                databaseManager = new DatabaseManager(database);
            } catch (SQLException e) {
                Main.log.log(Level.SEVERE, "Unexpected error while connecting database manager to database", e);
                Platform.runLater(() -> {
                    Main.showErrorMessage("Error while plugging manager into database", "See log for more details", e.getLocalizedMessage());
                    Platform.exit();
                    System.exit(1);
                });
                return;
            }
            databaseManager.setLoadListener(new MenagerieDatabaseLoadListener() {
                @Override
                public void startedItemLoading(int total) {
                    Platform.runLater(() -> statusLabel.setText("Loading " + total + " items..."));
                }

                @Override
                public void gettingItemList() {
                    Platform.runLater(() -> {
                        statusLabel.setText("Getting list of items from database...");
                        progressBar.setProgress(-1);
                    });
                }

                @Override
                public void itemsLoading(int count, int total) {
                    long time = System.currentTimeMillis();
                    if (time - lastProgressUpdate > PROGRESS_UPDATE_INTERVAL) {
                        lastProgressUpdate = time;
                        Platform.runLater(() -> progressBar.setProgress((double) count / total));
                    }
                }

                @Override
                public void startTagLoading(int total) {
                    Platform.runLater(() -> statusLabel.setText("Loading " + total + " tags..."));
                }

                @Override
                public void tagsLoading(int count, int total) {
                    long time = System.currentTimeMillis();
                    if (time - lastProgressUpdate > PROGRESS_UPDATE_INTERVAL) {
                        lastProgressUpdate = time;
                        Platform.runLater(() -> progressBar.setProgress((double) count / total));
                    }
                }

                @Override
                public void gettingNonDupeList() {
                    Platform.runLater(() -> {
                        statusLabel.setText("Getting non-duplicates list from database...");
                        progressBar.setProgress(-1);
                    });
                }

                @Override
                public void startNonDupeLoading(int total) {
                    Platform.runLater(() -> statusLabel.setText("Loading " + total + " non-duplicates..."));
                }

                @Override
                public void nonDupeLoading(int count, int total) {
                    long time = System.currentTimeMillis();
                    if (time - lastProgressUpdate > PROGRESS_UPDATE_INTERVAL) {
                        lastProgressUpdate = time;
                        Platform.runLater(() -> progressBar.setProgress((double) count / total));
                    }
                }
            });
            databaseManager.setDaemon(true);
            databaseManager.start();

            // ------------------------------------ Construct Menagerie ------------------------------------------------
            Menagerie menagerie;
            try {
                menagerie = new Menagerie(databaseManager);
            } catch (SQLException e) {
                Main.log.log(Level.SEVERE, "Error initializing Menagerie", e);
                Platform.runLater(() -> {
                    Main.showErrorMessage("Error while loading data into Menagerie", "See log for more details", e.getLocalizedMessage());
                    Platform.exit();
                    System.exit(1);
                });
                return;
            }

            // --------------------------------- Open main application window ------------------------------------------
            final Menagerie finalMenagerie = menagerie;
            Platform.runLater(() -> openMain(finalMenagerie, settings));

        }, "Startup Thread").start();
    }

    private void openMain(Menagerie menagerie, MenagerieSettings settings) {
        try {
            Main.log.info(String.format("Loading FXML: %s", Main.MAIN_FXML));
            FXMLLoader loader = new FXMLLoader(getClass().getResource(Main.MAIN_FXML));
            loader.setControllerFactory(param -> new MainController(menagerie, settings));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(Main.CSS);

            Stage newStage = new Stage();
            newStage.setScene(scene);
            newStage.setTitle(Main.MAIN_TITLE);
            newStage.getIcons().addAll(icons);
            newStage.show();

            // Close this splash screen
            ((Stage) rootPane.getScene().getWindow()).close();
        } catch (IOException e) {
            Main.log.log(Level.SEVERE, "Failed to load FXML: " + Main.MAIN_FXML, e);
            Main.showErrorMessage("Error on initialization", "Unknown error occurred while loading main FXML. See log for more info.", e.getLocalizedMessage());
            System.exit(1);
        }
    }

    /**
     * Attempts to back up the database file as specified in the settings object
     *
     * @throws IOException When copy fails.
     */
    private static void backupDatabase(String databaseURL) throws IOException {
        File dbFile = DatabaseManager.resolveDatabaseFile(databaseURL);

        if (dbFile.exists()) {
            Main.log.info("Backing up database at: " + dbFile);
            File backupFile = new File(dbFile.getAbsolutePath() + ".bak");
            Files.copy(dbFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Main.log.info("Successfully backed up database to: " + backupFile);
        } else {
            Main.log.warning("Cannot backup nonexistent database file at: " + dbFile);
        }
    }

}
