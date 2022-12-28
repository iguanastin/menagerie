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

import com.sun.jna.platform.FileUtils;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ArchiveItem extends GroupItem {

  private static final Logger LOGGER = Logger.getLogger(ArchiveItem.class.getName());

  private final File file;

  /**
   * ID uniqueness is not verified by this.
   *
   * @param menagerie Menagerie that owns this item.
   * @param id        Unique ID of this item.
   * @param dateAdded Date this item was added to the Menagerie.
   * @param title     Title of this group.
   */
  public ArchiveItem(Menagerie menagerie, int id, long dateAdded, String title, File file) {
    super(menagerie, id, dateAdded, title);
    this.file = file;
  }

  public File getFile() {
    return file;
  }

  @Override
  public boolean addItem(MediaItem item) {
    return false;
  }

  @Override
  public boolean removeItem(MediaItem item) {
    return false;
  }

  @Override
  public void removeAll() {
    // No action
  }

  @Override
  protected boolean delete() {
    if (!forget()) {
      return false;
    }

    FileUtils fu = FileUtils.getInstance();
    if (fu.hasTrash()) {
      try {
        fu.moveToTrash(new File[] {getFile()});
        return true;
      } catch (IOException e) {
        LOGGER.log(Level.SEVERE, "Unable to send file to recycle bin: " + getFile(), e);
        return false;
      }
    } else {
      return getFile().delete();
    }
  }

}
