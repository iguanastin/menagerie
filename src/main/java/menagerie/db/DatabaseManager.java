package menagerie.db;

import menagerie.db.update.DatabaseUpdater;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {

    private final Connection db;


    public DatabaseManager(String jdbcPath, String user, String password) throws SQLException {
        db = DriverManager.getConnection(jdbcPath, user, password);

        //TODO: Initialize database if necessary

        DatabaseUpdater.updateDatabaseIfNecessary(db);
    }

    public void close() throws SQLException {
        db.close();
    }

}
