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

package menagerie.gui.screens.duplicates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import menagerie.model.SimilarPair;
import menagerie.model.menagerie.Item;
import menagerie.model.menagerie.MediaItem;
import menagerie.model.menagerie.Menagerie;
import menagerie.util.CancellableThread;
import menagerie.util.listeners.ObjectListener;

public class DuplicateManagerThread extends CancellableThread {

  private final List<DuplicateFinderThread> finders = new ArrayList<>();
  private final List<SimilarPair<MediaItem>> pairs = new ArrayList<>();

  private final Menagerie menagerie;
  private final List<Item> compareFrom;
  private final List<Item> compareTo;
  private final double confidence;
  private final ObjectListener<Double> progressListener;
  private final ObjectListener<List<SimilarPair<MediaItem>>> finishListener;

  private int finished = 0, total = 0;

  public DuplicateManagerThread(Menagerie menagerie, List<Item> compareFrom, List<Item> compareTo,
                                double confidence, ObjectListener<Double> progressListener,
                                ObjectListener<List<SimilarPair<MediaItem>>> finishListener) {
    this.menagerie = menagerie;
    this.compareFrom = new ArrayList<>(compareFrom);
    this.compareTo = new ArrayList<>(compareTo);
    this.confidence = confidence;
    this.progressListener = progressListener;
    this.finishListener = finishListener;

    setName("Duplicate Finder Master");
  }

  @Override
  public void run() {
    total = compareFrom.size();
    finished = 0;

    final int threads = Math.min(Runtime.getRuntime().availableProcessors(), compareFrom.size());
    final int chunk = (int) Math.ceil((double) compareFrom.size() / threads);
    final Lock finishLock = new ReentrantLock();
    final CountDownLatch finishLatch = new CountDownLatch(threads);
    for (int i = 0; i < threads; i++) {
      DuplicateFinderThread finder = new DuplicateFinderThread(menagerie,
          compareFrom.subList(i * chunk, Math.min((i + 1) * chunk, compareFrom.size())), compareTo,
          confidence, () -> {
        finished++;
        if (progressListener != null) {
          progressListener.pass(getProgress());
        }
      }, results -> {
        finishLock.lock();
        pairs.addAll(results);
        finishLatch.countDown();
        finishLock.unlock();
      });
      finders.add(finder);
      finder.start();
    }

    while (running) {
      try {
        if (finishLatch.await(5, TimeUnit.SECONDS)) {
          if (finishListener != null) {
            List<SimilarPair<MediaItem>> temp = new ArrayList<>(new HashSet<>(pairs));
            temp.sort(Collections.reverseOrder(Comparator.comparing(SimilarPair::getSimilarity)));
            finishListener.pass(temp); // Put into hashset to remove all duplicates
          }
          break;
        }
      } catch (InterruptedException e) {
        // Do nothing
      }
    }

    running = false;
  }

  private double getProgress() {
    return (double) finished / total;
  }

  @Override
  public synchronized void cancel() {
    super.cancel();

    finders.forEach(CancellableThread::cancel);
  }

}
