package net.vuonnala;

public class Main {
    public static void main(String[] args) {
        try {
            // 1) Create a default validator with a known schema path
            String defaultSchemaPath = "schema.json"; // Adjust as needed
            MessageValidator validator = new MessageValidator(defaultSchemaPath);

            // 2) Create message input logic & storage
            MessageInput messageInput = new MessageInput();
            MessageStorage storage = new MessageStorage("responses.db");

            // 3) Create the dispatcher
            MessageDispatcher dispatcher = new MessageDispatcher(validator, messageInput, storage);

            // 4) Create the UI
            UserInterface ui = new UserInterface(dispatcher, validator, storage);
            ui.setVisible(true);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
