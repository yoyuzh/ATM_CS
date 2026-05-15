# ATM C/S

Java socket-based ATM client/server demo with both command-line and Swing GUI clients.

## Requirements

- JDK 21 or newer
- macOS, Linux, or Windows

## Files

- `ATMServer.java`: ATM server, listens on TCP port `2525` by default.
- `ATMClient.java`: command-line ATM client.
- `ATMGuiClient.java`: Java Swing ATM client.
- `users.txt`: card number and password data.
- `balances.txt`: card number and balance data.
- `start-atm-server.sh`: macOS/Linux server launcher.
- `start-atm-client.sh`: macOS/Linux command-line client launcher.
- `start-atm-gui.sh`: macOS/Linux Swing client launcher.
- `start-atm-win.bat`: Windows server/client launcher.
- `start-atm-gui-win.bat`: Windows Swing client launcher.

## Account Data

Add one account per line in `users.txt`:

```text
100001 1234
```

Add the matching balance in `balances.txt`:

```text
100001 5000.00
```

Restart `ATMServer` after changing account files.

## Run On macOS or Linux

Start the server:

```bash
./start-atm-server.sh 2525
```

Start the command-line client:

```bash
./start-atm-client.sh 127.0.0.1 2525
```

Start the Swing GUI client:

```bash
./start-atm-gui.sh
```

## Run On Windows

Start the server:

```bat
start-atm-win.bat server 2525
```

Start the command-line client:

```bat
start-atm-win.bat client 127.0.0.1 2525
```

Start the Swing GUI client:

```bat
start-atm-gui-win.bat
```

## LAN Connection

Run `ATMServer` on one computer, then connect from another computer in the same LAN by using the server computer's LAN IP:

```bash
./start-atm-client.sh 192.168.1.10 2525
```

or use the same host and port in the Swing GUI client.
