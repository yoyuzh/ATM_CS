# ATM C/S

基于 Java Socket 的 ATM 客户端/服务端示例项目，同时包含命令行客户端和 Swing 图形界面客户端。

## 环境要求

- JDK 21 或更高版本
- macOS、Linux 或 Windows

## 文件说明

- `ATMServer.java`：ATM 服务端，默认监听 `2525` 端口
- `ATMClient.java`：命令行客户端
- `ATMGuiClient.java`：Swing 图形界面客户端
- `users.txt`：账户和密码数据
- `balances.txt`：账户和余额数据
- `start-atm-server.sh`：macOS/Linux 服务端启动脚本
- `start-atm-client.sh`：macOS/Linux 命令行客户端启动脚本
- `start-atm-gui.sh`：macOS/Linux 图形界面客户端启动脚本
- `start-atm-win.bat`：Windows 服务端/命令行客户端启动脚本
- `start-atm-gui-win.bat`：Windows 图形界面客户端启动脚本

## 账户数据

在 `users.txt` 中每行添加一个账户和密码：

```text
100001 1234
```

在 `balances.txt` 中添加对应账户的余额：

```text
100001 5000.00
```

修改账户文件后，需要重启 `ATMServer` 才会生效。

## macOS 或 Linux 运行方式

启动服务端：

```bash
./start-atm-server.sh 2525
```

启动命令行客户端：

```bash
./start-atm-client.sh 127.0.0.1 2525
```

启动 Swing 图形界面客户端：

```bash
./start-atm-gui.sh
```

## Windows 运行方式

启动服务端：

```bat
start-atm-win.bat server 2525
```

启动命令行客户端：

```bat
start-atm-win.bat client 127.0.0.1 2525
```

启动 Swing 图形界面客户端：

```bat
start-atm-gui-win.bat
```

## 局域网连接

在一台电脑上运行 `ATMServer` 后，另一台同一局域网内的电脑可以通过服务端所在机器的局域网 IP 连接：

```bash
./start-atm-client.sh 192.168.1.10 2525
```

图形界面客户端同样填写这个 IP 和端口即可连接。
