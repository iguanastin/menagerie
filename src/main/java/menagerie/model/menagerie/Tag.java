package menagerie.model.menagerie;

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

    private final int id;
    private final String name;

    private int frequency = 0;


    /**
     * Constructs a tag. No checks for uniqueness of ID are performed.
     * <p>
     * Tag name is expected to match the {@link #NAME_REGEX} field.
     *
     * @param id   ID of this tag. Must be unique within a Menagerie.
     * @param name Tag's name. Will be converted to lowercase.
     */
    public Tag(int id, String name) {
        if (name == null) throw new NullPointerException("Name cannot be null");
        if (id < 0) throw new IllegalArgumentException("ID must be >= 0");
        if (!name.matches(NAME_REGEX))
            throw new IllegalArgumentException(String.format("Name must match regex: \"%s\"", NAME_REGEX));

        this.id = id;
        this.name = name.toLowerCase();
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
    void incrementFrequency() {
        frequency++;
    }

    /**
     * Decrements the frequency of usage.
     */
    void decrementFrequency() {
        frequency--;
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
