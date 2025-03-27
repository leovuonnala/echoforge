package net.vuonnala;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.SwingWorker;

public class UserInterface extends JFrame {

    private final MessageDispatcher dispatcher;
    private final MessageValidator validator;

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

    public UserInterface(MessageDispatcher dispatcher, MessageValidator validator) {
        super("LLM Message Dispatch Tool");
        this.dispatcher = dispatcher;
        this.validator = validator;

        initComponents();
        layoutComponents();
        initMenu();
        addListeners();
        loadHistory();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
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
                loadSelectedHistory(historyList.getSelectedValue());
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
        }
        addMessage("User", userMessage);
        userInputField.setText("");
        systemInputField.setText("");

        sendButton.setEnabled(false);

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                StringBuilder jsonBuilder = new StringBuilder();
                jsonBuilder.append("{\"model\": \"").append(selectedModel).append("\", \"messages\": [");
                if (!systemMessage.isEmpty()) {
                    jsonBuilder.append("{\"role\": \"system\", \"content\": \"")
                            .append(systemMessage.replace("\"", "\\"))
                            .append("\"},");
                }
                jsonBuilder.append("{\"role\": \"user\", \"content\": \"")
                        .append(userMessage.replace("\"", "\\"))
                        .append("\"}]");
                jsonBuilder.append("}");
                return dispatcher.dispatch(jsonBuilder.toString(), ip, port);
            }

            @Override
            protected void done() {
                sendButton.setEnabled(true);
                try {
                    String response = get();
                    org.json.JSONObject json = new org.json.JSONObject(response);
                    if (json.has("choices")) {
                        org.json.JSONArray choices = json.getJSONArray("choices");
                        for (int i = 0; i < choices.length(); i++) {
                            org.json.JSONObject choice = choices.getJSONObject(i);
                            if (choice.has("message")) {
                                org.json.JSONObject msg = choice.getJSONObject("message");
                                String role = msg.optString("role", "LLM");
                                String content = msg.optString("content", "");
                                addMessage(role, content);
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
        try (Stream<Path> paths = Files.walk(Path.of("responses"))) {
            List<String> files = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .map(Path::toString)
                    .sorted()
                    .collect(Collectors.toList());
            for (String path : files) {
                historyListModel.addElement(path);
            }
        } catch (IOException ignored) {}
    }

    private void loadSelectedHistory(String filePath) {
        if (filePath == null) return;
        try {
            String fileContent = Files.readString(Path.of(filePath));
            addMessage("File", fileContent);
        } catch (IOException e) {
            addMessage("Error", "Failed to load file: " + e.getMessage());
        }
    }
}
