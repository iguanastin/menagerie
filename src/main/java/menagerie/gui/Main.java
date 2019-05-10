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

import com.sun.jna.NativeLibrary;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import uk.co.caprica.vlcj.factory.discovery.strategy.WindowsNativeDiscoveryStrategy;
import uk.co.caprica.vlcj.support.version.LibVlcVersion;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class Main extends Application {

    private static boolean VLCJ_LOADED = false;

    public static final Logger log = Logger.getGlobal();
    private static final String logFilePath = "menagerie.log";

    private static final String SPLASH_FXML = "/fxml/splash.fxml";
    private static final String SPLASH_BACKGROUND = "/splash.jpg";
    static final String MAIN_FXML = "/fxml/main.fxml";
    static final String CSS = "/fxml/dark.css";
    static final String MAIN_TITLE = "Menagerie";
    static final String SETTINGS_PATH = "menagerie.settings";


    /**
     * Creates and shows a JFX alert.
     *
     * @param title   Title of alert
     * @param header  Header of the alert
     * @param content Content of the alert
     */
    static void showErrorMessage(String title, String header, String content) {
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
        if (!new File(logFilePath).delete()) Main.log.warning(String.format("Could not clear log file: %s", logFilePath));
        try {
            if (!new File(logFilePath).createNewFile()) Main.log.warning(String.format("Could not create new log file: %s", logFilePath));
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

        // Log some simple system info
        if (Runtime.getRuntime().maxMemory() == Long.MAX_VALUE) {
            log.info("Max Memory: No limit");
        } else {
            log.info(String.format("Max Memory: %.2fGB", Runtime.getRuntime().maxMemory() / 1024.0 / 1024.0 / 1024.0));
        }
        log.info(String.format("Processors: %d", Runtime.getRuntime().availableProcessors()));
        log.info(String.format("Operating System: %s", System.getProperty("os.name")));
        log.info(String.format("OS Version: %s", System.getProperty("os.version")));
        log.info(String.format("OS Architecture: %s", System.getProperty("os.arch")));
        log.info(String.format("Java version: %s", System.getProperty("java.version")));
        log.info(String.format("Java runtime version: %s", System.getProperty("java.runtime.version")));
        log.info(String.format("JavaFX version: %s", System.getProperty("javafx.version")));
        log.info(String.format("JavaFX runtime version: %s", System.getProperty("javafx.runtime.version")));

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
     *
     * @param stage State supplied by JFX
     */
    public void start(Stage stage) {
        log.config("Splash FXML: " + SPLASH_FXML);
        log.config("Main FXML: " + MAIN_FXML);
        log.config("Main Title: " + MAIN_TITLE);
        log.config("CSS: " + CSS);
        log.config("Settings path: " + SETTINGS_PATH);

        try {
            NativeLibrary.addSearchPath("libvlc", new WindowsNativeDiscoveryStrategy().discover());
            log.config("Loaded LibVLC Version: " + new LibVlcVersion().getVersion());

            VLCJ_LOADED = true;
        } catch (Throwable e) {
            log.log(Level.WARNING, "Error loading LibVLC", e);

            VLCJ_LOADED = false;
        }


        try {
            final List<Image> icons = getIcons();

            log.info("Loading FXML: " + SPLASH_FXML);
            FXMLLoader loader = new FXMLLoader(getClass().getResource(SPLASH_FXML));
            loader.setControllerFactory(param -> new SplashController(icons, new Image(SPLASH_BACKGROUND)));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(CSS);

            stage.initStyle(StageStyle.UNDECORATED);
            stage.setScene(scene);
            stage.getIcons().addAll(icons);
            stage.show();
        } catch (IOException e) {
            log.log(Level.SEVERE, "Error loading FXML: " + SPLASH_FXML, e);
            showErrorMessage("Error", "Unable to load FXML: " + SPLASH_FXML + ", see log for more details", e.getLocalizedMessage());
            Platform.exit();
            System.exit(1);
        }
    }

    private List<Image> getIcons() {
        List<Image> results = new ArrayList<>();
        try {
            results.add(new Image(getClass().getResourceAsStream("/icons/128.png")));
        } catch (NullPointerException ignored) {
        }
        try {
            results.add(new Image(getClass().getResourceAsStream("/icons/64.png")));
        } catch (NullPointerException ignored) {
        }
        try {
            results.add(new Image(getClass().getResourceAsStream("/icons/32.png")));
        } catch (NullPointerException ignored) {
        }
        try {
            results.add(new Image(getClass().getResourceAsStream("/icons/16.png")));
        } catch (NullPointerException ignored) {
        }

        return results;
    }

}
