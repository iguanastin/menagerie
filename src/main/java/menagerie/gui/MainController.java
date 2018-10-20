package menagerie.gui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import menagerie.model.ImageInfo;
import menagerie.model.Menagerie;
import menagerie.model.search.IntSearchRule;

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
        List<IntSearchRule> idRules = new ArrayList<>();
        for (String arg : searchTextField.getText().split("\\s+")) {
            if (arg.startsWith("-")) {
                blacklist.add(arg.substring(1));
            } else if (arg.startsWith("id:")) {
                String temp = arg.substring(3);
                try {
                    if (temp.startsWith("<")) {
                        idRules.add(new IntSearchRule(IntSearchRule.Type.LESS_THAN, Integer.parseInt(temp.substring(1))));
                    } else if (temp.startsWith(">")) {
                        idRules.add(new IntSearchRule(IntSearchRule.Type.GREATER_THAN, Integer.parseInt(temp.substring(1))));
                    } else {
                        idRules.add(new IntSearchRule(IntSearchRule.Type.EQUAL, Integer.parseInt(temp)));
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    Main.showErrorMessage("Error", "Error converting int value for ID rule", e.getLocalizedMessage());
                }
            } else {
                required.add(arg);
            }
        }

        List<ImageInfo> images = menagerie.searchImagesStr(required, blacklist, idRules, descending);

//        List<String> md5s = new ArrayList<>();
//        images.forEach(img -> md5s.add(img.getMd5()));
//        for (int i = 0; i < images.size() - 1; i++) {
//            String h1 = md5s.get(i);
//            for (int j = i + 1; j < images.size(); j++) {
//                String h2 = md5s.get(j);
//                if (h1.equals(h2)) {
//                    System.out.println(h1 + " duplicate pair (" + images.get(i).getId() + "," + images.get(j).getId() + ")");
//                }
//            }
//        }

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
