package lk.vipula.dep10.client.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lk.vipula.dep10.shared.enumaration.MessageHeader;
import lk.vipula.dep10.shared.model.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Arrays;

public class LoginViewController {

    public Label lblID;
    @FXML
    private Button btnLogin;
    @FXML
    private TextField txtName;
    @FXML
    private TextField txtPassword;
    private Socket socket;
    private ObjectOutputStream objectOutputStream;
    private ObjectInputStream objectInputStream;
    public void initialize(){

        connectServer();

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

    @FXML
    void btnLoginOnAction(ActionEvent event) throws IOException, ClassNotFoundException {
        if (!isValidEntry()) return;

        try {

            objectOutputStream.writeObject(new Message(MessageHeader.VALIDITY, Arrays.asList(txtName.getText().strip(),txtPassword.getText().strip())));
            objectOutputStream.flush();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (objectInputStream == null) objectInputStream = new ObjectInputStream(socket.getInputStream());
        Message message = (Message) objectInputStream.readObject();

        if(message.getMessageHeader() == MessageHeader.VALID){

            loadNewStage();

        }else {
            lblID.setText("wrong user name or password");
        }
    }

    private void loadNewStage(){
        try {
            Stage stage = (Stage) btnLogin.getScene().getWindow();
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/ClientView.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            ClientViewController controller = fxmlLoader.getController();
            controller.initData(socket,objectInputStream,objectOutputStream);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isValidEntry() {
        boolean isValid = true;
        txtName.getStyleClass().remove("invalid");
        txtPassword.getStyleClass().remove("invalid");

        String name = txtName.getText();
        String password = txtPassword.getText();

        if(!password.strip().matches("(.){3,}")){
            txtPassword.requestFocus();
            txtPassword.selectAll();
            txtPassword.getStyleClass().add("invalid");
            isValid = false;
        }
        if(!name.strip().matches("[a-zA-Z ]{3,}")){
            txtName.requestFocus();
            txtName.selectAll();
            txtName.getStyleClass().add("invalid");
            isValid = false;
        }
        return isValid;
    }

    public void btnSignInOnAction(ActionEvent actionEvent) {

        String name = txtName.getText();
        String password = txtPassword.getText();

        try {
            objectOutputStream.writeObject(new Message(MessageHeader.SIGNUP, Arrays.asList(name, password)));
            objectOutputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
