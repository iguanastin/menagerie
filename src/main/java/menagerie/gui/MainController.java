package menagerie.gui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyEvent;
import menagerie.model.ImageInfo;
import menagerie.model.Menagerie;
import menagerie.model.Tag;
import menagerie.model.search.DateAddedRule;
import menagerie.model.search.IDRule;
import menagerie.model.search.SearchRule;
import menagerie.model.search.TagRule;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MainController {

    public ToggleButton descendingToggleButton;
    public TextField searchTextField;
    public ImageGridView imageGridView;
    public DynamicImageView previewImageView;
    public Label resultsLabel;

    private Menagerie menagerie;

    private String dbPath = "jdbc:h2:~/test", dbUser = "sa", dbPass = "";


    @FXML
    public void initialize() {
        try {
            menagerie = new Menagerie(DriverManager.getConnection(dbPath, dbUser, dbPass));
        } catch (SQLException e) {
            e.printStackTrace();
            Main.showErrorMessage("Database Error", "Error when connecting to database or verifying it", e.getLocalizedMessage());
            Platform.exit();
        }
    }

    private void searchOnAction() {
        final boolean descending = descendingToggleButton.isSelected();

        List<SearchRule> rules = new ArrayList<>();
        for (String arg : searchTextField.getText().split("\\s+")) {
            if (arg == null || arg.isEmpty()) continue;

            if (arg.startsWith("id:")) {
                String temp = arg.substring(3);
                IDRule.Type type = IDRule.Type.EQUAL_TO;
                if (temp.startsWith("<")) {
                    type = IDRule.Type.LESS_THAN;
                    temp = temp.substring(1);
                } else if (temp.startsWith(">")) {
                    type = IDRule.Type.GREATER_THAN;
                    temp = temp.substring(1);
                }
                try {
                    rules.add(new IDRule(type, Integer.parseInt(temp)));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    Main.showErrorMessage("Error", "Error converting int value for ID rule", e.getLocalizedMessage());
                }
            } else if (arg.startsWith("date:") || arg.startsWith("time:")) {
                String temp = arg.substring(5);
                DateAddedRule.Type type = DateAddedRule.Type.EQUAL_TO;
                if (temp.startsWith("<")) {
                    type = DateAddedRule.Type.LESS_THAN;
                    temp = temp.substring(1);
                } else if (temp.startsWith(">")) {
                    type = DateAddedRule.Type.GREATER_THAN;
                    temp = temp.substring(1);
                }
                try {
                    rules.add(new DateAddedRule(type, Long.parseLong(temp)));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    Main.showErrorMessage("Error", "Error converting long value for date added rule", e.getLocalizedMessage());
                }
            } else if (arg.startsWith("md5:")) {

            } else if (arg.startsWith("-")) {
                Tag tag = menagerie.getTagByName(arg.substring(1));
                if (tag == null) tag = new Tag(-1, arg.substring(1));
                rules.add(new TagRule(tag, true));
            } else {
                Tag tag = menagerie.getTagByName(arg);
                if (tag == null) tag = new Tag(-1, arg);
                rules.add(new TagRule(tag, false));
            }
        }

        List<ImageInfo> images = menagerie.searchImages(rules, descending);

//        Thread thread = new Thread(() -> {
//            long t = System.currentTimeMillis();
//            List<String> md5s = new ArrayList<>();
//            images.forEach(img -> md5s.add(img.getMD5()));
//            for (int i = 0; i < images.size() - 1; i++) {
//                String h1 = md5s.get(i);
//                for (int j = i + 1; j < images.size(); j++) {
//                    String h2 = md5s.get(j);
//                    if (h1 != null && h1.equals(h2)) {
//                        System.out.println(h1 + " duplicate pair (" + images.get(i).getId() + "," + images.get(j).getId() + ")");
//                    }
//                }
//            }
//            System.out.println((System.currentTimeMillis() - t) / 1000.0 + "s");
//            menagerie.getUpdateQueue().commit();
//        });
//        thread.setDaemon(true);
//        thread.start();

        resultsLabel.setText("Results: " + images.size());
        imageGridView.clearSelection();
        imageGridView.getItems().clear();
        imageGridView.getItems().addAll(images);
    }

    public void searchButtonOnAction(ActionEvent event) {
        searchOnAction();
    }

    public void searchTextFieldOnAction(ActionEvent event) {
        searchOnAction();
    }

}
