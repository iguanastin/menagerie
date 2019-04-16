package menagerie.model.menagerie;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple Tag class.
 */
public class Tag implements Comparable<Tag> {

    /**
     * Regex defining the acceptable tag name strings.
     * <p>
     * No spaces, no newlines, only common alphanumerical characters and symbols.
     */
    public static final String NAME_REGEX = "[0-9a-zA-Z!@#$%^&*()\\-_=+\\[\\]{};:',./<>?`~]+";

    private final Menagerie menagerie;

    private final int id;
    private final String name;
    private List<String> notes = new ArrayList<>();
    private String colorCSS = null;

    private int frequency = 0;


    /**
     * Constructs a tag. No checks for uniqueness of ID are performed.
     * <p>
     * Tag name is expected to match the {@link #NAME_REGEX} field.
     *
     * @param id   ID of this tag. Must be unique within a Menagerie.
     * @param name Tag's name. Will be converted to lowercase.
     */
    public Tag(Menagerie menagerie, int id, String name, String colorCSS) {
        this.menagerie = menagerie;
        if (name == null) throw new NullPointerException("Name cannot be null");
        if (!name.matches(NAME_REGEX))
            throw new IllegalArgumentException(String.format("Name must match regex: \"%s\"", NAME_REGEX));

        this.id = id;
        this.name = name.toLowerCase();
        this.colorCSS = colorCSS;
    }

    /**
     * @return The ID of this tag.
     */
    public int getId() {
        return id;
    }

    /**
     * @return The name of this tag. Conforms to {@link #NAME_REGEX} requirement.
     */
    public String getName() {
        return name;
    }

    /**
     * @return Frequency this tag is used by items.
     */
    public int getFrequency() {
        return frequency;
    }

    /**
     * Increments the frequency of usage.
     */
    public void incrementFrequency() {
        frequency++;
    }

    /**
     * Decrements the frequency of usage.
     */
    void decrementFrequency() {
        frequency--;
    }

    public List<String> getNotes() {
        return notes;
    }

    public String getColorCSS() {
        return colorCSS;
    }

    public void setColorCSS(String colorCSS) {
        if (this.colorCSS.equals(colorCSS)) return;

        this.colorCSS = colorCSS;

        menagerie.getDatabaseManager().setTagColorAsync(getId(), colorCSS);
    }

    public void addNote(String note) {
        notes.add(note);

        menagerie.getDatabaseManager().addTagNoteAsync(getId(), note);
    }

    public void removeNote(String note) {
        if (notes.remove(note)) {
            menagerie.getDatabaseManager().removeTagNoteAsync(getId(), note);
        }
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Tag && ((Tag) obj).getId() == getId();
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(getId());
    }

    @Override
    public String toString() {
        return "Tag (" + getId() + ") \"" + getName() + "\"";
    }

    @Override
    public int compareTo(Tag o) {
        return getId() - o.getId();
    }

}
