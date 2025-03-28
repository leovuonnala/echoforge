package net.vuonnala;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.SwingWorker;

import org.json.JSONArray;
import org.json.JSONObject;

public class UserInterface extends JFrame {

    private final MessageDispatcher dispatcher;
    private final MessageValidator validator;
    private String currentConversationId = UUID.randomUUID().toString();
    private JTextField ipField;
    private JTextField portField;
    private JButton dispatchButton;
    private JTextField userInputField;
    private JTextField systemInputField;
    private JButton sendButton;
    private JPanel chatPanel;
    private JScrollPane chatScrollPane;
    private DefaultListModel<String> historyListModel;
    private JList<String> historyList;
    private JComboBox<String> modelDropdown;
    private List<JSONObject> messageHistory;
    private final MessageStorage messageStorage;
    private JButton newChatButton;
    private Map<String, String> conversationMap = new HashMap<>();  // title → UUID


    public UserInterface(MessageDispatcher dispatcher, MessageValidator validator, MessageStorage messageStorage) {
        super("LLM Message Dispatch Tool");
        this.dispatcher = dispatcher;
        this.validator = validator;
        this.messageStorage = messageStorage;
        initComponents();
        layoutComponents();
        initMenu();
        addListeners();
        loadHistory();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        messageHistory = new ArrayList<>();
    }

    private void initMenu() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");

        JMenuItem openSchemaItem = new JMenuItem("Open Schema");
        openSchemaItem.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File schemaFile = fileChooser.getSelectedFile();
                try {
                    validator.loadSchema(schemaFile.getAbsolutePath());
                    JOptionPane.showMessageDialog(this,
                            "Schema loaded successfully from: " + schemaFile.getName(),
                            "Schema Loaded", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this,
                            "Failed to load schema: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        fileMenu.add(openSchemaItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
    }

    private void initComponents() {
        ipField = new JTextField("127.0.0.1", 10);
        portField = new JTextField("1234", 5);
        dispatchButton = new JButton("Connect");
        newChatButton = new JButton("New Chat");
        userInputField = new JTextField(50);
        systemInputField = new JTextField(50);
        sendButton = new JButton("Send");
        modelDropdown = new JComboBox<>();
        modelDropdown.setPrototypeDisplayValue("Select model...");


        chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatScrollPane = new JScrollPane(chatPanel);
        chatScrollPane.setBorder(BorderFactory.createTitledBorder("Chat"));

        historyListModel = new DefaultListModel<>();
        historyList = new JList<>(historyListModel);
        historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String title = historyList.getSelectedValue();
                String id = conversationMap.get(title);
                loadSelectedHistory(id);
            }
        });
        JPopupMenu historyMenu = new JPopupMenu();
        JMenuItem renameItem = new JMenuItem("Rename Chat...");
        historyMenu.add(renameItem);
        renameItem.addActionListener(e -> {
            String currentTitle = historyList.getSelectedValue();
            if (currentTitle == null) return;

            String newTitle = JOptionPane.showInputDialog(
                    UserInterface.this,
                    "Enter a new title for the chat:",
                    currentTitle
            );

            if (newTitle != null && !newTitle.trim().isEmpty()) {
                String conversationId = conversationMap.get(currentTitle);

                try {
                    messageStorage.updateConversationTitle(conversationId, newTitle.trim());
                    loadHistory(); // refresh sidebar
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(UserInterface.this,
                            "Failed to rename chat: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });


// Show menu on right-click
        historyList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) showMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) showMenu(e);
            }

            private void showMenu(MouseEvent e) {
                int index = historyList.locationToIndex(e.getPoint());
                if (index >= 0) {
                    historyList.setSelectedIndex(index); // ensure selection
                    historyMenu.show(historyList, e.getX(), e.getY());
                }
            }
        });
    }

    private void layoutComponents() {
        JPanel configPanel = new JPanel();
        configPanel.add(new JLabel("LLMStudio IP:"));
        configPanel.add(ipField);
        configPanel.add(new JLabel("Port:"));
        configPanel.add(portField);
        configPanel.add(dispatchButton);
        configPanel.add(new JLabel("Model:"));
        configPanel.add(modelDropdown);
        configPanel.add(newChatButton);


        JPanel inputPanel = new JPanel(new BorderLayout());
        JPanel stackedInput = new JPanel();
        stackedInput.setLayout(new BoxLayout(stackedInput, BoxLayout.Y_AXIS));
        stackedInput.add(new JLabel("System Message:"));
        stackedInput.add(systemInputField);
        stackedInput.add(new JLabel("User Message:"));
        stackedInput.add(userInputField);
        inputPanel.add(stackedInput, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        JScrollPane scrollHistory = new JScrollPane(historyList);
        scrollHistory.setBorder(BorderFactory.createTitledBorder("Response History"));
        scrollHistory.setPreferredSize(new Dimension(250, 0));

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(configPanel, BorderLayout.NORTH);
        mainPanel.add(chatScrollPane, BorderLayout.CENTER);
        mainPanel.add(inputPanel, BorderLayout.SOUTH);
        mainPanel.add(scrollHistory, BorderLayout.EAST);

        add(mainPanel);
    }

    private void addMessage(String sender, String content) {
        JTextArea message = new JTextArea(sender + ":\n" + content);
        message.setLineWrap(true);
        message.setWrapStyleWord(true);
        message.setEditable(false);
        message.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        chatPanel.add(message);
        chatPanel.revalidate();
        chatScrollPane.getVerticalScrollBar().setValue(chatScrollPane.getVerticalScrollBar().getMaximum());
    }

    private void addListeners() {
        sendButton.addActionListener(e -> doDispatch());
        dispatchButton.addActionListener(e -> {
            String ip = ipField.getText().trim();
            String portText = portField.getText().trim();
            try {
                int port = Integer.parseInt(portText);
                loadModels(ip, port);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                        "Invalid port number.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        newChatButton.addActionListener(e -> startNewChat());
    }

    private void startNewChat() {
        messageHistory.clear();
        chatPanel.removeAll();
        chatPanel.revalidate();
        chatPanel.repaint();

        currentConversationId = UUID.randomUUID().toString();

        String title = "Chat on " + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        try {
            messageStorage.registerConversation(currentConversationId, title);
        } catch (SQLException e) {
            addMessage("Error", "Could not register conversation: " + e.getMessage());
        }
        addMessage("System", "Started new conversation: " + title);
        loadHistory();
    }

    private void doDispatch() {
        final String ip = ipField.getText().trim();
        final String portText = portField.getText().trim();
        final String userMessage = userInputField.getText().trim();
        final String systemMessage = systemInputField.getText().trim();
        final String selectedModel = (String) modelDropdown.getSelectedItem();

        if (ip.isEmpty() || portText.isEmpty() || userMessage.isEmpty() || selectedModel == null || selectedModel.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please fill in IP, Port, select a model, and enter a user message.",
                    "Missing Input", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Invalid port number.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!systemMessage.isEmpty()) {
            addMessage("System", systemMessage);
            JSONObject sysMsg = new JSONObject();
            sysMsg.put("role", "system");
            sysMsg.put("content", systemMessage);
            messageHistory.add(sysMsg);
        }

        addMessage("User", userMessage);
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messageHistory.add(userMsg);

        userInputField.setText("");
        systemInputField.setText("");
        sendButton.setEnabled(false);

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                JSONObject requestJson = new JSONObject();
                requestJson.put("conversation_id", currentConversationId);
                requestJson.put("model", selectedModel);
                requestJson.put("messages", new JSONArray(messageHistory));
                return dispatcher.dispatch(requestJson.toString(), ip, port);
            }

            @Override
            protected void done() {
                sendButton.setEnabled(true);
                try {
                    String response = get();
                    JSONObject json = new JSONObject(response);
                    if (json.has("choices")) {
                        JSONArray choices = json.getJSONArray("choices");
                        for (int i = 0; i < choices.length(); i++) {
                            JSONObject choice = choices.getJSONObject(i);
                            if (choice.has("message")) {
                                JSONObject msg = choice.getJSONObject("message");
                                String role = msg.optString("role", "LLM");
                                String content = msg.optString("content", "");
                                addMessage(role, content);

                                // Append LLM response to message history
                                JSONObject llmMsg = new JSONObject();
                                llmMsg.put("role", role);
                                llmMsg.put("content", content);
                                messageHistory.add(llmMsg);
                            }
                        }
                    } else {
                        addMessage("LLM", json.toString(2));
                    }
                    loadHistory();
                } catch (Exception ex) {
                    addMessage("Error", ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void loadModels(String ip, int port) {
        SwingWorker<List<String>, Void> modelLoader = new SwingWorker<>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                LLMClient client = new LLMClient(ip, port);
                return client.fetchAvailableModels();
            }

            @Override
            protected void done() {
                try {
                    List<String> models = get();
                    modelDropdown.removeAllItems();
                    for (String model : models) {
                        modelDropdown.addItem(model);
                    }
                    if (!models.isEmpty()) {
                        modelDropdown.setSelectedIndex(0);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(UserInterface.this,
                            "Failed to load models: " + e.getMessage(),
                            "Model Load Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        modelLoader.execute();
    }

    private void loadHistory() {
        historyListModel.clear();
        conversationMap.clear();

        try {
            List<MessageStorage.ConversationSummary> convos = messageStorage.getAllConversations();
            for (MessageStorage.ConversationSummary convo : convos) {
                historyListModel.addElement(convo.title);
                conversationMap.put(convo.title, convo.id);
            }
        } catch (Exception e) {
            addMessage("Error", "Failed to load history: " + e.getMessage());
        }
    }

    private void loadSelectedHistory(String conversationId) {
        if (conversationId == null || conversationId.isEmpty()) return;

        try {
            chatPanel.removeAll();
            chatPanel.revalidate();
            chatPanel.repaint();
            messageHistory.clear();

            List<MessageStorage.ResponseRecord> records = messageStorage.getResponsesByConversationId(conversationId);

            for (MessageStorage.ResponseRecord record : records) {
                JSONObject request = new JSONObject(record.requestJson);
                JSONArray messages = request.optJSONArray("messages");

                if (messages != null) {
                    for (int i = 0; i < messages.length(); i++) {
                        JSONObject msg = messages.getJSONObject(i);
                        String role = msg.optString("role", "unknown");
                        String content = msg.optString("content", "");
                        addMessage(role, content);
                        messageHistory.add(msg);  // rebuild message history
                    }
                }

                // Also show the assistant response
                JSONObject response = new JSONObject(record.responseContent);
                JSONArray choices = response.optJSONArray("choices");
                if (choices != null) {
                    for (int i = 0; i < choices.length(); i++) {
                        JSONObject choice = choices.getJSONObject(i);
                        JSONObject msg = choice.optJSONObject("message");
                        if (msg != null) {
                            String role = msg.optString("role", "assistant");
                            String content = msg.optString("content", "");
                            addMessage(role, content);
                            messageHistory.add(msg);
                        }
                    }
                }
            }

            currentConversationId = conversationId;

        } catch (Exception e) {
            addMessage("Error", "Failed to load full conversation: " + e.getMessage());
            e.printStackTrace();
        }
    }


}
