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

package menagerie.util;

public class Util {

  private Util() {
  }

  public static boolean equalsNullable(Object obj1, Object obj2) {
      if (obj1 == null && obj2 == null) {
          return true;
      }

    if (obj1 != null) {
      return obj1.equals(obj2);
    }

    return false;
  }

  /**
   * Converts a byte count into a pretty string for user's viewing pleasure.
   *
   * @param bytes Byte count
   * @return A string in the format: [0-9]+\.[0-9]{2}(B|KB|MB|GB) E.g. "123.45KB"
   */
  public static String bytesToPrettyString(long bytes) {
      if (bytes > 1024 * 1024 * 1024) {
          return String.format("%.2fGB", bytes / 1024.0 / 1024 / 1024);
      } else if (bytes > 1024 * 1024) {
          return String.format("%.2fMB", bytes / 1024.0 / 1024);
      } else if (bytes > 1024) {
          return String.format("%.2fKB", bytes / 1024.0);
      } else {
          return String.format("%dB", bytes);
      }
  }

}
