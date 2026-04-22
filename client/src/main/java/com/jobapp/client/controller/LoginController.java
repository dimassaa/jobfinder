package com.jobapp.client.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.client.NetworkService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.application.Platform;

import java.util.HashMap;
import java.util.Map;

public class LoginController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(LoginController.class);

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private CheckBox rememberMeCheckBox;

    @FXML
    private Button loginButton;

    @FXML
    private Button forgotPasswordButton;

    @FXML
    private Button registerButton;

    @FXML
    private Label statusLabel;

    private NetworkService networkService;
    private ObjectMapper objectMapper;

    public LoginController() {
        this.objectMapper = new ObjectMapper();
    }

    public void setNetworkService(NetworkService networkService) {
        this.networkService = networkService;
    }

    @FXML
    private void initialize() {
        logger.info("LoginController initialized");
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Пожалуйста, заполните все поля");
            return;
        }

        if (!isValidEmail(email)) {
            showError("Неверный формат email");
            return;
        }

        loginButton.setDisable(true);
        statusLabel.setVisible(false);

        new Thread(() -> {
            try {
                if (networkService == null) {
                    logger.warn("NetworkService not set, using local stub");
                    handleLoginStub(email, password);
                    return;
                }

                Map<String, String> data = new HashMap<>();
                data.put("email", email);
                data.put("password", password);

                Message message = new Message("REQUEST", "LOGIN", data);

                Response response = networkService.sendAndReceive(message);

                Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    if ("OK".equals(response.getStatus())) {
                        logger.info("Login successful for: {}", email);
                        showSuccess("Вход выполнен: " + response.getMessage());
                    } else {
                        showError(response.getMessage());
                    }
                });
            } catch (Exception e) {
                logger.error("Login error: {}", e.getMessage());
                Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    showError("Ошибка соединения: " + e.getMessage());
                });
            }
        }).start();
    }

    private void handleLoginStub(String email, String password) {
        logger.info("Login stub called with email: {}", email);
        Platform.runLater(() -> {
            loginButton.setDisable(false);
            showSuccess("Login stub - OK");
        });
    }

    @FXML
    private void handleRegister() {
        logger.info("Register button clicked");
        showError("Регистрация временно недоступна");
    }

    @FXML
    private void handleForgotPassword() {
        logger.info("Forgot password button clicked");
        showError("Восстановление пароля временно недоступно");
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #D32F2F;");
        statusLabel.setVisible(true);
    }

    private void showSuccess(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #388E3C;");
        statusLabel.setVisible(true);
    }
}