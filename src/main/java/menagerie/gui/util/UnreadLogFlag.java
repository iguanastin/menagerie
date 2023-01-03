package menagerie.gui.util;

import javafx.beans.property.BooleanPropertyBase;
import javafx.css.PseudoClass;
import javafx.scene.control.Button;

public class UnreadLogFlag extends BooleanPropertyBase {

  private final Button logButton;
  private final PseudoClass pseudoClass;
  private final String name;

  public UnreadLogFlag(Button logButton, PseudoClass pseudoClass, String name) {
    this.logButton = logButton;
    this.pseudoClass = pseudoClass;
    this.name = name;
  }

  @Override
  protected void invalidated() {
    logButton.pseudoClassStateChanged(pseudoClass, get());
  }

  @Override
  public Object getBean() {
    return logButton.getClass();
  }

  @Override
  public String getName() {
    return name;
  }
}
