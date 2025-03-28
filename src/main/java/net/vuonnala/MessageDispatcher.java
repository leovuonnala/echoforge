package net.vuonnala;

public class MessageDispatcher {
    private final MessageValidator messageValidator;
    private final MessageInput messageInput;
    private final MessageStorage messageStorage;

    public MessageDispatcher(MessageValidator validator,
                             MessageInput input,
                             MessageStorage storage) {
        this.messageValidator = validator;
        this.messageInput = input;
        this.messageStorage = storage;
    }

    /**
     * Validate, call LLM, and store the response.
     *
     * @param jsonContent  The user-supplied JSON
     * @param ip           The IP for LLMStudio
     * @param port         The port for LLMStudio
     * @return The raw response from LLMStudio
     * @throws Exception   If validation or sending fails
     */
    public String dispatch(String jsonContent, String ip, int port) throws Exception {
        messageValidator.validate(jsonContent);
        String conversationId = messageInput.getConversationId(jsonContent);

        System.out.println("[DEBUG] Creating new LLMClient with IP=" + ip + " port=" + port);
        LLMClient client = new LLMClient(ip, port);
        String response = client.sendToLlmStudio(jsonContent);
        System.out.println("[DEBUG] Response from LLMClient: " + response);
        messageStorage.storeResponse(conversationId, jsonContent, response);

        return response;
    }
}
