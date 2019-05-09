package menagerie.gui.taglist;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import menagerie.util.listeners.ObjectListener;

public class SimpleCSSColorPicker extends HBox {

    private ObjectListener<String> colorPickedListener = null;


    public SimpleCSSColorPicker(String[] colors) {
        setSpacing(5);
        setPadding(new Insets(5));

        for (String css : colors) {
            Button b = new Button();
            b.prefWidthProperty().bind(b.prefHeightProperty());
            b.setOnAction(event -> confirmedColor(css));
            b.setStyle(String.format("-fx-base: %s;", css));
            getChildren().add(b);
        }
        Button b = new Button("Default");
        b.setOnAction(event -> confirmedColor(null));
        getChildren().add(b);

        TextField textField = new TextField();
        textField.setPromptText("Custom");
        textField.setOnAction(event -> confirmedColor(textField.getText()));
        getChildren().add(textField);
    }

    public SimpleCSSColorPicker(String[] colors, ObjectListener<String> colorPickedListener) {
        this(colors);
        setColorPickedListener(colorPickedListener);
    }

    void setColorPickedListener(ObjectListener<String> colorPickedListener) {
        this.colorPickedListener = colorPickedListener;
    }

    private void confirmedColor(String css) {
        if (colorPickedListener != null) colorPickedListener.pass(css);
    }

}
