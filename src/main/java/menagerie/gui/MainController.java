package menagerie.gui;

import javafx.fxml.FXML;
import menagerie.db.DatabaseManager;

import java.sql.SQLException;

public class MainController {

    private DatabaseManager db;

    private String dbPath = "jdbc:h2:~/test", dbUser = "sa", dbPass = "";


    @FXML
    public void initialize() {
        try {
            db = new DatabaseManager(dbPath, dbUser, dbPass);
        } catch (SQLException e) {
            e.printStackTrace();
            Main.showErrorMessage("Database Error", "Driver failed to connect to database: " + dbPath, e.getLocalizedMessage());
        }
    }

}
