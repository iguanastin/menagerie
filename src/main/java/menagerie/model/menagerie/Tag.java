package menagerie.model.menagerie;

public class Tag implements Comparable<Tag> {

    private Menagerie menagerie;
    private final int id;
    private final String name;


    public Tag(Menagerie menagerie, int id, String name) {
        this.menagerie = menagerie;
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Menagerie getMenagerie() {
        return menagerie;
    }

    public int computeFrequency() {
        int count = 0;

        for (ImageInfo img : menagerie.getImages()) {
            if (img.hasTag(this)) count++;
        }

        return count;
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
