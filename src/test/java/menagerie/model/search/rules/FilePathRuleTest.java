package menagerie.model.search.rules;

import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilePathRuleTest {

  private MediaItem createMediaItem(String pathname) {
    return new MediaItem(null, 1, 1, new File(pathname));
  }

  @Test
  void testAccept() {
    // Create a media item with file path "C:\test\test.jpg"
    MediaItem mediaItem = createMediaItem("C:\\test\\test.jpg");

    // Create a FilePathRule that searches for "test" in the file path
    FilePathRule rule = new FilePathRule("test", false);

    // The rule should accept the media item because its file path contains "test"
    assertTrue(rule.accept(mediaItem));

    // Create a FilePathRule that searches for "abc" in the file path
    rule = new FilePathRule("abc", false);

    // The rule should not accept the media item because its file path does not contain "abc"
    assertFalse(rule.accept(mediaItem));
  }

  @Test
  void testNonMediaItem() {
    Item testItem = new TestItemBuilder().build();
    FilePathRule rule = new FilePathRule("test", false);
    // The rule should reject the item because it's not a MediaItem
    assertFalse(rule.accept(testItem));
  }
}
