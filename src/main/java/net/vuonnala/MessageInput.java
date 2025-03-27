package net.vuonnala;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MessageInput {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Extracts the conversation_id from the JSON (if present). Otherwise returns "unknown".
     */
    public String getConversationId(String json) {
        try {
            JsonNode root = MAPPER.readTree(json);
            JsonNode idNode = root.get("conversation_id");
            if (idNode != null && idNode.isTextual()) {
                return idNode.asText();
            }
        } catch (Exception e) {
            // Swallow or log; validation is handled in MessageValidator anyway.
        }
        return "unknown";
    }
}
