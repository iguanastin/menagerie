package menagerie.gui;

import com.sun.jna.NativeLibrary;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import uk.co.caprica.vlcj.discovery.windows.DefaultWindowsNativeDiscoveryStrategy;
import uk.co.caprica.vlcj.version.LibVlcVersion;

import java.io.IOException;
import java.io.PrintStream;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class Main extends Application {

    private static boolean VLCJ_LOADED = false;

    public static final Logger log = Logger.getGlobal();


    public static void showErrorMessage(String title, String header, String content) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(header);
        a.setContentText(content);
        a.showAndWait();
    }

    public void start(Stage stage) {
        try {
            NativeLibrary.addSearchPath("libvlc", new DefaultWindowsNativeDiscoveryStrategy().discover());
            log.config("LibVLC Version: " + LibVlcVersion.getVersion());

            VLCJ_LOADED = true;
        } catch (Throwable e) {
            log.log(Level.WARNING, "Error loading vlcj", e);

            VLCJ_LOADED = false;
        }

        final String splash = "/fxml/splash.fxml";
        final String fxml = "/fxml/main.fxml";
        final String css = "/fxml/dark.css";
        final String title = "Menagerie";

        try {
            Parent root = FXMLLoader.load(getClass().getResource(splash));
            Scene scene = new Scene(root);
            scene.getStylesheets().add(css);

            stage.initStyle(StageStyle.UNDECORATED);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            log.log(Level.SEVERE, "Error loading FXML: " + splash, e);
            showErrorMessage("Error", "Unable to load FXML: " + splash, e.getLocalizedMessage());
        }

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
                Parent root = loader.load();
                Scene scene = new Scene(root);
                scene.getStylesheets().add(css);

                Stage newStage = new Stage();
                newStage.setScene(scene);
                newStage.setTitle(title);
                newStage.show();
                stage.close();
            } catch (IOException e) {
                log.log(Level.SEVERE, "Failed to load FXML: " + fxml, e);
                System.exit(1);
            }
        });

    }

    public static void main(String[] args) {
        log.setLevel(Level.ALL);

        // Init logger handler
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> Main.log.log(Level.SEVERE, "Uncaught exception in thread: " + t, e));
        DateTimeFormatter dtf = DateTimeFormatter.ISO_INSTANT;
        log.setUseParentHandlers(false);
        log.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                PrintStream s = System.out;
                if (record.getLevel() == Level.SEVERE) s = System.err;
                s.println(dtf.format(Instant.ofEpochMilli(record.getMillis())) + " [" + record.getLevel() + "]: " + record.getMessage());
                if (record.getThrown() != null) record.getThrown().printStackTrace();
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() throws SecurityException {
            }
        });

        // Launch application
        launch(args);
    }

    public static boolean isVlcjLoaded() {
        return VLCJ_LOADED;
    }

}
