package menagerie.model.menagerie.db;

public class DatabaseUpdateException extends RuntimeException {

    public DatabaseUpdateException() {
        super();
    }

    public DatabaseUpdateException(String message) {
        super(message);
    }

}
