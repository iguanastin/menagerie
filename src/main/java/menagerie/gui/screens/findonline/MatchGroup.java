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

package menagerie.gui.screens.findonline;

import menagerie.duplicates.DuplicateFinder;
import menagerie.duplicates.Match;
import menagerie.model.menagerie.MediaItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MatchGroup {

    private static final Logger LOGGER = Logger.getLogger(MatchGroup.class.getName());

    enum Status {
        WAITING, PROCESSING, FAILED, SUCCEEDED
    }

    private final MediaItem item;
    private final List<Match> matches = new ArrayList<>();
    private Status status = Status.WAITING;


    public MatchGroup(MediaItem item) {
        this.item = item;
    }

    public MediaItem getItem() {
        return item;
    }

    public Status retrieveMatches(List<DuplicateFinder> finders) {
        setStatus(Status.PROCESSING);
        matches.clear();

        for (DuplicateFinder finder : finders) {
            try {
                matches.addAll(finder.getMatchesFor(item.getFile()));
            } catch (IOException | NullPointerException | ArrayIndexOutOfBoundsException e) {
                LOGGER.log(Level.INFO, "Failed to get matches for: " + item.getFile(), e);
                setStatus(Status.FAILED);
            }
        }

        if (getStatus() != Status.FAILED) setStatus(Status.SUCCEEDED);
        return getStatus();
    }

    public List<Match> getMatches() {
        return matches;
    }

    public synchronized void setStatus(Status status) {
        this.status = status;
    }

    public synchronized Status getStatus() {
        return status;
    }

}
