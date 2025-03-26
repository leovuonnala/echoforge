package net.vuonnala;

public class MessageDispatcher {
    private MessageValidator messageValidator;

    public MessageDispatcher (MessageValidator messageValidator) {
        this.messageValidator = messageValidator;
    }

    public void dispatch (String message) {
        try {
            messageValidator.validate(message);
        } catch (IllegalArgumentException e) {
            
        }
    }
}
