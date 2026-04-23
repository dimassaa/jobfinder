package com.jobapp.client.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobapp.client.AppContext;
import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.AuthResponse;
import com.jobapp.client.NetworkService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.application.Platform;
import javafx.scene.Parent;

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

    @FXML
    private Label connectionStatusLabel;

    @FXML
    private Button reconnectButton;

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
        updateConnectionStatus();
    }

    private void updateConnectionStatus() {
        if (networkService != null && networkService.isConnected()) {
            connectionStatusLabel.setText("Подключено");
            connectionStatusLabel.setStyle("-fx-text-fill: #388E3C;");
            reconnectButton.setVisible(false);
        } else {
            connectionStatusLabel.setText("Не подключено");
            connectionStatusLabel.setStyle("-fx-text-fill: #D32F2F;");
            reconnectButton.setVisible(true);
        }
    }

    @FXML
    private void handleReconnect() {
        logger.info("Reconnect button clicked");
        reconnectButton.setDisable(true);
        connectionStatusLabel.setText("Подключение...");
        connectionStatusLabel.setStyle("-fx-text-fill: #FFA000;");

        new Thread(() -> {
            try {
                if (networkService != null) {
                    networkService.reconnect();
                }
            } finally {
                Platform.runLater(this::updateConnectionStatus);
                Platform.runLater(() -> reconnectButton.setDisable(false));
            }
        }).start();
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
        final String emailForSuccess = email;

        new Thread(() -> {
            try {
                if (networkService == null) {
                    logger.warn("NetworkService not set, using local stub");
                    handleLoginStub(email, password);
                    return;
                }

                if (!networkService.isConnected()) {
                    logger.warn("Not connected to server");
                    Platform.runLater(() -> {
                        loginButton.setDisable(false);
                        showError("Не подключено к серверу. Нажмите 'Переподключиться'.");
                        updateConnectionStatus();
                    });
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
                        logger.info("Login successful for: {}", emailForSuccess);
                        handleLoginSuccess(response);
                    } else {
                        showError(response.getMessage());
                    }
                    updateConnectionStatus();
                });
            } catch (Exception e) {
                logger.error("Login error: {}", e.getMessage());
                Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    showError("Ошибка соединения: " + e.getMessage());
                    updateConnectionStatus();
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

    private void handleLoginSuccess(Response response) {
        if (response.getData() instanceof AuthResponse) {
            AuthResponse authResponse = (AuthResponse) response.getData();
            AppContext.setCurrentUserId(authResponse.getUserId());
            AppContext.setCurrentRole(authResponse.getRole());
            AppContext.setCurrentEmail(emailField.getText().trim());

            logger.info("Login successful. User: {}, Role: {}", authResponse.getUserId(), authResponse.getRole());

            if ("HR".equals(authResponse.getRole())) {
                showSuccess("Вход выполнен (HR). Открытие панели работодателя...");
                switchToHrScene();
            } else {
                showSuccess("Вход выполнен (Соискатель). Открытие главного окна...");
                switchToMainScene();
            }
        } else {
            showSuccess("Вход выполнен: " + response.getMessage());
        }
    }

    private void switchToMainScene() {
        try {
            logger.info("Switching to main scene for USER");
            Parent mainRoot = javafx.fxml.FXMLLoader.load(getClass().getResource("/fxml/main.fxml"));
            javafx.scene.Scene scene = new javafx.scene.Scene(mainRoot, 800, 600);
            javafx.stage.Stage stage = (javafx.stage.Stage) emailField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("JobMatch - Главное окно");
            stage.setResizable(true);
            stage.show();
        } catch (Exception e) {
            logger.error("Failed to switch to main scene: {}", e.getMessage());
            showError("Не удалось загрузить главное окно");
        }
    }

    private void switchToHrScene() {
        try {
            logger.info("Switching to HR scene");
            Parent hrRoot = javafx.fxml.FXMLLoader.load(getClass().getResource("/fxml/hr_main.fxml"));
            javafx.scene.Scene scene = new javafx.scene.Scene(hrRoot, 900, 650);
            javafx.stage.Stage stage = (javafx.stage.Stage) emailField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("JobMatch - Панель работодателя");
            stage.setResizable(true);
            stage.show();
        } catch (Exception e) {
            logger.error("Failed to switch to HR scene: {}", e.getMessage());
            showError("Не удалось загрузить панель HR");
        }
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