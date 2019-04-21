package menagerie.model.menagerie.db;

public interface MenagerieDatabaseLoadListener {

    void startItemLoading(int total);

    void itemsLoading(int count, int total);

    void startTagLoading(int total);

    void tagsLoading(int count, int total);

}
