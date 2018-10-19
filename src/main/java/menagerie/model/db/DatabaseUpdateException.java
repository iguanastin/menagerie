package menagerie.model.db;

public class DatabaseUpdateException extends RuntimeException {

    public DatabaseUpdateException() {
        super();
    }

    public DatabaseUpdateException(String message) {
        super(message);
    }

}
