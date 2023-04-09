package lk.vipula.dep10.server;

import lk.vipula.dep10.server.enumaration.MessageHeader;
import lk.vipula.dep10.server.model.Message;
import lk.vipula.dep10.server.model.User;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.AbstractList;
import java.util.ArrayList;

public class AppInitializer {

    private static volatile AbstractList<User> userList = new ArrayList<>();
    private static volatile String chatHistory = "";
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(5050);

        while (true){
            Socket localSocket = serverSocket.accept();
            User user = new User(localSocket);
            System.out.println(user.getRemoteAddress());
            userList.add(user);

            new Thread(() -> {
                try {
                    restoreChatHistory(user);
                    broadCastUsers();

                    ObjectInputStream objectInputStream = user.getObjectInputStream();

                    while (true){
                        Message message = (Message) objectInputStream.readObject();
                        if(message.getMessageHeader() == MessageHeader.MSG){
                            chatHistory += String.format("%s , %s \n",user.getRemoteAddress(), message.getMessageBody());
                            broadCastChatHistory();
                        } else if (message.getMessageHeader() == MessageHeader.EXIT) {
                            removeUser(user);
                        }
                    }
                } catch (Exception e) {
                    removeUser(user);

                    /*if suddenly stream is come to an end or do nothing*/
                    if(e instanceof EOFException) return;

                    e.printStackTrace();
                }
            }).start();
        }
    }

    private static void removeUser(User user) {
        userList.remove(user);
        broadCastUsers();
    }

    private static void broadCastChatHistory() {
        for (User user : userList) {
            new Thread(() -> {
                try {
                    ObjectOutputStream objectOutputStream = user.getObjectOutputStream();
                    objectOutputStream.writeObject(new Message(MessageHeader.MSG,chatHistory));
                    objectOutputStream.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
    }

    private static void broadCastUsers() {
        ArrayList<String> ipAddressesList = new ArrayList<>();
        for (User user : userList) {
            ipAddressesList.add(user.getRemoteAddress());
        }
        for (User user : userList) {
            new Thread(() -> {
                try {
                    ObjectOutputStream objectOutputStream = user.getObjectOutputStream();
                    Message message = new Message(MessageHeader.USERS, ipAddressesList);
                    objectOutputStream.writeObject(message);
                    objectOutputStream.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }


    }

    private static void restoreChatHistory(User user){
        try {
            ObjectOutputStream objectOutputStream = user.getObjectOutputStream();
            objectOutputStream.writeObject(new Message(MessageHeader.MSG,chatHistory));
            objectOutputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
