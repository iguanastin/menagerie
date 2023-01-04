package menagerie.gui.util;

import static java.util.Objects.requireNonNull;

import javafx.scene.image.Image;

public class Icons {

  private Icons() {
  }

  private static final Icons INSTANCE = new Icons();

  public static Icons getInstance() {
    return INSTANCE;
  }

  private final Image descendingIcon =
      getImage("/misc/descending.png");

  private final Image openGroupsIcon =
      getImage("/misc/opengroups.png");

  private final Image shuffleIcon =
      getImage("/misc/shuffle.png");

  private Image getImage(String name) {
    final var resource = requireNonNull(getClass().getResource(name),
        "resource %s not found".formatted(name));
    return new Image(resource.toString());
  }

  public Image getDescendingIcon() {
    return descendingIcon;
  }

  public Image getOpenGroupsIcon() {
    return openGroupsIcon;
  }

  public Image getShuffleIcon() {
    return shuffleIcon;
  }

}
