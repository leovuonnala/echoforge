package net.vuonnala;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

public class MessageBuilderUI extends JPanel {

    private JTextField conversationIdField;
    private JTextField modelField;
    private JTextArea systemMessageArea;
    private JTextArea userMessageArea;
    private JTextField tagField;
    private JButton addUserMessageButton;
    private JButton clearMessagesButton;
    private DefaultListModel<String> messageListModel;
    private JList<String> messageList;

    public MessageBuilderUI() {
        setLayout(new BorderLayout());
        initComponents();
        layoutComponents();
        addListeners();
    }

    private void initComponents() {
        conversationIdField = new JTextField(20);
        modelField = new JTextField("my-local-model", 20);
        systemMessageArea = new JTextArea(3, 40);
        userMessageArea = new JTextArea(3, 40);
        tagField = new JTextField(30);

        addUserMessageButton = new JButton("Add User Message");
        clearMessagesButton = new JButton("Clear Messages");

        messageListModel = new DefaultListModel<>();
        messageList = new JList<>(messageListModel);
    }

    private void layoutComponents() {
        JPanel topPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        topPanel.setBorder(new TitledBorder("Metadata"));
        topPanel.add(new JLabel("Conversation ID:"));
        topPanel.add(conversationIdField);
        topPanel.add(new JLabel("Model:"));
        topPanel.add(modelField);
        topPanel.add(new JLabel("Tags (comma-separated):"));
        topPanel.add(tagField);

        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.setBorder(new TitledBorder("System Message"));
        messagePanel.add(new JScrollPane(systemMessageArea), BorderLayout.CENTER);

        JPanel userPanel = new JPanel(new BorderLayout());
        userPanel.setBorder(new TitledBorder("User Message"));
        userPanel.add(new JScrollPane(userMessageArea), BorderLayout.CENTER);

        JPanel userActions = new JPanel();
        userActions.add(addUserMessageButton);
        userActions.add(clearMessagesButton);

        JPanel messageListPanel = new JPanel(new BorderLayout());
        messageListPanel.setBorder(new TitledBorder("Message Sequence"));
        messageListPanel.add(new JScrollPane(messageList), BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(messagePanel, BorderLayout.WEST);
        add(userPanel, BorderLayout.CENTER);
        add(userActions, BorderLayout.SOUTH);
        add(messageListPanel, BorderLayout.EAST);
    }

    private void addListeners() {
        addUserMessageButton.addActionListener(this::handleAddUserMessage);
        clearMessagesButton.addActionListener(e -> messageListModel.clear());
    }

    private void handleAddUserMessage(ActionEvent e) {
        String content = userMessageArea.getText().trim();
        if (!content.isEmpty()) {
            messageListModel.addElement("{" + "\"role\": \"user\", \"content\": \"" + content.replaceAll("\"", "\\\"") + "\"}");
            userMessageArea.setText("");
        }
    }

    public String buildJson() {
        List<String> messages = new ArrayList<>();

        String sysMsg = systemMessageArea.getText().trim();
        if (!sysMsg.isEmpty()) {
            messages.add("{" + "\"role\": \"system\", \"content\": \"" + sysMsg.replaceAll("\"", "\\\"") + "\"}");
        }

        for (int i = 0; i < messageListModel.getSize(); i++) {
            messages.add(messageListModel.getElementAt(i));
        }

        String convId = conversationIdField.getText().trim();
        String model = modelField.getText().trim();
        String tags = tagField.getText().trim();

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        if (!convId.isEmpty()) json.append("  \"conversation_id\": \"").append(convId).append("\",\n");
        json.append("  \"model\": \"").append(model).append("\",\n");
        json.append("  \"messages\": [\n    ").append(String.join(",\n    ", messages)).append("\n  ]");
        if (!tags.isEmpty()) {
            json.append(",\n  \"metadata\": { \"tags\": [");
            String[] tagArr = tags.split(",");
            for (int i = 0; i < tagArr.length; i++) {
                if (i > 0) json.append(", ");
                json.append("\"").append(tagArr[i].trim()).append("\"");
            }
            json.append("] }\n");
        } else {
            json.append("\n");
        }
        json.append("}");
        return json.toString();
    }

    // Optional: Expose fields for integration with outer UI if needed
    public JTextField getConversationIdField() { return conversationIdField; }
    public JTextField getModelField() { return modelField; }
    public JTextArea getSystemMessageArea() { return systemMessageArea; }
    public JTextArea getUserMessageArea() { return userMessageArea; }
    public JList<String> getMessageList() { return messageList; }
    public String getFinalJson() { return buildJson(); }
}