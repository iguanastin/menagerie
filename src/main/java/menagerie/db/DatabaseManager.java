package menagerie.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {

    private final Connection db;


    public DatabaseManager(String jdbcPath, String user, String password) throws SQLException {
        db = DriverManager.getConnection(jdbcPath, user, password);

        //TODO: Update database if necessary
    }

    public void close() throws SQLException {
        db.close();
    }

}
