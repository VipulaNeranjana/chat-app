package lk.vipula.dep10.client.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import lk.vipula.dep10.shared.enumaration.MessageHeader;
import lk.vipula.dep10.shared.model.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class ClientViewController {

    public ListView<String> txtUsers;
    public AnchorPane root;
    @FXML
    private Button btnSend;

    @FXML
    private TextArea txtShowChat;

    @FXML
    private TextField txtType;

    private Socket socket;
    private ObjectOutputStream objectOutputStream;
    private ObjectInputStream objectInputStream;
    public void initialize(){
        connectServer();
        readServerResponse();

        Platform.runLater(() -> closeSocketOnStageCloseRequest());
    }

    private void connectServer(){
        try {
            socket = new Socket("127.0.0.1", 5050);

            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectOutputStream.flush();

        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR,"failed to connect to the server").showAndWait();
            Platform.exit();
        }
    }
    private void readServerResponse(){
        new Thread(() -> {
            try {
                objectInputStream = new ObjectInputStream(socket.getInputStream());

                while (true){
                    Message message = (Message) objectInputStream.readObject();

                    if(message.getMessageHeader() == MessageHeader.USERS){
                        ArrayList<String> userList = (ArrayList<String>) message.getMessageBody();
                        Platform.runLater(() -> {
                            txtUsers.getItems().clear();
                            txtUsers.getItems().addAll(userList);
                        });
                    } else if (message.getMessageHeader() == MessageHeader.MSG) {
                        Platform.runLater(() -> {
                            txtShowChat.setText(message.getMessageBody().toString());
                            txtShowChat.setScrollTop(Double.MAX_VALUE);
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                new Alert(Alert.AlertType.ERROR,"failed to get response from server!").showAndWait();
            }
        }).start();

    }
    private void closeSocketOnStageCloseRequest(){
        btnSend.getScene().getWindow().setOnCloseRequest(event -> {
            try {
                objectOutputStream.writeObject(new Message(MessageHeader.EXIT,null));
                objectOutputStream.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }


    @FXML
    void btnSendOnAction(ActionEvent event) {
        try {
            Message message = new Message(MessageHeader.MSG, txtType.getText());
            objectOutputStream.writeObject(message);
            objectOutputStream.flush();
            txtType.clear();
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR,"failed to send message, try again!").show();
            e.printStackTrace();
        }

    }


    public void rootOnKeyPressed(KeyEvent keyEvent) {
        if(keyEvent.getCode() == KeyCode.ENTER) btnSend.fire();
    }
}
