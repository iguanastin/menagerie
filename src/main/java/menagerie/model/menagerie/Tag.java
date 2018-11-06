package menagerie.model.menagerie;

public class Tag implements Comparable<Tag> {

    private final int id;
    private final String name;

    private int frequency = 0;


    public Tag(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getFrequency() {
        return frequency;
    }

    void incrementFrequency() {
        frequency++;
    }

    void decrementFrequency() {
        frequency--;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Tag && ((Tag) obj).getId() == getId();
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
