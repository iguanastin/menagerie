package menagerie.model.menagerie;

import com.sun.jna.platform.FileUtils;
import menagerie.gui.Main;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class ArchiveItem extends GroupItem {

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
        if (!forget()) return false;

        FileUtils fu = FileUtils.getInstance();
        if (fu.hasTrash()) {
            try {
                fu.moveToTrash(new File[]{getFile()});
                return true;
            } catch (IOException e) {
                Main.log.log(Level.SEVERE, "Unable to send file to recycle bin: " + getFile(), e);
                return false;
            }
        } else {
            return getFile().delete();
        }
    }

}
