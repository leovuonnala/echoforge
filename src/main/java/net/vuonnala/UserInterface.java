package net.vuonnala;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.SwingWorker;

public class UserInterface extends JFrame {

    private final MessageDispatcher dispatcher;
    private final MessageValidator validator;

    private JTextField ipField;
    private JTextField portField;
    private JButton dispatchButton;
    private JTextArea resultArea;
    private MessageBuilderUI messageBuilderUI;

    public UserInterface(MessageDispatcher dispatcher, MessageValidator validator) {
        super("LLM Message Dispatch Tool");
        this.dispatcher = dispatcher;
        this.validator = validator;

        initComponents();
        layoutComponents();
        initMenu();
        addListeners();

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
        dispatchButton = new JButton("Dispatch to LLMStudio");
        resultArea = new JTextArea(10, 50);
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);

        messageBuilderUI = new MessageBuilderUI();
    }

    private void layoutComponents() {
        JPanel configPanel = new JPanel();
        configPanel.add(new JLabel("LLMStudio IP:"));
        configPanel.add(ipField);
        configPanel.add(new JLabel("Port:"));
        configPanel.add(portField);
        configPanel.add(dispatchButton);

        JScrollPane scrollResult = new JScrollPane(resultArea);
        scrollResult.setBorder(BorderFactory.createTitledBorder("LLMStudio Response"));

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(configPanel, BorderLayout.NORTH);
        mainPanel.add(messageBuilderUI, BorderLayout.CENTER);
        mainPanel.add(scrollResult, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void addListeners() {
        dispatchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doDispatch();
            }
        });
    }

    private void doDispatch() {
        final String ip = ipField.getText().trim();
        final String portText = portField.getText().trim();
        final String jsonContent = messageBuilderUI.getFinalJson();

        if (ip.isEmpty() || portText.isEmpty() || jsonContent.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please fill in IP, Port, and ensure message content is not empty.",
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

        dispatchButton.setEnabled(false);
        resultArea.setText("Dispatching... please wait.\n");

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                return dispatcher.dispatch(jsonContent, ip, port);
            }

            @Override
            protected void done() {
                dispatchButton.setEnabled(true);
                try {
                    String response = get();
                    resultArea.setText(response);
                } catch (Exception ex) {
                    resultArea.setText("Error: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }
}
