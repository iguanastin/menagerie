package menagerie.model.search.rules;

import menagerie.model.menagerie.Tag;

import java.util.ArrayList;
import java.util.List;

public class TestItemBuilder {
    private TestItem item;

    public TestItemBuilder() {
        initTestItem();
    }

    private void initTestItem() {
        this.item = new TestItem(null, 1, 1);
    }

    public TestItem build() {
        return item;
    }

    public TestItemBuilder id(int id) {
        item.setId(id);
        return this;
    }

    public TestItemBuilder dateAdded(long dateAdded) {
        item.setDateAdded(dateAdded);
        return this;
    }

    public TestItemBuilder tags(String... tags) {
        List<Tag> tagObjects = new ArrayList<>();
        for (String t: tags) {
            tagObjects.add(new Tag(null, 1, t, null));
        }
        item.setTags(tagObjects);
        return this;
    }
}
