import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ATMServer {
    private static final int DEFAULT_PORT = 2525;
    private static final String USERS_FILE = "users.txt";
    private static final String BALANCES_FILE = "balances.txt";

    private static final Map<String, String> USER_PASSWORDS = new ConcurrentHashMap<>();
    private static final Map<String, Double> USER_BALANCES = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        int port = parsePort(args);
        loadUserPasswords();
        loadUserBalances();

        ExecutorService pool = Executors.newCachedThreadPool();
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("ATM服务器已启动，监听端口 " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("客户端连接: " + clientSocket.getRemoteSocketAddress());
                pool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("服务器异常: " + e.getMessage());
        } finally {
            pool.shutdown();
        }
    }

    private static int parsePort(String[] args) {
        if (args.length == 0) {
            return DEFAULT_PORT;
        }

        try {
            int port = Integer.parseInt(args[0]);
            if (port < 1 || port > 65535) {
                System.err.println("端口号应在 1~65535 之间，将使用默认端口 " + DEFAULT_PORT);
                return DEFAULT_PORT;
            }
            return port;
        } catch (NumberFormatException e) {
            System.err.println("无效的端口号，将使用默认端口 " + DEFAULT_PORT);
            return DEFAULT_PORT;
        }
    }

    private static void loadUserPasswords() {
        USER_PASSWORDS.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(USERS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                String[] parts = trimmed.split("\\s+");
                if (parts.length == 2) {
                    USER_PASSWORDS.put(parts[0], parts[1]);
                }
            }
        } catch (IOException e) {
            System.err.println("读取 " + USERS_FILE + " 失败: " + e.getMessage());
        }
    }

    private static void loadUserBalances() {
        USER_BALANCES.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(BALANCES_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                String[] parts = trimmed.split("\\s+");
                if (parts.length != 2) {
                    continue;
                }

                try {
                    USER_BALANCES.put(parts[0], Double.parseDouble(parts[1]));
                } catch (NumberFormatException ignored) {
                    System.err.println("忽略非法余额记录: " + trimmed);
                }
            }
        } catch (IOException e) {
            System.err.println("读取 " + BALANCES_FILE + " 失败: " + e.getMessage());
        }
    }

    private static synchronized void saveBalances() {
        Map<String, Double> sortedBalances = new TreeMap<>(USER_BALANCES);
        try (PrintWriter writer = new PrintWriter(new FileWriter(BALANCES_FILE))) {
            for (Map.Entry<String, Double> entry : sortedBalances.entrySet()) {
                writer.printf("%s %.2f%n", entry.getKey(), entry.getValue());
            }
        } catch (IOException e) {
            System.err.println("保存 " + BALANCES_FILE + " 失败: " + e.getMessage());
        }
    }

    private enum SessionState {
        INIT,
        AUTH_REQUIRED,
        LOGGED_IN
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private String currentCard;
        private SessionState state = SessionState.INIT;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter output = new PrintWriter(socket.getOutputStream(), true)
            ) {
                String line;
                while ((line = input.readLine()) != null) {
                    String command = line.trim();
                    if (command.isEmpty()) {
                        output.println("401 ERROR!");
                        continue;
                    }

                    System.out.println("收到[" + socket.getRemoteSocketAddress() + "]: " + command);
                    String response = processCommand(command);
                    output.println(response);
                    System.out.println("回复[" + socket.getRemoteSocketAddress() + "]: " + response);

                    if ("BYE".equals(response)) {
                        break;
                    }
                }
            } catch (IOException e) {
                System.err.println("客户端处理异常: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }

        private String processCommand(String rawCommand) {
            String[] parts = rawCommand.split("\\s+", 2);
            String command = parts[0].toUpperCase();

            return switch (command) {
                case "HELO" -> handleHelo(parts);
                case "PASS" -> handlePass(parts);
                case "BALA" -> handleBala(parts);
                case "WDRA" -> handleWdra(parts);
                case "QUIT" -> handleQuit(parts);
                default -> "401 ERROR!";
            };
        }

        private String handleHelo(String[] parts) {
            if (state != SessionState.INIT || parts.length != 2) {
                return "401 ERROR!";
            }

            String cardNo = parts[1].trim();
            if (!USER_PASSWORDS.containsKey(cardNo)) {
                return "401 ERROR!";
            }

            currentCard = cardNo;
            state = SessionState.AUTH_REQUIRED;
            return "500 AUTH REQUIRE";
        }

        private String handlePass(String[] parts) {
            if (state != SessionState.AUTH_REQUIRED || currentCard == null || parts.length != 2) {
                return "401 ERROR!";
            }

            String password = parts[1].trim();
            if (!USER_PASSWORDS.getOrDefault(currentCard, "").equals(password)) {
                return "401 ERROR!";
            }

            state = SessionState.LOGGED_IN;
            return "525 OK!";
        }

        private String handleBala(String[] parts) {
            if (state != SessionState.LOGGED_IN || parts.length != 1 || currentCard == null) {
                return "401 ERROR!";
            }

            Double balance = USER_BALANCES.get(currentCard);
            if (balance == null) {
                return "401 ERROR!";
            }

            return String.format("AMNT:%.2f", balance);
        }

        private String handleWdra(String[] parts) {
            if (state != SessionState.LOGGED_IN || parts.length != 2 || currentCard == null) {
                return "401 ERROR!";
            }

            double amount;
            try {
                amount = Double.parseDouble(parts[1].trim());
            } catch (NumberFormatException e) {
                return "401 ERROR!";
            }

            if (amount <= 0) {
                return "401 ERROR!";
            }

            synchronized (ATMServer.class) {
                Double currentBalance = USER_BALANCES.get(currentCard);
                if (currentBalance == null || currentBalance < amount) {
                    return "401 ERROR!";
                }

                USER_BALANCES.put(currentCard, currentBalance - amount);
                saveBalances();
            }

            return "525 OK!";
        }

        private String handleQuit(String[] parts) {
            if (parts.length != 1 || (state != SessionState.AUTH_REQUIRED && state != SessionState.LOGGED_IN)) {
                return "401 ERROR!";
            }

            currentCard = null;
            state = SessionState.INIT;
            return "BYE";
        }
    }
}
