package menagerie.gui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import menagerie.model.Settings;
import menagerie.model.menagerie.Menagerie;
import menagerie.model.menagerie.db.DatabaseManager;
import menagerie.model.menagerie.db.DatabaseVersionUpdater;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;

public class SplashController {

    public StackPane rootPane;
    public ImageView backgroundImageView;
    public Label titleLabel;
    public Label statusLabel;

    private final List<Image> icons;
    private final Image splashBackground;


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
            final Settings settings = new Settings(new File(Main.SETTINGS_PATH));

            // ----------------------------------------- Back up database ----------------------------------------------
            if (settings.getBoolean(Settings.Key.BACKUP_DATABASE)) {
                Platform.runLater(() -> statusLabel.setText("Backing up database..."));
                try {
                    backupDatabase(settings.getString(Settings.Key.DATABASE_URL));
                } catch (IOException e) {
                    Main.log.log(Level.SEVERE, "Failed to backup database. Unexpected error occured.", e);
                    Main.log.info("DB URL: " + settings.getString(Settings.Key.DATABASE_URL));
                    Main.showErrorMessage("Error while backing up database", "See log for more details", e.getLocalizedMessage());
                    Platform.exit();
                    System.exit(1);
                }
            }

            // ---------------------------------------- Connect to database --------------------------------------------
            Platform.runLater(() -> statusLabel.setText("Connecting to database: " + settings.getString(Settings.Key.DATABASE_URL) + "..."));
            Connection database = null;
            try {
                database = DriverManager.getConnection("jdbc:h2:" + settings.getString(Settings.Key.DATABASE_URL), settings.getString(Settings.Key.DATABASE_USER), settings.getString(Settings.Key.DATABASE_PASSWORD));
            } catch (SQLException e) {
                Main.log.log(Level.SEVERE, "Error connecting to database: " + settings.getString(Settings.Key.DATABASE_URL), e);
                Main.showErrorMessage("Error connecting to database", "Database is most likely open in another application", e.getLocalizedMessage());
                Platform.exit();
                System.exit(1);
            }

            // -------------------------------------- Verify/upgrade database ------------------------------------------
            Platform.runLater(() -> statusLabel.setText("Verifying and upgrading database: " + settings.getString(Settings.Key.DATABASE_URL) + "..."));
            try {
                DatabaseVersionUpdater.updateDatabase(database);
            } catch (SQLException e) {
                Main.log.log(Level.SEVERE, "Unexpected error while attempting to verify or upgrade database", e);
                Main.showErrorMessage("Error while verifying or upgrading database", "See log for more details", e.getLocalizedMessage());
                Platform.exit();
                System.exit(1);
            }

            // ----------------------------------- Connect database manager --------------------------------------------
            Platform.runLater(() -> statusLabel.setText("Plugging in database manager..."));
            DatabaseManager databaseManager = null;
            try {
                databaseManager = new DatabaseManager(database);
            } catch (SQLException e) {
                Main.log.log(Level.SEVERE, "Unexpected error while connecting database manager to database", e);
                Main.showErrorMessage("Error while plugging manager into database", "See log for more details", e.getLocalizedMessage());
                Platform.exit();
                System.exit(1);
            }
            databaseManager.setDaemon(true);
            databaseManager.start();

            // ------------------------------------ Construct Menagerie ------------------------------------------------
            Platform.runLater(() -> statusLabel.setText("Loading data from database..."));
            Menagerie menagerie = null;
            try {
                menagerie = new Menagerie(databaseManager);
            } catch (SQLException e) {
                Main.log.log(Level.SEVERE, "Error initializing Menagerie", e);
                Main.showErrorMessage("Error while loading data into Menagerie", "See log for more details", e.getLocalizedMessage());
                Platform.exit();
                System.exit(1);
            }

            // --------------------------------- Open main application window ------------------------------------------
            final Menagerie finalMenagerie = menagerie;
            Platform.runLater(() -> openMain(finalMenagerie, settings));

        }).start();
    }

    private void openMain(Menagerie menagerie, Settings settings) {
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
