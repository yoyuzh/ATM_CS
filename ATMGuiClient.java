import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public class ATMGuiClient extends JFrame {
    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final String DEFAULT_PORT = "2525";

    private final JTextField hostField = new JTextField(DEFAULT_HOST, 14);
    private final JTextField portField = new JTextField(DEFAULT_PORT, 6);
    private final JTextField cardField = new JTextField(14);
    private final JPasswordField passwordField = new JPasswordField(14);
    private final JTextField withdrawField = new JTextField(10);
    private final JLabel statusLabel = new JLabel("未连接");
    private final JTextArea logArea = new JTextArea(12, 46);

    private final JButton loginButton = new JButton("连接并登录");
    private final JButton balanceButton = new JButton("查询余额");
    private final JButton withdrawButton = new JButton("取款");
    private final JButton quitButton = new JButton("退出登录");

    private Socket socket;
    private BufferedReader serverReader;
    private PrintWriter serverWriter;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ATMGuiClient client = new ATMGuiClient();
            client.setVisible(true);
        });
    }

    public ATMGuiClient() {
        super("ATM 图形客户端");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(560, 420));

        buildLayout();
        bindActions();
        setLoggedIn(false);
        pack();
        setLocationRelativeTo(null);
    }

    private void buildLayout() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        setContentPane(root);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("登录配置"));
        root.add(formPanel, BorderLayout.NORTH);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        addFormRow(formPanel, gbc, 0, "服务器IP", hostField, "端口", portField);
        addFormRow(formPanel, gbc, 1, "卡号", cardField, "密码", passwordField);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 4;
        gbc.fill = GridBagConstraints.NONE;
        formPanel.add(loginButton, gbc);

        JPanel operationPanel = new JPanel(new GridBagLayout());
        operationPanel.setBorder(BorderFactory.createTitledBorder("账户操作"));
        root.add(operationPanel, BorderLayout.CENTER);

        gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        operationPanel.add(balanceButton, gbc);

        gbc.gridx = 1;
        operationPanel.add(new JLabel("取款金额"), gbc);

        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        operationPanel.add(withdrawField, gbc);

        gbc.gridx = 3;
        gbc.fill = GridBagConstraints.NONE;
        operationPanel.add(withdrawButton, gbc);

        gbc.gridx = 4;
        operationPanel.add(quitButton, gbc);

        JPanel bottomPanel = new JPanel(new BorderLayout(8, 8));
        root.add(bottomPanel, BorderLayout.SOUTH);

        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("通信记录"));
        bottomPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        statusPanel.add(statusLabel, BorderLayout.WEST);
        bottomPanel.add(statusPanel, BorderLayout.SOUTH);
    }

    private void addFormRow(
        JPanel panel,
        GridBagConstraints gbc,
        int row,
        String firstLabel,
        JTextField firstField,
        String secondLabel,
        JTextField secondField
    ) {
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;

        gbc.gridx = 0;
        panel.add(new JLabel(firstLabel), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(firstField, gbc);

        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel(secondLabel), gbc);

        gbc.gridx = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(secondField, gbc);
    }

    private void bindActions() {
        loginButton.addActionListener(event -> login());
        balanceButton.addActionListener(event -> queryBalance());
        withdrawButton.addActionListener(event -> withdraw());
        quitButton.addActionListener(event -> logout());
    }

    private void login() {
        String host = hostField.getText().trim();
        int port = parsePort(portField.getText().trim());
        String cardNo = cardField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (host.isEmpty()) {
            showError("服务器IP不能为空。");
            return;
        }
        if (port == -1) {
            showError("端口号应为 1~65535。");
            return;
        }
        if (cardNo.isEmpty() || password.isEmpty()) {
            showError("卡号和密码不能为空。");
            return;
        }

        runTask("正在连接服务器...", () -> {
            closeConnection();
            socket = new Socket(host, port);
            serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            serverWriter = new PrintWriter(socket.getOutputStream(), true);

            String heloResponse = sendAndReceive("HELO " + cardNo);
            if (!"500 AUTH REQUIRE".equals(heloResponse)) {
                closeConnection();
                throw new IOException("认证流程启动失败: " + heloResponse);
            }

            String passResponse = sendAndReceive("PASS " + password);
            if (!"525 OK!".equals(passResponse)) {
                closeConnection();
                throw new IOException("登录失败: " + passResponse);
            }

            SwingUtilities.invokeLater(() -> {
                setLoggedIn(true);
                statusLabel.setText("已登录 " + host + ":" + port + "，卡号 " + cardNo);
                appendLog("登录成功。");
            });
            return null;
        });
    }

    private void queryBalance() {
        runTask("正在查询余额...", () -> {
            String response = sendAndReceive("BALA");
            if (response.startsWith("AMNT:")) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("当前余额: " + response.substring(5) + " 元");
                    appendLog("当前余额: " + response.substring(5) + " 元");
                });
                return null;
            }
            throw new IOException("查询余额失败: " + response);
        });
    }

    private void withdraw() {
        String amount = withdrawField.getText().trim();
        if (!isPositiveAmount(amount)) {
            showError("取款金额必须是大于 0 的数字。");
            return;
        }

        runTask("正在取款...", () -> {
            String response = sendAndReceive("WDRA " + amount);
            if ("525 OK!".equals(response)) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("取款成功。");
                    appendLog("取款成功，金额: " + amount + " 元");
                    withdrawField.setText("");
                });
                return null;
            }
            throw new IOException("取款失败，请检查余额或金额: " + response);
        });
    }

    private void logout() {
        runTask("正在退出登录...", () -> {
            if (serverWriter != null) {
                sendAndReceive("QUIT");
            }
            closeConnection();
            SwingUtilities.invokeLater(() -> {
                setLoggedIn(false);
                statusLabel.setText("已退出登录");
                appendLog("已退出登录。");
            });
            return null;
        });
    }

    private String sendAndReceive(String command) throws IOException {
        if (serverReader == null || serverWriter == null) {
            throw new IOException("尚未连接服务器。");
        }

        appendLog("客户端 -> " + command);
        serverWriter.println(command);

        String response = serverReader.readLine();
        if (response == null) {
            closeConnection();
            throw new IOException("服务器已断开连接。");
        }

        appendLog("服务器 -> " + response);
        return response;
    }

    private void runTask(String busyMessage, ClientTask task) {
        setButtonsEnabled(false);
        statusLabel.setText(busyMessage);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                task.run();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                } catch (Exception e) {
                    handleTaskError(e);
                } finally {
                    setButtonsEnabled(true);
                    setLoggedIn(isConnected());
                }
            }
        }.execute();
    }

    private void handleTaskError(Exception e) {
        Throwable cause = e.getCause() == null ? e : e.getCause();
        statusLabel.setText("操作失败");
        appendLog("错误: " + cause.getMessage());
        showError(cause.getMessage());
        if (!isConnected()) {
            setLoggedIn(false);
        }
    }

    private void setLoggedIn(boolean loggedIn) {
        hostField.setEnabled(!loggedIn);
        portField.setEnabled(!loggedIn);
        cardField.setEnabled(!loggedIn);
        passwordField.setEnabled(!loggedIn);
        loginButton.setEnabled(!loggedIn);

        balanceButton.setEnabled(loggedIn);
        withdrawField.setEnabled(loggedIn);
        withdrawButton.setEnabled(loggedIn);
        quitButton.setEnabled(loggedIn);
    }

    private void setButtonsEnabled(boolean enabled) {
        loginButton.setEnabled(enabled);
        balanceButton.setEnabled(enabled);
        withdrawButton.setEnabled(enabled);
        quitButton.setEnabled(enabled);
    }

    private boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    private void closeConnection() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
        socket = null;
        serverReader = null;
        serverWriter = null;
    }

    private int parsePort(String rawPort) {
        try {
            int port = Integer.parseInt(rawPort);
            if (port < 1 || port > 65535) {
                return -1;
            }
            return port;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private boolean isPositiveAmount(String rawAmount) {
        try {
            return Double.parseDouble(rawAmount) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + System.lineSeparator());
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "错误", JOptionPane.ERROR_MESSAGE);
    }

    @FunctionalInterface
    private interface ClientTask {
        Void run() throws Exception;
    }
}
