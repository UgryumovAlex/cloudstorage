package com.geekbrains.cloudstorage.client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class RegisterController {

    private Controller controller;

    @FXML
    private TextField loginField;
    @FXML
    private PasswordField passwordField;

    @FXML
    private Button registerButton;

    public void tryToRegister(ActionEvent actionEvent) {

        String login = loginField.getText().trim();
        String password = passwordField.getText().trim();

        controller.registration(login, password);

        Stage stage = (Stage) registerButton.getScene().getWindow();
        stage.close();
    }

    public void setController(Controller controller) {
        this.controller = controller;
    }
}
