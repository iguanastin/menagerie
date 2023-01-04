package menagerie.gui.util;

import javafx.stage.Stage;
import menagerie.settings.MenagerieSettings;

public class RootPaneSettings {

  private final Stage stage;
  private final MenagerieSettings settings;

  public RootPaneSettings(Stage stage, MenagerieSettings settings) {
    this.stage = stage;
    this.settings = settings;
  }

  public void synchronize() {
    stage.setMaximized(settings.windowMaximized.getValue());
    if (settings.windowWidth.getValue() > 0) {
      stage.setWidth(settings.windowWidth.getValue());
    } else {
      settings.windowWidth.setValue((int) stage.getWidth());
    }
    if (settings.windowHeight.getValue() > 0) {
      stage.setHeight(settings.windowHeight.getValue());
    } else {
      settings.windowHeight.setValue((int) stage.getHeight());
    }
    if (settings.windowX.getValue() >= 0) {
      stage.setX(settings.windowX.getValue());
    } else {
      settings.windowX.setValue((int) stage.getX());
    }
    if (settings.windowY.getValue() >= 0) {
      stage.setY(settings.windowY.getValue());
    } else {
      settings.windowY.setValue((int) stage.getY());
    }
  }

  public void bind() {
    // TODO test simplified binding
    settings.windowMaximized.valueProperty().bind(stage.maximizedProperty());
    settings.windowWidth.valueProperty().bind(stage.widthProperty());
    settings.windowHeight.valueProperty().bind(stage.heightProperty());
    settings.windowX.valueProperty().bind(stage.xProperty());
    settings.windowY.valueProperty().bind(stage.yProperty());
  }

}
