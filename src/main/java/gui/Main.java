package gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    static void showErrorMessage(String title, String header, String content) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(header);
        a.setContentText(content);
        a.showAndWait();
    }

    public void start(Stage stage) {
        final String fxml = "main.fxml";
        final String title = "Menagerie";

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxml));
            Parent root = loader.load();
            stage.setScene(new Scene(root));
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
