package net.vuonnala;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.time.Duration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONArray;
import org.json.JSONObject;

public class LLMClient {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Single, unified HTTP client
    private final HttpClient httpClient;
    private final String baseUrl;

    public LLMClient(String ip, int port) {
        // Example: Force HTTP/1.1, set a 5s connect timeout
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)               // Force HTTP/1.1
                .connectTimeout(Duration.ofSeconds(5))             // Connection handshake
                .build();

        this.baseUrl = "http://" + ip + ":" + port;
    }

    /**
     * Sends the user (already validated) JSON to LM Studio’s /v1/chat/completions endpoint.
     * Expects the JSON to contain "model" and "messages" fields:
     *   {
     *     "model": "my-local-model",
     *     "messages": [
     *       {"role": "system", "content": "some system context"},
     *       {"role": "user", "content": "my question"}
     *     ]
     *   }
     *
     * @param validatedJson The entire validated conversation JSON
     * @return The raw response from LM Studio
     * @throws IOException
     * @throws InterruptedException
     */
    public String sendToLlmStudio(String validatedJson) throws IOException, InterruptedException {
        System.out.println("[DEBUG] sendToLlmStudio called. Attempting HTTP request...");

        // 1) Parse the validated JSON
        JsonNode rootNode = MAPPER.readTree(validatedJson);

        // 2) Extract the "model"
        JsonNode modelNode = rootNode.get("model");
        if (modelNode == null || !modelNode.isTextual()) {
            throw new IllegalArgumentException("JSON must contain a 'model' field (string).");
        }
        String model = modelNode.asText();

        // 3) Extract the "messages" array
        JsonNode messagesNode = rootNode.get("messages");
        if (messagesNode == null || !messagesNode.isArray()) {
            throw new IllegalArgumentException("Validated JSON does not have a 'messages' array.");
        }

        // 4) Build the request body for LM Studio
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", model);
        requestBody.put("messages", new JSONArray(messagesNode.toString()));
        // If you need other fields, parse them from validatedJson and add to requestBody

        // 5) Build the POST request, with an overall 5s timeout
        String endpoint = baseUrl + "/v1/chat/completions";
        System.out.println("[DEBUG] POSTing to: " + endpoint);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                // This covers the time to receive the entire response, not just connect
                //.timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        // 6) Send request
        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            System.out.println("[DEBUG] HTTP status: " + response.statusCode());
            System.out.println("[DEBUG] Response body: " + response.body());
            return response.body();

        } catch (IOException | InterruptedException e) {
            // Catch the exact connection or timeout error
            System.out.println("[DEBUG] Exception during HTTP request: " + e.getMessage());
            e.printStackTrace();
            throw e; // Rethrow so it's handled upstream
        }
    }
}
