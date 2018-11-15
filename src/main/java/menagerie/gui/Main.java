package menagerie.gui;

import com.sun.jna.NativeLibrary;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import uk.co.caprica.vlcj.discovery.windows.DefaultWindowsNativeDiscoveryStrategy;

import java.io.IOException;

public class Main extends Application {

    public static void showErrorMessage(String title, String header, String content) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(header);
        a.setContentText(content);
        a.showAndWait();
    }

    public void start(Stage stage) {
        NativeLibrary.addSearchPath("libvlc", new DefaultWindowsNativeDiscoveryStrategy().discover());

        final String fxml = "/fxml/main.fxml";
        final String css = "/fxml/dark.css";
        final String title = "Menagerie";

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(css);
            stage.setScene(scene);
            stage.setTitle(title);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showErrorMessage("Fatal error", "Unable to load FXML: " + fxml, e.getLocalizedMessage());
            System.exit(1);
        }

    }

    public static void main(String[] args) {
        launch(args);
    }

}
