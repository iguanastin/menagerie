/*
 MIT License

 Copyright (c) 2019. Austin Thompson

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */

package menagerie.model.menagerie;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import menagerie.util.Filters;
import org.junit.jupiter.api.Test;

class MediaItemTests {

  private static final File WHITE_IMAGE_FILE = new File("target/test-classes/white.png");
  private static final File BLACK_IMAGE_FILE = new File("target/test-classes/black.png");
  private static final String WHITE_IMAGE_FILE_MD5 = "F45C83B8E82FC139F7A984059E443A11";
  private static final File NONEXISTENT_FILE = new File("nonexistentfile.zoop");

  @Test
  void createGet() {
    final File file = WHITE_IMAGE_FILE;
    MediaItem m1 = new MediaItem(null, 1, 1, file);

    assertEquals(file, m1.getFile());

    final int pageIndex = 5;
    final boolean noSimilar = true;
    final GroupItem group = new GroupItem(null, 2, 1, "group");
    final String md5 = "f9e2cb7b1b1436182d16b29d81b6bf78";
    MediaItem m2 = new MediaItem(null, 1, 1, pageIndex, noSimilar, group, file, md5, null);

    assertEquals(pageIndex, m2.getPageIndex());
    assertEquals(group, m2.getGroup());
    assertEquals(file, m2.getFile());
    assertEquals(md5, m2.getMD5());
    assertEquals(noSimilar, m2.hasNoSimilar());
  }

  @Test
  void setGet() {
    MediaItem m1 = new MediaItem(null, 1, 1, null);

    final int page = 2;
    m1.setPageIndex(page);
    assertEquals(page, m1.getPageIndex());

    GroupItem group = new GroupItem(null, 2, 1, "group");
    m1.setGroup(group);
    assertEquals(group, m1.getGroup());

    assertNull(m1.getHistogram());
  }

  @Test
  void isVideoIsImage() {
    for (String ext : Filters.IMAGE_EXTS) {
      MediaItem m1 = new MediaItem(null, 1, 1, new File("image" + ext));
      assertTrue(m1.isImage());
      assertFalse(m1.isVideo());
    }

    for (String ext : Filters.VIDEO_EXTS) {
      MediaItem m1 = new MediaItem(null, 1, 1, new File("video" + ext));
      assertFalse(m1.isImage());
      assertTrue(m1.isVideo());
    }
  }

  @Test
  void md5() {
    MediaItem m1 = new MediaItem(null, 1, 1, null);
    m1.initializeMD5();
    assertNull(m1.getMD5());

    MediaItem m2 = new MediaItem(null, 1, 1, NONEXISTENT_FILE);
    m2.initializeMD5();
    assertNull(m2.getMD5());

    MediaItem m3 = new MediaItem(null, 1, 1, WHITE_IMAGE_FILE);
    m3.initializeMD5();
    assertTrue(WHITE_IMAGE_FILE_MD5.equalsIgnoreCase(m3.getMD5()));
  }

  @Test
  void renameTo() {
    MediaItem m1 = new MediaItem(null, 1, 1, null);
    assertFalse(m1.moveFile(null));
    assertFalse(m1.moveFile(NONEXISTENT_FILE));

    MediaItem m2 = new MediaItem(null, 1, 1, NONEXISTENT_FILE);
    assertTrue(m2.moveFile(NONEXISTENT_FILE));
    assertFalse(m2.moveFile(new File("randomfilethatdoesntexist.zapp")));

    File file = new File("target/test-classes/renametest.bmp");
    File dest = new File("target/test-classes/renametest-dest.bmp");
    try {
      dest.delete();
      file.createNewFile();
      MediaItem m3 = new MediaItem(null, 1, 1, file);
      assertTrue(m3.moveFile(dest));
      assertTrue(m3.moveFile(file));
    } catch (IOException ignored) {
    }
  }

  @Test
  void similarity() {
    MediaItem m1 = new MediaItem(null, 1, 1, WHITE_IMAGE_FILE);
    MediaItem m2 = new MediaItem(null, 2, 1, WHITE_IMAGE_FILE);
    MediaItem m3 = new MediaItem(null, 3, 1, BLACK_IMAGE_FILE);

    assertEquals(0, m1.getSimilarityTo(m2));
    assertEquals(0, m1.getSimilarityTo(m3));

    m1.initializeMD5();
    assertEquals(0, m1.getSimilarityTo(m2));

    m2.initializeMD5();
    assertEquals(1, m1.getSimilarityTo(m2));
    assertEquals(1, m2.getSimilarityTo(m1));

    m3.initializeMD5();
    assertEquals(0, m1.getSimilarityTo(m3));
  }

  @Test
  void toStringDoesNotThrow() {
    assertDoesNotThrow(() -> new MediaItem(null, 1, 1, null).toString());
  }

}
