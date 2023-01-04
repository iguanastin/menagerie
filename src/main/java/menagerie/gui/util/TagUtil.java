package menagerie.gui.util;

import java.util.ArrayList;
import java.util.List;
import menagerie.model.menagerie.Tag;

public class TagUtil {

  private TagUtil() {
  }

  public static List<String> getTop8Tags(String prefix, boolean negative, List<Tag> tags) {
    List<String> results = new ArrayList<>();
    tags.sort((o1, o2) -> o2.getFrequency() - o1.getFrequency());
    for (Tag tag : tags) {
      if (tag.getName().toLowerCase().startsWith(prefix)) {
        if (negative) {
          results.add("-" + tag.getName());
        } else {
          results.add(tag.getName());
        }
      }

      if (results.size() >= 8) {
        break;
      }
    }
    return results;
  }
}
