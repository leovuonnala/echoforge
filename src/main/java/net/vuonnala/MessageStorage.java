package net.vuonnala;

import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

public class MessageStorage {

    private final Path basePath;

    public MessageStorage(String baseDirectory) {
        // e.g. baseDirectory could be "responses/"
        this.basePath = Path.of(baseDirectory);
    }

    /**
     * Store the LLM response in a file named with timestamp.
     *
     * @param conversationId An ID from the JSON or "unknown"
     * @param responseContent The raw text or JSON from LLMStudio
     */
    public void storeResponse(String conversationId, String responseContent) throws IOException {
        Files.createDirectories(basePath.resolve(conversationId));

        String timestamp = Instant.now().toString().replace(":", "-");
        String fileName = "response_" + timestamp + ".json";
        Path filePath = basePath.resolve(conversationId).resolve(fileName);

        JSONObject data = new JSONObject();
        data.put("timestamp", Instant.now().toString());
        data.put("conversation_id", conversationId);
        data.put("response_content", responseContent);

        Files.writeString(filePath, data.toString(2));
    }
}
