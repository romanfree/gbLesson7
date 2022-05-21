package server;

import com.google.gson.Gson;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private final long TIMEOUT = 2;

    private Socket socket;
    private MyServer myServer;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private String nick;
    private long birthTime;
    private Thread thread;

    public ClientHandler(MyServer myServer, Socket socket) {
        birthTime -= System.currentTimeMillis();
        try {
            this.myServer = myServer;
            this.socket = socket;
            this.dataInputStream = new DataInputStream(socket.getInputStream());
            this.dataOutputStream = new DataOutputStream(socket.getOutputStream());
            new Thread(() -> {
                try {
                    authentication();
                    readMessages();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    closeConnection();
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeConnection() {
        myServer.unsubscribe(this);
        Message message = new Message();
        message.setMessage(nick + " вышел из чата");
        myServer.broadcastMessage(message);
        try {
            dataOutputStream.close();
            dataInputStream.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void authentication() {
        while (true) {
            try {
                if(System.currentTimeMillis() - birthTime > 1000 * TIMEOUT){
                    thread.interrupt();
                    socket.close();
                    return;
                }
                AuthMessage message = new Gson().fromJson(dataInputStream.readUTF(), AuthMessage.class);
                String nick = myServer.getAuthService().getNickByLoginAndPass(message.getLogin(), message.getPassword());
                if (nick != null && !myServer.isNickBusy(nick)) {
                    message.setAuthenticated(true);
                    message.setNick(nick);
                    this.nick = nick;
                    dataOutputStream.writeUTF(new Gson().toJson(message));
                    Message broadcastMsg = new Message();
                    broadcastMsg.setMessage(nick + " вошел в чат");
                    myServer.broadcastMessage(broadcastMsg);
                    myServer.subscribe(this);
                    this.nick = nick;
                    return;
                }
            } catch (IOException ignored) {
            }
        }
    }

    private void readMessages() throws IOException {
        while (true) {
            Message message = new Gson().fromJson(dataInputStream.readUTF(), Message.class);
            message.setNick(nick);
            System.out.println(message);
            if (!message.getMessage().startsWith("/")) {
                myServer.broadcastMessage(message);
                continue;
            }
            // /<command> <message>
            String[] tokens = message.getMessage().split("\\s");
            switch (tokens[0]) {
                case "/end": {
                    return;
                }
                case "/w": {// /w <nick> <message>
                    if (tokens.length < 3) {
                        Message msg = new Message();
                        msg.setMessage("Не хватает параметров, необходимо отправить команду следующего вида: /w <ник> <сообщение>");
                        this.sendMessage(msg);
                    }
                    String nick = tokens[1];
                    StringBuilder msg = new StringBuilder();
                    for (int i = 2; i < tokens.length; i++) {
                        msg.append(" ");
                        msg.append(tokens[i]);
                    }
                    myServer.sendMsgToClient(this, nick, msg.toString());
                    break;
                }
            }
        }
    }

    public void sendMessage(Message message) {
        try {
            dataOutputStream.writeUTF(new Gson().toJson(message));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNick() {
        return nick;
    }
}
