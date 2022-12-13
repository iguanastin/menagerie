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

package menagerie.gui.screens.move;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import menagerie.model.menagerie.MediaItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileMoveNode {

    private final File folder;
    private final List<MediaItem> items;
    private final List<FileMoveNode> nodes;
    private final int depth;
    private FileMoveNode parent = null;
    private final BooleanProperty preserve = new SimpleBooleanProperty(false);


    public FileMoveNode(File folder, List<MediaItem> items, List<FileMoveNode> nodes) {
        this.folder = folder;
        this.items = items;
        this.nodes = nodes;

        int result = 0;
        File f = getFolder();
        while (f.getParentFile() != null) {
            result++;
            f = f.getParentFile();
        }

        depth = result;
    }

    public FileMoveNode(File folder) {
        this(folder, new ArrayList<>(), new ArrayList<>());
    }

    public File getFolder() {
        return folder;
    }

    public List<MediaItem> getItems() {
        return items;
    }

    public List<FileMoveNode> getNodes() {
        return nodes;
    }

    public int getDepth() {
        return depth;
    }

    public FileMoveNode getParent() {
        return parent;
    }

    public boolean isRoot() {
        for (File root : File.listRoots()) {
            if (root.equals(getFolder())) return true;
        }

        return false;
    }

    public boolean isPreserved() {
        return preserve.getValue();
    }

    public BooleanProperty preserveProperty() {
        return preserve;
    }

    public void setPreserve(boolean preserve) {
        if (!isRoot()) {
            this.preserve.set(preserve);

            if (preserve) {
                for (FileMoveNode node : getNodes()) {
                    node.setPreserve(true);
                }
            } else {
                if (getParent() != null) {
                    getParent().setPreserve(false);
                }
            }
        }
    }

    public void setParent(FileMoveNode parent) {
        this.parent = parent;
    }

}
