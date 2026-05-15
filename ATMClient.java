import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ATMClient {
    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 2525;

    public static void main(String[] args) {
        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;

        if (args.length >= 1) {
            host = args[0];
        }
        if (args.length >= 2) {
            port = parsePort(args[1]);
        }

        try (
            Socket socket = new Socket(host, port);
            BufferedReader serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter serverWriter = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in))
        ) {
            System.out.println("已连接到ATM服务器 " + host + ":" + port);
            if (!authenticate(consoleReader, serverReader, serverWriter)) {
                return;
            }
            runMenuLoop(consoleReader, serverReader, serverWriter);
        } catch (IOException e) {
            System.err.println("客户端异常: " + e.getMessage());
        }
    }

    private static int parsePort(String rawPort) {
        try {
            int port = Integer.parseInt(rawPort);
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

    private static boolean authenticate(
        BufferedReader consoleReader,
        BufferedReader serverReader,
        PrintWriter serverWriter
    ) throws IOException {
        System.out.print("请输入卡号: ");
        String cardNo = readConsoleLine(consoleReader);
        if (cardNo == null || cardNo.isBlank()) {
            System.out.println("卡号不能为空。");
            return false;
        }

        serverWriter.println("HELO " + cardNo.trim());
        String response = serverReader.readLine();
        if (!"500 AUTH REQUIRE".equals(response)) {
            System.out.println("认证流程启动失败: " + response);
            return false;
        }

        while (true) {
            System.out.println("服务器要求验证口令: " + response);
            System.out.print("请输入口令: ");
            String password = readConsoleLine(consoleReader);
            if (password == null) {
                return false;
            }

            serverWriter.println("PASS " + password.trim());
            response = serverReader.readLine();
            if ("525 OK!".equals(response)) {
                System.out.println("认证成功！");
                return true;
            }

            if (!"401 ERROR!".equals(response)) {
                System.out.println("服务器返回异常响应: " + response);
                return false;
            }

            System.out.print("认证失败。输入 y 重试，其他任意键退出: ");
            String retry = readConsoleLine(consoleReader);
            if (retry == null || !"y".equalsIgnoreCase(retry.trim())) {
                serverWriter.println("QUIT");
                String quitResponse = serverReader.readLine();
                if ("BYE".equals(quitResponse)) {
                    System.out.println("服务器已结束会话。");
                }
                return false;
            }
        }
    }

    private static void runMenuLoop(
        BufferedReader consoleReader,
        BufferedReader serverReader,
        PrintWriter serverWriter
    ) throws IOException {
        while (true) {
            System.out.println();
            System.out.println("可选操作: BALA(查询余额)  WDRA <金额>  QUIT(退出)");
            System.out.print("> ");
            String input = readConsoleLine(consoleReader);
            if (input == null) {
                return;
            }

            String trimmed = input.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            serverWriter.println(trimmed);
            String response = serverReader.readLine();
            if (response == null) {
                System.out.println("服务器已断开连接。");
                return;
            }

            if (response.startsWith("AMNT:")) {
                System.out.println("当前余额: " + response.substring(5) + " 元");
                continue;
            }

            if ("525 OK!".equals(response)) {
                System.out.println("操作成功！");
                continue;
            }

            if ("401 ERROR!".equals(response)) {
                System.out.println("操作失败！请检查命令格式、认证状态或余额。");
                continue;
            }

            if ("BYE".equals(response)) {
                System.out.println("服务器已结束会话，感谢使用。");
                return;
            }

            System.out.println("未知响应: " + response);
        }
    }

    private static String readConsoleLine(BufferedReader consoleReader) throws IOException {
        return consoleReader.readLine();
    }
}
