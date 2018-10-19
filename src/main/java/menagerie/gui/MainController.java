package menagerie.gui;

import javafx.fxml.FXML;
import menagerie.model.Menagerie;

import java.sql.DriverManager;
import java.sql.SQLException;

public class MainController {

    private Menagerie menagerie;

    private String dbPath = "jdbc:h2:~/test", dbUser = "sa", dbPass = "";


    @FXML
    public void initialize() {
        try {
            menagerie = new Menagerie(DriverManager.getConnection(dbPath, dbUser, dbPass));
        } catch (SQLException e) {
            e.printStackTrace();
            Main.showErrorMessage("Database Error", "Error when connecting to database or verifying it", e.getLocalizedMessage());
        }
    }

}
