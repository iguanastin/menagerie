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

package menagerie.util.folderwatcher;

import java.io.File;
import java.io.FileFilter;

// REENG: Why not extend util.CancellableThread here?
public class FolderWatcherThread extends Thread {

  private volatile boolean running = false;

  private final long timeout;
  private final File watchFolder;
  private final FileFilter filter;

  private final FolderWatcherListener listener;

  /**
   * Constructs a folder watcher thread.
   *
   * @param watchFolder Target folder to watch for files.
   * @param filter      Filter to reduce results.
   * @param timeout     Time between file checks.
   * @param listener    Listener to notify when files are found.
   */
  public FolderWatcherThread(File watchFolder, FileFilter filter, long timeout,
                             FolderWatcherListener listener) {
    super("Folder Watcher Thread");
    this.watchFolder = watchFolder;
    this.filter = filter;
    this.timeout = timeout;
    this.listener = listener;
  }

  /**
   * Tell thread to stop watching for files. Does not forcibly stop the thread.
   */
  public void stopWatching() {
    running = false;
    notify();
  }

  @Override
  public void run() {
      if (listener == null || watchFolder == null) {
          return;
      }

    running = true;

    while (running) {
      File folder = watchFolder;
      if (folder.exists() && folder.isDirectory()) {
        File[] files = folder.listFiles(filter);
          if (files != null && files.length > 0) {
              listener.foundNewFiles(files);
          }
      }

      try {
        synchronized (this) {
          wait(timeout);
        }
      } catch (InterruptedException ignored) {
      }
    }
  }

}
