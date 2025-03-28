package net.vuonnala;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class MessageStorage {

    private final String dbUrl;

    public MessageStorage(String dbFile) throws SQLException {
        this.dbUrl = "jdbc:sqlite:" + dbFile;
        initializeDatabase();
    }

    private void initializeDatabase() throws SQLException {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            String sql = """
                CREATE TABLE IF NOT EXISTS responses (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp TEXT NOT NULL,
                    conversation_id TEXT NOT NULL,
                    request_json TEXT NOT NULL,
                    response_content TEXT NOT NULL
                );
            """;
            String createConversations = """
                CREATE TABLE IF NOT EXISTS conversations (
                    conversation_id TEXT PRIMARY KEY,
                    title TEXT NOT NULL,
                    created_at TEXT NOT NULL
                );
            """;
            String createResponses = """
                CREATE TABLE IF NOT EXISTS responses (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp TEXT NOT NULL,
                    conversation_id TEXT NOT NULL,
                    request_json TEXT NOT NULL,
                    response_content TEXT NOT NULL,
                    FOREIGN KEY (conversation_id) REFERENCES conversations(conversation_id)
                );
            """;

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
                stmt.execute(createConversations);
                stmt.execute(createResponses);
            }
        }
    }
    public void registerConversation(String conversationId, String title) throws SQLException {
        String sql = "INSERT OR IGNORE INTO conversations (conversation_id, title, created_at) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, conversationId);
            pstmt.setString(2, title);
            pstmt.setString(3, Instant.now().toString());
            pstmt.executeUpdate();
        }
    }

    public void storeResponse(String conversationId, String requestJson, String responseContent) throws SQLException {
        String sql = "INSERT INTO responses (timestamp, conversation_id, request_json, response_content) VALUES (?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, Instant.now().toString());
            pstmt.setString(2, conversationId);
            pstmt.setString(3, requestJson);         // new field: the full message history sent to LLM
            pstmt.setString(4, responseContent);     // the reply from LLM
            pstmt.executeUpdate();
        }
    }


    public List<ResponseRecord> getAllResponses() throws SQLException {
        String sql = "SELECT * FROM responses ORDER BY timestamp DESC";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return extractResults(rs);
        }
    }

    public List<ResponseRecord> getResponsesByConversationId(String conversationId) throws SQLException {
        String sql = "SELECT * FROM responses WHERE conversation_id = ? ORDER BY timestamp ASC";
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, conversationId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return extractResults(rs);
            }
        }
    }

    public List<ConversationSummary> getAllConversations() throws SQLException {
        List<ConversationSummary> list = new ArrayList<>();
        String sql = "SELECT conversation_id, title FROM conversations ORDER BY created_at DESC";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String id = rs.getString("conversation_id");
                String title = rs.getString("title");
                list.add(new ConversationSummary(id, title));
            }
        }

        return list;
    }
    public void updateConversationTitle(String conversationId, String newTitle) throws SQLException {
        String sql = "UPDATE conversations SET title = ? WHERE conversation_id = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newTitle);
            pstmt.setString(2, conversationId);
            pstmt.executeUpdate();
        }
    }

    public static class ConversationSummary {
        public final String id;
        public final String title;

        public ConversationSummary(String id, String title) {
            this.id = id;
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }



    private List<ResponseRecord> extractResults(ResultSet rs) throws SQLException {
        List<ResponseRecord> results = new ArrayList<>();
        while (rs.next()) {
            ResponseRecord record = new ResponseRecord(
                    rs.getInt("id"),
                    rs.getString("timestamp"),
                    rs.getString("conversation_id"),
                    rs.getString("request_json"),
                    rs.getString("response_content")
            );
            results.add(record);
        }
        return results;
    }

    public static class ResponseRecord {
        public final int id;
        public final String timestamp;
        public final String conversationId;
        public final String requestJson;
        public final String responseContent;

        public ResponseRecord(int id, String timestamp, String conversationId, String requestJson, String responseContent) {
            this.id = id;
            this.timestamp = timestamp;
            this.conversationId = conversationId;
            this.requestJson = requestJson;
            this.responseContent = responseContent;
        }

        @Override
        public String toString() {
            return String.format("ID: %d\nTime: %s\nConversation ID: %s\nResponse:\n%s\n",
                    id, timestamp, conversationId, responseContent);
        }
    }
}


