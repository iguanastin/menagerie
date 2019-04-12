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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class Main extends Application {

    private static boolean VLCJ_LOADED = false;

    public static final Logger log = Logger.getGlobal();
    private static final String logFilePath = "menagerie.log";


    /**
     * Creates and shows a JFX alert.
     *
     * @param title   Title of alert
     * @param header  Header of the alert
     * @param content Content of the alert
     */
    public static void showErrorMessage(String title, String header, String content) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(header);
        a.setContentText(content);
        a.showAndWait();
    }

    public static void main(String[] args) {
        log.setLevel(Level.ALL); // Default log level

        // Set log level to severe only with arg
        for (String arg : args) {
            if (arg.equalsIgnoreCase("-quiet")) {
                log.setLevel(Level.SEVERE);
            }
        }

        // Clear log file
        if (!new File(logFilePath).delete())
            Main.log.warning(String.format("Could not clear log file: %s", logFilePath));
        try {
            if (!new File(logFilePath).createNewFile())
                Main.log.warning(String.format("Could not create new log file: %s", logFilePath));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Init logger handler
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> Main.log.log(Level.SEVERE, "Uncaught exception in thread: " + t, e));
        log.setUseParentHandlers(false);
        log.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                StringBuilder str = new StringBuilder(new Date(record.getMillis()).toString());
                str.append(" [").append(record.getLevel()).append("]: ").append(record.getMessage());

                // Print to sout/serr
                PrintStream s = System.out;
                if (record.getLevel() == Level.SEVERE) s = System.err;
                s.println(str.toString());
                if (record.getThrown() != null) record.getThrown().printStackTrace();

                // Print to file
                if (record.getThrown() != null) {
                    str.append("\n").append(record.getThrown().toString());
                    for (StackTraceElement element : record.getThrown().getStackTrace()) {
                        str.append("\n    at ").append(element.toString());
                    }
                }
                str.append("\n");
                try {
                    Files.write(Paths.get(logFilePath), str.toString().getBytes(), StandardOpenOption.APPEND);
                } catch (IOException e) {
                    System.err.println(String.format("Failed to write log to file: %s", logFilePath));
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() throws SecurityException {
            }
        });

        // Launch application
        log.info("Starting JFX Application...");
        launch(args);
    }

    /**
     * Checks flag that is set during FX Application launch.
     *
     * @return True if VLCJ native libraries were found and loaded successfully, false otherwise.
     */
    public static boolean isVlcjLoaded() {
        return VLCJ_LOADED;
    }

    /**
     * JavaFX application start method, called via launch() in main()
     * @param stage State supplied by JFX
     */
    public void start(Stage stage) {
        try {
            NativeLibrary.addSearchPath("libvlc", new DefaultWindowsNativeDiscoveryStrategy().discover());
            log.config("Loaded LibVLC Version: " + LibVlcVersion.getVersion());

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
            log.info(String.format("Loading FXML: %s", splash));
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
                log.info(String.format("Loading FXML: %s", fxml));
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

}
