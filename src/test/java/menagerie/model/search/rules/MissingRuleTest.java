package menagerie.model.search.rules;

import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.MediaItem;
import menagerie.model.menagerie.histogram.ImageHistogram;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MissingRuleTest {

  private MediaItem createMediaItem(File file, String md5, ImageHistogram histogram) {
    return new MediaItem(null, 1, 1, 1, false,
        new GroupItem(null, 2, 1, "group"), file, md5, histogram);
  }

  @Test
  public void testMissingMD5() {
    MissingRule rule = new MissingRule(MissingRule.Type.MD5, false);
    final String md5 = "f9e2cb7b1b1436182d16b29d81b6bf78";

    MediaItem item1 = createMediaItem(null, md5, null);
    assertFalse(rule.accept(item1));

    MediaItem item2 = createMediaItem(null, null, null);
    assertTrue(rule.accept(item2));
  }

  @Test
  public void testMissingFile() {
    MissingRule rule = new MissingRule(MissingRule.Type.FILE, false);
    File existingFile = mock(File.class);
    when(existingFile.exists()).thenReturn(true);

    File missingFile = mock(File.class);
    when(missingFile.exists()).thenReturn(false);

    MediaItem item1 = createMediaItem(existingFile, null, null);
    assertFalse(rule.accept(item1));

    MediaItem item2 = createMediaItem(null, null, null);
    assertTrue(rule.accept(item2));

    MediaItem item3 = createMediaItem(missingFile, null, null);
    assertTrue(rule.accept(item3));
  }

  @Test
  public void testMissingHistogram() {
    MissingRule rule = new MissingRule(MissingRule.Type.HISTOGRAM, false);

    MediaItem item1 = createMediaItem(null, null, mock(ImageHistogram.class));
    assertFalse(rule.accept(item1));

    MediaItem item2 = createMediaItem(null, null, null);
    assertTrue(rule.accept(item2));
  }
}

