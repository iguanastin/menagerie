package menagerie.model.menagerie.importer;

public interface ImportJobStatusListener {

    void changed(ImportJob.Status status);

}
