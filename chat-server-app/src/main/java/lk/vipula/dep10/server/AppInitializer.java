package lk.vipula.dep10.server;

import lk.vipula.dep10.server.model.Signing;
import lk.vipula.dep10.shared.enumaration.MessageHeader;
import lk.vipula.dep10.shared.model.Message;
import lk.vipula.dep10.server.model.User;

import java.io.*;
import java.lang.reflect.Type;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Instant;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AppInitializer {

    private static volatile AbstractList<User> userList = new ArrayList<>();
    private static volatile String chatHistory = "";
    private static volatile ArrayList<Signing> signings = new ArrayList<>();
    private static volatile ArrayList<String> loggedUsers = new ArrayList<>();
    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(5050);

        while (true){
            System.out.println("waiting for request...");

            Socket localSocket = serverSocket.accept();
            User user = new User(localSocket);
            System.out.println(user.getRemoteAddress());

            new Thread(() -> {
                String userLoggedIn = "";
                try {

                    ObjectInputStream objectInputStream = user.getObjectInputStream();

                    while (true){
                        
                        System.out.println("waiting for messages...");
                        Message message = (Message) objectInputStream.readObject();
                        
                        if(message.getMessageHeader() == MessageHeader.VALIDITY){
                            
                            if(!checkUserValidity(message)){

                                ObjectOutputStream objectOutputStream1 = user.getObjectOutputStream();
                                objectOutputStream1.writeObject(new Message(MessageHeader.INVALID,null));
                                objectOutputStream1.flush();
                                
                            }else {
                                
                                List<String> loggedUser = (List<String>) message.getMessageBody();
                                userLoggedIn = loggedUser.get(0);

                                ObjectOutputStream objectOutputStream = user.getObjectOutputStream();
                                objectOutputStream.writeObject(new Message(MessageHeader.VALID,null));
                                objectOutputStream.flush();

                                userList.add(user);
                                loggedUsers.add(userLoggedIn);
                                broadCastChatHistory();
                                Thread.sleep(100);
                                broadCastUsers();

                            }
                        } else if (message.getMessageHeader() == MessageHeader.SIGNUP) {

                            signUpUser(message);


                        } else if(message.getMessageHeader() == MessageHeader.MSG){
                            System.out.println("msg");
                            chatHistory += String.format("%s : %s \n",userLoggedIn, message.getMessageBody());
                            broadCastChatHistory();
                        } else if (message.getMessageHeader() == MessageHeader.EXIT) {
                            removeUser(user,userLoggedIn);
                            return;
                        }

                    }
                } catch (Exception e) {
                    removeUser(user,userLoggedIn);

                    /*if suddenly stream is come to an end or do nothing*/
                    if(e instanceof EOFException) return;

                    e.printStackTrace();
                }
            }).start();
        }

    }

    private static void signUpUser(Message message) throws IOException, ClassNotFoundException {
        File file = new File("/home/vipula/Desktop/userPw.chat");

        if(file.exists()){
            FileInputStream fileInputStream2 = new FileInputStream(file);
            ObjectInputStream objectInputStream2 = new ObjectInputStream(fileInputStream2);

            signings = (ArrayList<Signing>) objectInputStream2.readObject();
            for (Signing signing : signings) {
                System.out.println(signing.getName());
            }
            objectInputStream2.close();
        }

        FileOutputStream fileOutputStream2 = new FileOutputStream(file);
        ObjectOutputStream objectOutputStream2 = new ObjectOutputStream(fileOutputStream2);

        List<String> loggedUser = (List<String>) message.getMessageBody();
        String name = loggedUser.get(0);
        String password = loggedUser.get(1);

        signings.add(new Signing(name,password));
        objectOutputStream2.writeObject(signings);

        objectOutputStream2.close();
    }

    private static boolean checkUserValidity(Message message) {
        
        /*check the name pw list by file io*/
        List<String> loggedUser = (List<String>) message.getMessageBody();
        String name = loggedUser.get(0);
        String password = loggedUser.get(1);

        File file = new File("/home/vipula/Desktop/userPw.chat");
        if (!file.exists()) return false;
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            List<Signing> list = (List<Signing>) objectInputStream.readObject();

            for (Signing user : list) {
                if(!name.equals(user.getName())) continue;
                if(!password.equals(user.getPassword())) return false;
                return true;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    private static void removeUser(User user,String userLoggedIn) {
        userList.remove(user);
        loggedUsers.remove(userLoggedIn);
        broadCastUsers();
    }

    private static void broadCastChatHistory() {
        for (User user : userList) {
            new Thread(() -> {
                try {
                    ObjectOutputStream objectOutputStream = user.getObjectOutputStream();
                    Message message = new Message(MessageHeader.MSG, chatHistory);
                    objectOutputStream.writeObject(message);
                    objectOutputStream.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
    }

    private static void broadCastUsers() {
        ArrayList<String> loggedUserList = new ArrayList<>();
        for (String user : loggedUsers) {
            loggedUserList.add(user);
        }
        for (User user : userList) {
            new Thread(() -> {
                try {
                    ObjectOutputStream objectOutputStream = user.getObjectOutputStream();
                    Message message = new Message(MessageHeader.USERS, loggedUserList);
                    objectOutputStream.writeObject(message);
                    objectOutputStream.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
    }

}
