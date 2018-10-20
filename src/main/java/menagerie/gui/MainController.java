package menagerie.gui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import menagerie.model.ImageInfo;
import menagerie.model.Menagerie;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MainController {

    public ToggleButton descendingToggleButton;
    public TextField searchTextField;
    public ImageGridView imageGridView;

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

    private void searchOnAction() {
        final boolean descending = descendingToggleButton.isSelected();

        List<String> required = new ArrayList<>();
        List<String> blacklist = new ArrayList<>();
        for (String arg : searchTextField.getText().split("\\s+")) {
            if (arg.startsWith("-")) {
                blacklist.add(arg.substring(1));
            } else {
                required.add(arg);
            }
        }

        List<ImageInfo> images = menagerie.searchImagesStr(required, blacklist, descending);

        //TODO: Apply result set to grid
        imageGridView.getItems().addAll(images);
    }

    public void searchButtonOnAction(ActionEvent event) {
        searchOnAction();
    }

    public void searchTextFieldOnAction(ActionEvent event) {
        searchOnAction();
    }

}
