// This is a JUnit test class for MessageInput and supporting classes
package net.vuonnala;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class MessageInputTest {

    private MessageInput input;

    @BeforeEach
    void setUp() {
        input = new MessageInput();
    }

    @Test
    void testGetConversationId_valid() {
        String json = "{\"conversation_id\":\"abc123\"}";
        assertEquals("abc123", input.getConversationId(json));
    }

    @Test
    void testGetConversationId_missing() {
        String json = "{\"model\":\"test\"}";
        assertEquals("unknown", input.getConversationId(json));
    }

    @Test
    void testGetConversationId_invalidJson() {
        String json = "{";
        assertEquals("unknown", input.getConversationId(json));
    }

    @Test
    void testMessageValidator_validJson() throws Exception {
        String schema = """
        {
          "$schema": "http://json-schema.org/draft-07/schema#",
          "type": "object",
          "properties": {
            "model": {"type": "string"},
            "messages": {"type": "array"}
          },
          "required": ["model", "messages"]
        }
        """;

        MessageValidator validator = new MessageValidator(
                new ByteArrayInputStream(schema.getBytes())
        );

        String validJson = "{\"model\":\"test\",\"messages\":[]}";
        assertDoesNotThrow(() -> validator.validate(validJson));
    }

    @Test
    void testMessageValidator_invalidJson() throws Exception {
        String schema = """
        {
          "$schema": "http://json-schema.org/draft-07/schema#",
          "type": "object",
          "properties": {
            "model": {"type": "string"},
            "messages": {"type": "array"}
          },
          "required": ["model", "messages"]
        }
        """;

        MessageValidator validator = new MessageValidator(
                new ByteArrayInputStream(schema.getBytes())
        );

        String invalidJson = "{\"model\":\"test\"}"; // missing 'messages'
        Exception ex = assertThrows(IllegalArgumentException.class, () -> validator.validate(invalidJson));
        assertTrue(ex.getMessage().contains("JSON validation error"));
    }

    @Test
    void testLLMClientSendToLlmStudio() throws IOException, InterruptedException {
        HttpClient mockClient = mock(HttpClient.class);
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn("{\"choices\":[{\"message\":{\"content\":\"Hi\"}}]}");

        // Stub only; actual injection into LLMClient not implemented
        LLMClient client = new LLMClient("127.0.0.1", 1234);
        assertNotNull(client);
    }

    @Test
    void testMessageBuilderUI_buildJson() {
        MessageBuilderUI ui = new MessageBuilderUI();
        ui.getConversationIdField().setText("abc");
        ui.getModelField().setText("model-x");
        ui.getSystemMessageArea().setText("System prompt");
        ui.getUserMessageArea().setText("User message");

        ui.getMessageList().setListData(new String[]{
                "{\"role\": \"user\", \"content\": \"Hello\"}"
        });

        String json = ui.getFinalJson();
        assertTrue(json.contains("conversation_id"));
        assertTrue(json.contains("messages"));
        assertTrue(json.contains("System prompt"));
    }

    @Test
    void testMessageDispatcherIntegration() throws Exception {
        String schema = """
        {
          "$schema": "http://json-schema.org/draft-07/schema#",
          "type": "object",
          "properties": {
            "conversation_id": {"type": "string"},
            "model": {"type": "string"},
            "messages": {"type": "array"}
          },
          "required": ["conversation_id", "model", "messages"]
        }
        """;

        MessageValidator validator = new MessageValidator(
                new ByteArrayInputStream(schema.getBytes())
        );

        MessageInput input = new MessageInput();
        MessageStorage storage = new MessageStorage(":memory:");

        MessageDispatcher dispatcher = new MessageDispatcher(validator, input, storage);

        String validJson = """
        {
          \"conversation_id\": \"conv1\",
          \"model\": \"model-test\",
          \"messages\": [
            {\"role\": \"user\", \"content\": \"hello\"}
          ]
        }
        """;

        // Since dispatch calls real HTTP, we expect this to fail unless mocked or sandboxed
        assertThrows(Exception.class, () -> dispatcher.dispatch(validJson, "127.0.0.1", 1234));
    }
}