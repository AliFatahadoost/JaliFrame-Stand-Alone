package JaliFrame.ConfigAndLauncherManager;

import com.sun.net.httpserver.HttpServer;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.sql.*;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import JaliFrame.WebServerHandlers.pageHandlerOpener;
import JaliFrame.PageRelatedEnums.FilesEnum;
import JaliFrame.PageRelatedEnums.WebPagesEnum;
import JaliFrame.WebServerHandlers.apiManagement;
import JaliFrame.PageRelatedEnums.CrudQueriesEnum;
import JaliFrame.DataBase.DataBaseInit;

public class readConfig {

    // ──────────── Configurable properties (public static) ────────────
    public static String BASE_FILE_ADDRESS = Paths.get("").toAbsolutePath().resolve("./../../../ClientSide").normalize().toString();
    public static int MAX_SESSION_TIME = 86400;
    public static String server = "localhost";          // Database host
    public static int port = 1433;                      // Database port
    public static String databaseName = "PROJECT_ZERO";
    public static String username = "sa";
    public static String password = "12";
    public static int MAX_CONNECTION_POOL = 5;
    public static int portNumber = 8080;               // HTTP server port
    public static int queueWaitLine = 10;               // HTTP backlog
    // NEW: IP address the HTTP server will bind to / advertise
    public static String serverIP = "127.0.0.1";

    // Derived URL for database (SQL Server)
    public static String url = buildURL();

    // Derived base URL for HTTP clients (computed from serverIP & portNumber)
    public static String serversBaseUrl = "";

    // ──────────── Internal config file path ────────────
    private static String configFilePath = System.getProperty("user.dir") +
                                           File.separator + "config.txt";

    // Server state
    private static final AtomicBoolean serverRunning = new AtomicBoolean(false);
    private static volatile HttpServer httpServer;

    // ──────────── Entry point ────────────
    public static HttpServer initiate() {
        
        // Try to locate config.txt next to the JAR, otherwise use default path
        try {
            String jarDir = new File(readConfig.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).getParent();
            Path possiblePath = Paths.get(jarDir, "config.txt");
            if (Files.exists(possiblePath)) {
                configFilePath = possiblePath.toString();
            }
        } catch (Exception ignored) {}

        // Load existing config or create default
        if (!loadConfig()) {
            System.out.println("Config file not found. Creating default config.txt ...");
            saveConfig();
        }
        // Ensure derived fields are in sync
        updateServersBaseUrl();
        
        // Ask for GUI or console mode
        Scanner scanner = new Scanner(System.in);
        String input;
        do {
            System.out.print("Launch GUI server configuration? (Y/n/s S stands for start with no questions): ");
            input = scanner.nextLine().trim();
        } while (!(input.equalsIgnoreCase("y") || input.equalsIgnoreCase("n") || input.equalsIgnoreCase("s") || input.isEmpty()));
        
        if(input.equalsIgnoreCase("s"))
        {
            try {
                return launchHttpServer();
            } catch (IOException ex) {
                Logger.getLogger(readConfig.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        boolean gui = input.equalsIgnoreCase("y") || input.isEmpty();

        if (gui) {
            launchGui();
        } else {
            launchConsole();
        }/**/

        return httpServer;
    }

    // ──────────── File I/O ────────────
    public static boolean loadConfig() {
        File file = new File(configFilePath);
        if (!file.exists()) return false;

        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(file)) {
            props.load(fis);
        } catch (IOException e) {
            System.err.println("Error reading config file: " + e.getMessage());
            return false;
        }

        BASE_FILE_ADDRESS = props.getProperty("BASE_FILE_ADDRESS", BASE_FILE_ADDRESS);
        MAX_SESSION_TIME = Integer.parseInt(props.getProperty("MAX_SESSION_TIME", "86400"));
        server = props.getProperty("server", "localhost");
        port = Integer.parseInt(props.getProperty("port", "1433"));
        databaseName = props.getProperty("databaseName", "PROJECT_ZERO");
        username = props.getProperty("username", "sa");
        password = props.getProperty("password", "12");
        MAX_CONNECTION_POOL = Integer.parseInt(props.getProperty("MAX_CONNECTION_POOL", "5"));
        portNumber = Integer.parseInt(props.getProperty("portNumber", "55952"));
        queueWaitLine = Integer.parseInt(props.getProperty("queueWaitLine", "10"));
        // NEW: load serverIP from file
        serverIP = props.getProperty("serverIP", "127.0.0.1");

        rebuildURL();
        updateServersBaseUrl();   // keep serversBaseUrl in sync
        return true;
    }

    public static void saveConfig() {
        Properties props = new Properties();
        props.setProperty("BASE_FILE_ADDRESS", BASE_FILE_ADDRESS);
        props.setProperty("MAX_SESSION_TIME", String.valueOf(MAX_SESSION_TIME));
        props.setProperty("server", server);
        props.setProperty("port", String.valueOf(port));
        props.setProperty("databaseName", databaseName);
        props.setProperty("username", username);
        props.setProperty("password", password);
        props.setProperty("MAX_CONNECTION_POOL", String.valueOf(MAX_CONNECTION_POOL));
        props.setProperty("portNumber", String.valueOf(portNumber));
        props.setProperty("queueWaitLine", String.valueOf(queueWaitLine));
        // NEW: save serverIP
        props.setProperty("serverIP", serverIP);

        try (FileOutputStream fos = new FileOutputStream(configFilePath)) {
            props.store(fos, "Server Configuration");
            System.out.println("Config saved to " + configFilePath);
        } catch (IOException e) {
            System.err.println("Error saving config: " + e.getMessage());
        }
    }

    // Update the derived database URL
    private static void rebuildURL() {
        url = "jdbc:sqlserver://" + server + ":" + port +
              ";databaseName=" + databaseName +
              ";encrypt=true;trustServerCertificate=true";
    }

    private static String buildURL() {
        return "jdbc:sqlserver://" + server + ":" + port +
               ";databaseName=" + databaseName +
               ";encrypt=true;trustServerCertificate=true";
    }

    // NEW: recompute the servers base URL whenever IP or port changes
    private static void updateServersBaseUrl() {
        serversBaseUrl = "http://" + serverIP + ":" + portNumber;
    }

    // ──────────── Database test ────────────
    public static void testDatabaseConnection(JTextArea outputArea) {
        String result;
        StringBuilder stackTrace = new StringBuilder();
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            try (Connection conn = DriverManager.getConnection(url, username, password);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT 1")) {
                if (rs.next()) {
                    result = "✅ Database connection successful!";
                } else {
                    result = "❌ Connection established but query failed.";
                }
            }
        } catch (Exception ex) {
            result = "❌ Database connection failed: " + ex.getClass().getSimpleName() +
                     " - " + ex.getMessage();
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            stackTrace.append(sw.toString());
        }

        if (outputArea != null) {
            outputArea.append(result + "\n");
            if (stackTrace.length() > 0) {
                outputArea.append(stackTrace.toString());
            }
        } else {
            System.out.println(result);
            if (stackTrace.length() > 0) System.out.println(stackTrace.toString());
        }
    }

    // ──────────── Launch server (uses your existing method) ────────────
    public static void launchServer() {
        if (serverRunning.get()) {
            System.out.println("Server is already running.");
            return;
        }
        System.out.println("Launching HTTP server on " + serverIP + ":" + portNumber + " ...");
        new Thread(() -> {
            try {
                launchHttpServer();          // <-- your method, now binds to serverIP
                serverRunning.set(true);
            } catch (Exception e) {
                System.err.println("Failed to start server: " + e.getMessage());
                e.printStackTrace();
            }
        }, "HttpServer-Thread").start();
    }

    // ──────────── Stop server and exit application ────────────
    public static void shutdownEverything() {
        System.out.println("Shutting down...");
        System.exit(0);
    }

    // ──────────── Console interface ────────────
    private static void launchConsole() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Console configuration. Type 'help' for commands.");
        while (true) {
            System.out.print("> ");
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split("\\s+", 3);
            String cmd = parts[0].toLowerCase();

            switch (cmd) {
                case "help":
                    showConsoleHelp();
                    break;
                case "get":
                    if (parts.length < 2) { System.out.println("Usage: get <property>"); break; }
                    printProperty(parts[1]);
                    break;
                case "set":
                    if (serverRunning.get()) {
                        System.out.println("Cannot modify configuration while server is running.");
                        break;
                    }
                    if (parts.length < 3) { System.out.println("Usage: set <property> <value>"); break; }
                    setProperty(parts[1], parts[2]);
                    break;
                case "load":
                    if (loadConfig()) System.out.println("Configuration loaded.");
                    else System.out.println("Failed to load config file.");
                    break;
                case "save":
                    if (serverRunning.get()) {
                        System.out.println("Cannot save configuration while server is running.");
                        break;
                    }
                    saveConfig();
                    break;
                case "testdb":
                    System.out.println("Testing database connection...");
                    testDatabaseConnection(null);
                    break;
                case "launch":
                    launchServer();
                    break;
                case "end":
                case "exit":
                    shutdownEverything();
                    break;
                default:
                    System.out.println("Unknown command. Type 'help'.");
            }
        }
    }

    private static void showConsoleHelp() {
        System.out.println("Commands:");
        System.out.println("  get <property>        - show a property value");
        System.out.println("  set <property> <val>  - change a property (disabled if server running)");
        System.out.println("  load                  - reload from config.txt");
        System.out.println("  save                  - save to config.txt (disabled if server running)");
        System.out.println("  testdb                - test database connection");
        System.out.println("  launch                - start the HTTP server");
        System.out.println("  end / exit            - shut down server and exit");
        System.out.println("Properties: BASE_FILE_ADDRESS, MAX_SESSION_TIME, server, port,");
        System.out.println("           databaseName, username, password, MAX_CONNECTION_POOL,");
        System.out.println("           portNumber, queueWaitLine, serverIP");
    }

    private static void printProperty(String prop) {
        switch (prop.toLowerCase()) {
            case "base_file_address": System.out.println(BASE_FILE_ADDRESS); break;
            case "max_session_time": System.out.println(MAX_SESSION_TIME); break;
            case "server": System.out.println(server); break;
            case "port": System.out.println(port); break;
            case "databasename": System.out.println(databaseName); break;
            case "username": System.out.println(username); break;
            case "password": System.out.println(password); break;
            case "max_connection_pool": System.out.println(MAX_CONNECTION_POOL); break;
            case "portnumber": System.out.println(portNumber); break;
            case "queuewaitline": System.out.println(queueWaitLine); break;
            case "url": System.out.println(url); break;
            case "serverip": System.out.println(serverIP); break;
            case "serversbaseurl": System.out.println(serversBaseUrl); break;
            default: System.out.println("Unknown property."); break;
        }
    }

    private static void setProperty(String prop, String value) {
        try {
            switch (prop.toLowerCase()) {
                case "base_file_address": BASE_FILE_ADDRESS = value; break;
                case "max_session_time": MAX_SESSION_TIME = Integer.parseInt(value); break;
                case "server": server = value; rebuildURL(); break;
                case "port": port = Integer.parseInt(value); rebuildURL(); break;
                case "databasename": databaseName = value; rebuildURL(); break;
                case "username": username = value; break;
                case "password": password = value; break;
                case "max_connection_pool": MAX_CONNECTION_POOL = Integer.parseInt(value); break;
                case "portnumber":
                    portNumber = Integer.parseInt(value);
                    updateServersBaseUrl();
                    break;
                case "queuewaitline": queueWaitLine = Integer.parseInt(value); break;
                // NEW: allow setting server IP
                case "serverip":
                    serverIP = value;
                    updateServersBaseUrl();
                    break;
                default: System.out.println("Unknown property."); return;
            }
            System.out.println(prop + " set to " + value);
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format for " + prop);
        }
    }

    // ──────────── GUI (Swing) ────────────
    private static void launchGui() {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Server Configuration Manager");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(950, 750);
            frame.setLayout(new BorderLayout());

            // --- Form panel with text fields ---
            JPanel formPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            // Standard fields
            JTextField baseAddressField = addField(formPanel, gbc, "BASE_FILE_ADDRESS", BASE_FILE_ADDRESS);
            JTextField maxSessionField = addField(formPanel, gbc, "MAX_SESSION_TIME", String.valueOf(MAX_SESSION_TIME));
            JTextField serverField = addField(formPanel, gbc, "DB server", server);
            JTextField portField = addField(formPanel, gbc, "DB port", String.valueOf(port));
            JTextField dbNameField = addField(formPanel, gbc, "databaseName", databaseName);
            JTextField usernameField = addField(formPanel, gbc, "DB username", username);
            JTextField passwordField = addField(formPanel, gbc, "DB password", password);
            JTextField maxPoolField = addField(formPanel, gbc, "MAX_CONNECTION_POOL", String.valueOf(MAX_CONNECTION_POOL));
            JTextField httpPortField = addField(formPanel, gbc, "HTTP port", String.valueOf(portNumber));
            JTextField queueField = addField(formPanel, gbc, "queueWaitLine", String.valueOf(queueWaitLine));

            // ── NEW: Server IP combo box with custom option ──
            gbc.gridx = 0;
            gbc.gridy++;
            formPanel.add(new JLabel("Server IP:"), gbc);
            gbc.gridx = 1;
            JPanel ipPanel = new JPanel(new BorderLayout(5, 0));
            String[] ipPresets = {"127.0.0.1 (Localhost)", "0.0.0.0 (All Interfaces)", "Custom..."};
            JComboBox<String> ipCombo = new JComboBox<>(ipPresets);
            // Determine which preset matches current serverIP (ignore "Custom...")
            String currentIp = serverIP;
            if (currentIp.equals("127.0.0.1")) ipCombo.setSelectedIndex(0);
            else if (currentIp.equals("0.0.0.0")) ipCombo.setSelectedIndex(1);
            else {
                ipCombo.setSelectedIndex(2);  // Custom
            }
            JTextField customIpField = new JTextField(currentIp.equals("127.0.0.1") || currentIp.equals("0.0.0.0") ? "" : currentIp, 15);
            customIpField.setEnabled(ipCombo.getSelectedIndex() == 2);
            ipPanel.add(ipCombo, BorderLayout.WEST);
            ipPanel.add(customIpField, BorderLayout.CENTER);
            formPanel.add(ipPanel, gbc);

            // Combo listener: enable/disable custom field and set the IP value accordingly
            ipCombo.addActionListener(e -> {
                int idx = ipCombo.getSelectedIndex();
                if (idx == 0) {
                    customIpField.setEnabled(false);
                    customIpField.setText("");
                    serverIP = "127.0.0.1";
                } else if (idx == 1) {
                    customIpField.setEnabled(false);
                    customIpField.setText("");
                    serverIP = "0.0.0.0";
                } else {
                    customIpField.setEnabled(true);
                    serverIP = customIpField.getText().trim();
                }
                updateServersBaseUrl();
                // Update the base URL display (we'll update the read‑only field below)
                // We'll do that by calling a common method; for now just update the urlField later
            });

            // Read-only URL fields
            JLabel urlLabel = new JLabel("DB URL:");
            JTextField urlField = new JTextField(url, 40);
            urlField.setEditable(false);
            gbc.gridx = 0; gbc.gridy++;
            formPanel.add(urlLabel, gbc);
            gbc.gridx = 1;
            formPanel.add(urlField, gbc);

            // NEW: display derived serversBaseUrl
            JLabel baseUrlLabel = new JLabel("Server Base URL:");
            JTextField baseUrlField = new JTextField(serversBaseUrl, 40);
            baseUrlField.setEditable(false);
            gbc.gridx = 0; gbc.gridy++;
            formPanel.add(baseUrlLabel, gbc);
            gbc.gridx = 1;
            formPanel.add(baseUrlField, gbc);

            // --- Button panel (Load, Save, Test DB, Launch Server, End) ---
            JPanel buttonPanel = new JPanel();
            JButton loadBtn = new JButton("Load");
            JButton saveBtn = new JButton("Save");
            JButton testDbBtn = new JButton("Test Database");
            JButton launchBtn = new JButton("Launch Server");
            JButton endBtn = new JButton("END");
            endBtn.setBackground(Color.RED);
            endBtn.setForeground(Color.WHITE);

            buttonPanel.add(loadBtn);
            buttonPanel.add(saveBtn);
            buttonPanel.add(testDbBtn);
            buttonPanel.add(launchBtn);
            buttonPanel.add(endBtn);

            // --- Debugging panel (output + filter) ---
            JPanel debugPanel = new JPanel(new BorderLayout());
            JTextPane outputPane = new JTextPane();
            outputPane.setEditable(false);
            outputPane.setFont(new Font("Monospaced", Font.PLAIN, 12));
            JScrollPane scrollPane = new JScrollPane(outputPane);

            JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            filterPanel.add(new JLabel("Filter lines containing:"));
            JTextField filterField = new JTextField(20);
            JButton filterBtn = new JButton("Apply Filter");
            JButton clearFilterBtn = new JButton("Clear");
            filterPanel.add(filterField);
            filterPanel.add(filterBtn);
            filterPanel.add(clearFilterBtn);

            debugPanel.add(filterPanel, BorderLayout.NORTH);
            debugPanel.add(scrollPane, BorderLayout.CENTER);

            JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                    formPanel, debugPanel);
            splitPane.setDividerLocation(350);  // a bit more space for the new fields
            frame.add(splitPane, BorderLayout.CENTER);
            frame.add(buttonPanel, BorderLayout.SOUTH);

            // Helper to disable/enable configuration controls (except the combo and custom field)
            final java.util.List<JTextField> configFields = java.util.Arrays.asList(
                    baseAddressField, maxSessionField, serverField, portField, dbNameField,
                    usernameField, passwordField, maxPoolField, httpPortField, queueField
            );

            ActionListener disableConfigUI = e -> {
                boolean running = serverRunning.get();
                loadBtn.setEnabled(!running);
                saveBtn.setEnabled(!running);
                for (JTextField tf : configFields) tf.setEnabled(!running);
                ipCombo.setEnabled(!running);
                customIpField.setEnabled(!running && ipCombo.getSelectedIndex() == 2);
            };

            disableConfigUI.actionPerformed(null);

            // Refresh the read-only URL fields whenever they might change
            Runnable updateDerivedFields = () -> {
                urlField.setText(url);
                baseUrlField.setText(serversBaseUrl);
            };

            // --- Button Listeners ---
            loadBtn.addActionListener(e -> {
                if (loadConfig()) {
                    baseAddressField.setText(BASE_FILE_ADDRESS);
                    maxSessionField.setText(String.valueOf(MAX_SESSION_TIME));
                    serverField.setText(server);
                    portField.setText(String.valueOf(port));
                    dbNameField.setText(databaseName);
                    usernameField.setText(username);
                    passwordField.setText(password);
                    maxPoolField.setText(String.valueOf(MAX_CONNECTION_POOL));
                    httpPortField.setText(String.valueOf(portNumber));
                    queueField.setText(String.valueOf(queueWaitLine));
                    // Update IP combo
                    if (serverIP.equals("127.0.0.1")) ipCombo.setSelectedIndex(0);
                    else if (serverIP.equals("0.0.0.0")) ipCombo.setSelectedIndex(1);
                    else {
                        ipCombo.setSelectedIndex(2);
                        customIpField.setText(serverIP);
                    }
                    updateServersBaseUrl();
                    updateDerivedFields.run();
                    outputPane.setText("Configuration loaded.\n");
                } else {
                    outputPane.setText("Failed to load config.\n");
                }
            });

            saveBtn.addActionListener(e -> {
                // Read current values from fields back into static variables
                BASE_FILE_ADDRESS = baseAddressField.getText();
                MAX_SESSION_TIME = Integer.parseInt(maxSessionField.getText());
                server = serverField.getText();
                port = Integer.parseInt(portField.getText());
                databaseName = dbNameField.getText();
                username = usernameField.getText();
                password = passwordField.getText();
                MAX_CONNECTION_POOL = Integer.parseInt(maxPoolField.getText());
                portNumber = Integer.parseInt(httpPortField.getText());
                queueWaitLine = Integer.parseInt(queueField.getText());

                // IP already set by combo listener, but double-check if custom field is active
                if (ipCombo.getSelectedIndex() == 2) {
                    serverIP = customIpField.getText().trim();
                    if (serverIP.isEmpty()) serverIP = "127.0.0.1"; // fallback
                }
                // else serverIP already set by combo selection

                rebuildURL();
                updateServersBaseUrl();
                updateDerivedFields.run();
                saveConfig();
                outputPane.setText("Configuration saved.\n");
            });

            testDbBtn.addActionListener(e -> {
                outputPane.setText("");
                JTextArea tempArea = new JTextArea();
                testDatabaseConnection(tempArea);
                appendToPane(outputPane, tempArea.getText(), true);
            });

            launchBtn.addActionListener(e -> {
                if (serverRunning.get()) {
                    outputPane.setText("Server is already running.\n");
                    return;
                }
                outputPane.setText("Launching server...\n");
                new Thread(() -> {
                    try {
                        launchHttpServer();   // binds to the current serverIP
                        serverRunning.set(true);
                        SwingUtilities.invokeLater(() -> {
                            appendToPane(outputPane, "Server started on " + serverIP + ":" + portNumber + "\n", false);
                            disableConfigUI.actionPerformed(null);
                        });
                    } catch (Exception ex) {
                        StringWriter sw = new StringWriter();
                        ex.printStackTrace(new PrintWriter(sw));
                        SwingUtilities.invokeLater(() -> {
                            appendToPane(outputPane, "Error: " + sw.toString(), true);
                        });
                    }
                }, "HttpServer-Thread").start();
            });

            endBtn.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(frame,
                        "Are you sure you want to END everything?",
                        "Exit Confirmation", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    shutdownEverything();
                }
            });

            // Filter logic
            filterBtn.addActionListener(e -> {
                String filter = filterField.getText().trim();
                if (filter.isEmpty()) return;
                String fullText = outputPane.getText();
                StringBuilder filtered = new StringBuilder();
                for (String line : fullText.split("\n")) {
                    if (line.contains(filter)) {
                        filtered.append(line).append("\n");
                    }
                }
                outputPane.setText(filtered.toString());
            });

            clearFilterBtn.addActionListener(e -> {
                filterField.setText("");
            });

            frame.setVisible(true);
        });
    }

    // Helper to add a labeled text field and return the JTextField
    private static JTextField addField(JPanel panel, GridBagConstraints gbc,
                                       String label, String initialValue) {
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel(label + ":"), gbc);
        gbc.gridx = 1;
        JTextField textField = new JTextField(initialValue, 30);
        panel.add(textField, gbc);
        return textField;
    }

    private static void appendToPane(JTextPane pane, String text, boolean isError) {
        StyledDocument doc = pane.getStyledDocument();
        Style style = pane.addStyle("Style", null);
        if (isError) {
            StyleConstants.setForeground(style, Color.RED);
        } else {
            StyleConstants.setForeground(style, Color.BLACK);
        }
        try {
            doc.insertString(doc.getLength(), text, style);
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
    }

    // ──────────── Your existing HTTP server method ────────────
    private static HttpServer launchHttpServer() throws IOException {
        System.out.println("Server attempting to Lunch on " + serverIP + ":" + portNumber + " ...");
        // Bind to the configured IP (not just port)
        DataBaseInit.initBasicDataBaseActions();
        HttpServer server = HttpServer.create(new InetSocketAddress(serverIP, portNumber), queueWaitLine);


        
        server.createContext("/coreJs",                                                 new pageHandlerOpener(BASE_FILE_ADDRESS, FilesEnum.coreJs , false));
        server.createContext("/tableFormElement",                                       new pageHandlerOpener(BASE_FILE_ADDRESS, FilesEnum.tableFormElement , false));
        server.createContext("/dataCombo",                                              new pageHandlerOpener(BASE_FILE_ADDRESS, FilesEnum.dataCombo , false));
        server.createContext("/dataComboCss",                                           new pageHandlerOpener(BASE_FILE_ADDRESS, FilesEnum.dataComboCss , false));
        server.createContext("/FindObjectBoxCss",                                       new pageHandlerOpener(BASE_FILE_ADDRESS, FilesEnum.findObjectBoxCss , false));
        server.createContext("/findObjectBox",                                          new pageHandlerOpener(BASE_FILE_ADDRESS, FilesEnum.findObjectBox , false));
        server.createContext("/dateBox",                                                new pageHandlerOpener(BASE_FILE_ADDRESS, FilesEnum.dateBox , false));
        server.createContext("/jaliForm",                                               new pageHandlerOpener(BASE_FILE_ADDRESS, FilesEnum.jaliForm , false));
        server.createContext("/cssTableFormData",                                       new pageHandlerOpener(BASE_FILE_ADDRESS, FilesEnum.cssTableFormData , false));
        server.createContext("/cssDataForm",                                            new pageHandlerOpener(BASE_FILE_ADDRESS, FilesEnum.cssDataForm , false));
        server.createContext("/dateBoxCss",                                             new pageHandlerOpener(BASE_FILE_ADDRESS, FilesEnum.dateBoxCss , false));
        server.createContext("/leafletJs",                                              new pageHandlerOpener(readConfig.BASE_FILE_ADDRESS, FilesEnum.leafletJs,           false));
        server.createContext("/leafletCss",                                             new pageHandlerOpener(readConfig.BASE_FILE_ADDRESS, FilesEnum.leafletCss,          false));
        server.createContext("/leafletMarkerIcon",                                      new pageHandlerOpener(readConfig.BASE_FILE_ADDRESS, FilesEnum.leafletMarkerIcon,   false));
        server.createContext("/leafletMarkerIcon2x",                                    new pageHandlerOpener(readConfig.BASE_FILE_ADDRESS, FilesEnum.leafletMarkerIcon2x, false));
        server.createContext("/leafletMarkerShadow",                                    new pageHandlerOpener(readConfig.BASE_FILE_ADDRESS, FilesEnum.leafletMarkerShadow, false));
        server.createContext("/mapBox",                                                 new pageHandlerOpener(readConfig.BASE_FILE_ADDRESS, FilesEnum.mapBox,              false));
        server.createContext("/mapBoxCss",                                              new pageHandlerOpener(readConfig.BASE_FILE_ADDRESS, FilesEnum.mapBoxCss,           false));
        server.createContext("/fleetTripTable",
            new pageHandlerOpener(
                readConfig.BASE_FILE_ADDRESS,
                FilesEnum.fleetTripTable,
                false
            ));
        
        
        server.setExecutor(null);
        server.start();
        httpServer = server;
        return server;
    }
}