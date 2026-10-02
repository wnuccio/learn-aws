package org.example.lambda2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RealMessageRepository implements MessageRepository {

    private static final Logger log = LoggerFactory.getLogger(RealMessageRepository.class);

    private static final String DB_URL = requireEnv("DB_URL");
    private static final String DB_USER = requireEnv("DB_USER");
    private static final String DB_PASSWORD = requireEnv("DB_PASSWORD");

    private static final String CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS messages (id TEXT PRIMARY KEY, message TEXT NOT NULL)";
    private static final String INSERT_MESSAGE =
            "INSERT INTO messages (id, message) VALUES (?, ?)";
    private static final String RETRIEVE_MESSAGES =
            "SELECT message FROM messages ORDER BY message";

    private static String requireEnv(String name) {
        String value = System.getenv(name);

        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(name + " environment variable not set");
        }

        return value;
    }

    @Override
    public void insertMessage(String id, String message) {
        // one connection per call: a pooled/static connection could be stale after the Lambda is frozen
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            createTableIfMissing(connection);

            try (PreparedStatement statement = connection.prepareStatement(INSERT_MESSAGE)) {
                statement.setString(1, id);
                statement.setString(2, message);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("failed to insert message id=" + id, e);
        }
    }

    @Override
    public List<String> getAllMessagesOrdered() {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            createTableIfMissing(connection);

            try (PreparedStatement statement = connection.prepareStatement(RETRIEVE_MESSAGES)) {
                try (ResultSet rs = statement.executeQuery()) {
                    List<String> messages = new ArrayList<>();
                    while (rs.next()) {
                        messages.add(rs.getString("message"));
                    }
                    return messages;
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("failed to retrieve messages", e);
        }
    }

    private void createTableIfMissing(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(CREATE_TABLE);
        }
        log.info("table messages ready");
    }
}