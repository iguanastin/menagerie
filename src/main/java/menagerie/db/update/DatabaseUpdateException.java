package menagerie.db.update;

public class DatabaseUpdateException extends RuntimeException {

    public DatabaseUpdateException() {
        super();
    }

    public DatabaseUpdateException(String message) {
        super(message);
    }

}
