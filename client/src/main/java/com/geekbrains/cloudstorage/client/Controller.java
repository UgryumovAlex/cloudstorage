package com.geekbrains.cloudstorage.client;


import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    @FXML
    VBox clientPanel;
    @FXML
    Label labelInfo;

    private Stage stage;

    private Stage loginStage;
    private LoginController loginController;

    private Stage registerStage;
    private RegisterController registerController;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private final String IP_ADDRESS = "localhost";
    private final int PORT = 5000;

    private boolean authenticated;
    private String login;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        PanelController clientController = (PanelController)clientPanel.getProperties().get("ctrl");
        clientController.setPanelLabel("CLIENT SIDE");

        Platform.runLater(() -> {
            stage = (Stage) clientPanel.getScene().getWindow();
            stage.setOnCloseRequest(event -> {
                if (socket != null && !socket.isClosed()) {
                    sendCommand("disconnect");
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        });
        setAuthenticated(false);
    }

    private void connect() {
        try {
            socket = new Socket(IP_ADDRESS, PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void tryToLogin(ActionEvent actionEvent) {
        if (loginStage == null) {
            createLoginWindow();
        }
        Platform.runLater(() -> loginStage.show());
    }

    private void createLoginWindow() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/login.fxml"));
            Parent root = fxmlLoader.load();
            loginStage = new Stage();
            loginStage.setTitle("Authentication");
            loginStage.setScene(new Scene(root, 225, 100));

            loginStage.initModality(Modality.APPLICATION_MODAL);
            loginStage.initStyle(StageStyle.UTILITY);

            loginController = fxmlLoader.getController();
            loginController.setController(this);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void tryToRegister(ActionEvent actionEvent) {
        if (registerStage == null) {
            createRegisterWindow();
        }
        Platform.runLater(() -> registerStage.show());
    }

    private void createRegisterWindow() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/register.fxml"));
            Parent root = fxmlLoader.load();
            registerStage = new Stage();
            registerStage.setTitle("Registration");
            registerStage.setScene(new Scene(root, 225, 125));

            registerStage.initModality(Modality.APPLICATION_MODAL);
            registerStage.initStyle(StageStyle.UTILITY);

            registerController = fxmlLoader.getController();
            registerController.setController(this);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void authentication(String login, String password) {
        /*
        sendCommand(String.format("auth %s %s", login, password));
        String response = getResponse();
        */

        if (socket == null || socket.isClosed()) {
            connect();
        }

        try {
            out.write(String.format("auth %s %s", login, password).getBytes(StandardCharsets.UTF_8));
            out.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }

        String response = null;

        try {
            int size = in.available();
            StringBuffer buffer = new StringBuffer();
            for(int i = 0; i< size; i++) {
                buffer.append((char)in.read());
            }
            response = buffer.toString();

        } catch (IOException e) {
            e.printStackTrace();
        }

        if ("USER_AUTHENTICATED".equals(response)) {
            this.login = login;
            setAuthenticated(true);
        } else if ("INCORRECT_LOGIN_OR_PASSWORD".equals(response)) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Incorrect login or password", ButtonType.OK);
            alert.showAndWait();

            this.login = null;
            setAuthenticated(false);
        }
    }

    public void registration(String login, String password) {
        sendCommand(String.format("reg %s %s", login, password));
        String response = getResponse();

        System.out.println(response);
        if ("USER_REGISTERED".equals(response)) {
            this.login = login;
            setAuthenticated(true);

        } else if ("REGISTRATION_ERROR".equals(response)) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Registration error", ButtonType.OK);
            alert.showAndWait();
            this.login = null;
            setAuthenticated(false);
        }
    }

    private void sendCommand(String command) {
        System.out.println("command : " + command);

        if (socket == null || socket.isClosed()) {
            connect();
        }

        try {
            out.write(command.getBytes(StandardCharsets.UTF_8));
            out.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private String getResponse() {
        System.out.println("getResponse start");
        try {
            int size = in.available();
            System.out.println("bytes to read available : " + size);
            StringBuffer buffer = new StringBuffer();
            for(int i = 0; i< size; i++) {
                buffer.append((char)in.read());
            }
            String response = buffer.toString();
            System.out.println("response : " + response);

            return response;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "getResponseERROR";
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;

        if (!authenticated) {
            login = "";
        }
        setUserStatus();
    }

    public void tryToLogout(ActionEvent actionEvent) {
        sendCommand("disconnect");
        String response = getResponse();
        if ("USER_DISCONNECTED".equals(response)) {
            setAuthenticated(false);
        }
    }

    private void setUserStatus() {
        if (!authenticated) {
            labelInfo.setText("SERVER SIDE, user not connected");
            labelInfo.setStyle("-fx-font-weight: normal;");
        } else {
            labelInfo.setText("SERVER SIDE, user connected : "+ login);
            labelInfo.setStyle("-fx-font-weight: bold;");
        }
    }
}
