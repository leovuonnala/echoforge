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
        System.out.println("[DEBUG] Dispatch method called.");
        // 1) Validate
        messageValidator.validate(jsonContent);

        // 2) Extract conversation ID
        String conversationId = messageInput.getConversationId(jsonContent);

        // 3) Send to LLM
        System.out.println("[DEBUG] Creating new LLMClient with IP=" + ip + " port=" + port);
        LLMClient client = new LLMClient(ip, port);
        String response = client.sendToLlmStudio(jsonContent);
        System.out.println("[DEBUG] Response from LLMClient: " + response);
        // 4) Store the response
        messageStorage.storeResponse(conversationId, response);

        return response;
    }
}
