package client;

import com.google.gson.*;
import server.AuthMessage;
import server.Message;
import server.MyServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class SocketServerService implements ServerService {

    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private boolean isConnected = false;

    public boolean isConnected() {
        return isConnected;
    }

    @Override
    public void openConnection() {
        try {
            socket = new Socket("localhost", MyServer.PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String authorization(String login, String password) throws IOException {
        AuthMessage authMessage = new AuthMessage();
        authMessage.setLogin(login);
        authMessage.setPassword(password);
        out.writeUTF(new Gson().toJson(authMessage));

        authMessage = new Gson().fromJson(in.readUTF(), AuthMessage.class);
        if (authMessage.isAuthenticated()) {
            isConnected = true;
        }
        return authMessage.getNick();
    }

    @Override
    public void closeConnection() {
        try {
            out.close();
            in.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendMessage(String message) {
        Message msg = new Message();
        msg.setMessage(message);

        try {
            out.writeUTF(new Gson().toJson(msg));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Message readMessages() {
        try {
            return new Gson().fromJson(in.readUTF(), Message.class);
        } catch (IOException e) {
            e.printStackTrace();
            return new Message();
        }
    }
}
