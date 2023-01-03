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
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import menagerie.util.Util;
import uk.co.caprica.vlcj.factory.discovery.strategy.LinuxNativeDiscoveryStrategy;
import uk.co.caprica.vlcj.factory.discovery.strategy.OsxNativeDiscoveryStrategy;
import uk.co.caprica.vlcj.factory.discovery.strategy.WindowsNativeDiscoveryStrategy;
import uk.co.caprica.vlcj.support.version.LibVlcVersion;

public class Main extends Application {

  private static boolean vlcjLoaded = false;

  static final Logger MENAGERIE_LOGGER = Logger.getLogger("menagerie");
  private static final String LOG_FILE_PATH = "menagerie.log";
  private static final int LOG_FILE_SIZE_LIMIT = 1048576; // 1 MB

  private static final String SPLASH_FXML = "/fxml/splash.fxml";
  private static final String SPLASH_BACKGROUND = "/misc/splash.jpg";
  static final String MAIN_FXML = "/fxml/main.fxml";
  static final String CSS = "/fxml/dark.css";
  static final String MAIN_TITLE = "Menagerie";
  public static final String SETTINGS_PATH = "menagerie.settings";


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
    initMenagerieLogger();

    MENAGERIE_LOGGER.info("\n" +
                          "===============================================================================================================\n" +
                          "========================================== Starting Menagerie =================================================\n" +
                          "===============================================================================================================");

    // Log some simple system info
    if (Runtime.getRuntime().maxMemory() == Long.MAX_VALUE) {
      MENAGERIE_LOGGER.info("Max Memory: No limit");
    } else {
      MENAGERIE_LOGGER.info(
          "Max Memory: " + Util.bytesToPrettyString(Runtime.getRuntime().maxMemory()));
    }
    MENAGERIE_LOGGER.info(
        String.format("Processors: %d", Runtime.getRuntime().availableProcessors()));
    MENAGERIE_LOGGER.info(String.format("Operating System: %s", System.getProperty("os.name")));
    MENAGERIE_LOGGER.info(String.format("OS Version: %s", System.getProperty("os.version")));
    MENAGERIE_LOGGER.info(String.format("OS Architecture: %s", System.getProperty("os.arch")));
    MENAGERIE_LOGGER.info(String.format("Java version: %s", System.getProperty("java.version")));
    MENAGERIE_LOGGER.info(
        String.format("Java runtime version: %s", System.getProperty("java.runtime.version")));
    MENAGERIE_LOGGER.info(
        String.format("JavaFX version: %s", System.getProperty("javafx.version")));
    MENAGERIE_LOGGER.info(
        String.format("JavaFX runtime version: %s", System.getProperty("javafx.runtime.version")));

    // Launch application
    MENAGERIE_LOGGER.info("Starting JFX Application...");
    launch(args);
  }

  private static void initMenagerieLogger() {
    MENAGERIE_LOGGER.setLevel(Level.ALL); // Default log level

    // Init logger handler
    Thread.setDefaultUncaughtExceptionHandler(
        (t, e) -> MENAGERIE_LOGGER.log(Level.SEVERE, "Uncaught exception in thread: " + t, e));
    MENAGERIE_LOGGER.setUseParentHandlers(false);
    Formatter formatter = new Formatter() {
      final DateFormat dateFormat =
          DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);

      @Override
      public String format(LogRecord record) {
        StringBuilder str = new StringBuilder("(");
        str.append(dateFormat.format(record.getMillis())).append(") ");
        str.append(record.getSourceClassName()).append("#").append(record.getSourceMethodName());
        str.append(" [").append(record.getLevel()).append("]: ").append(record.getMessage());

        if (record.getThrown() != null) {
          str.append("\n").append(record.getThrown().toString());
          for (StackTraceElement element : record.getThrown().getStackTrace()) {
            str.append("\n    at ").append(element.toString());
          }
        }
        return str.toString();
      }
    };
    MENAGERIE_LOGGER.addHandler(new Handler() {
      @Override
      public void publish(LogRecord record) {
        PrintStream s = System.out;
        if (record.getLevel() == Level.SEVERE) {
          s = System.err;
        }
        s.println(formatter.format(record));
      }

      @Override
      public void flush() {
      }

      @Override
      public void close() throws SecurityException {
      }
    });
    try {
      FileHandler fileHandler = new FileHandler(LOG_FILE_PATH, LOG_FILE_SIZE_LIMIT, 1, true);
      fileHandler.setFormatter(new Formatter() {
        @Override
        public String format(LogRecord logRecord) {
          return formatter.format(logRecord) + System.lineSeparator();
        }
      });
      MENAGERIE_LOGGER.addHandler(fileHandler);
    } catch (IOException e) {
      MENAGERIE_LOGGER.log(Level.SEVERE, "Failed to create log file handler: " + LOG_FILE_PATH, e);
    }
  }

  /**
   * Checks flag that is set during FX Application launch.
   *
   * @return True if VLCJ native libraries were found and loaded successfully, false otherwise.
   */
  public static boolean isVlcjLoaded() {
    return vlcjLoaded;
  }

  /**
   * JavaFX application start method, called via launch() in main()
   *
   * @param stage State supplied by JFX
   */
  public void start(Stage stage) {
    MENAGERIE_LOGGER.config("Splash FXML: " + SPLASH_FXML);
    MENAGERIE_LOGGER.config("Main FXML: " + MAIN_FXML);
    MENAGERIE_LOGGER.config("Main Title: " + MAIN_TITLE);
    MENAGERIE_LOGGER.config("CSS: " + CSS);
    MENAGERIE_LOGGER.config("Settings path: " + SETTINGS_PATH);

    try {
      final List<Image> icons = getIcons();

      MENAGERIE_LOGGER.info("Loading FXML: " + SPLASH_FXML);
      FXMLLoader loader = new FXMLLoader(getClass().getResource(SPLASH_FXML));
      loader.setControllerFactory(
          param -> new SplashController(icons, new Image(SPLASH_BACKGROUND)));
      Parent root = loader.load();
      Scene scene = new Scene(root);
      scene.getStylesheets().add(CSS);

      stage.initStyle(StageStyle.UNDECORATED);
      stage.setScene(scene);
      stage.getIcons().addAll(icons);
      stage.show();
    } catch (IOException e) {
      MENAGERIE_LOGGER.log(Level.SEVERE, "Error loading FXML: " + SPLASH_FXML, e);
      showErrorMessage("Error",
          "Unable to load FXML: " + SPLASH_FXML + ", see log for more details",
          e.getLocalizedMessage());
      Platform.exit();
      System.exit(1);
    }
  }

  static void loadVLCJ(String path) {
    List<String> paths = new ArrayList<>();
    if (path != null) {
      paths.add(path);
    }
    path = new WindowsNativeDiscoveryStrategy().discover();
    if (path != null) {
      paths.add(path);
    }
    path = new LinuxNativeDiscoveryStrategy().discover();
    if (path != null) {
      paths.add(path);
    }
    path = new OsxNativeDiscoveryStrategy().discover();
    if (path != null) {
      paths.add(path);
    }

    for (String p : paths) {
      NativeLibrary.addSearchPath("libvlc", p);
      try {
        NativeLibrary.getInstance("libvlc");
        MENAGERIE_LOGGER.config("Loaded LibVLC Version: " + new LibVlcVersion().getVersion());
        vlcjLoaded = true;
        break;
      } catch (Throwable t) {
        MENAGERIE_LOGGER.log(Level.WARNING, "Failed to load libvlc with path: " + p, t);
      }
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
