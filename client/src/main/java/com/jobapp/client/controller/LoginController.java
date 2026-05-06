package com.jobapp.client.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public void setNetworkService(NetworkService networkService) {
        this.networkService = networkService;
    }

    @FXML
    private void initialize() {
        logger.info("LoginController initialized");
        updateConnectionStatus();
    }

    public void updateConnectionStatus() {
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

        final boolean[] successHolder = {false};

        new Thread(() -> {
            try {
                if (networkService != null) {
                    successHolder[0] = networkService.reconnect();
                }
            } finally {
                boolean finalSuccess = successHolder[0];
                Platform.runLater(() -> {
                    if (finalSuccess) {
                        connectionStatusLabel.setText("Подключено");
                        connectionStatusLabel.setStyle("-fx-text-fill: #388E3C;");
                        reconnectButton.setVisible(false);
                    } else {
                        connectionStatusLabel.setText("Не подключено");
                        connectionStatusLabel.setStyle("-fx-text-fill: #D32F2F;");
                        reconnectButton.setVisible(true);
                    }
                    reconnectButton.setDisable(false);
                });
            }
        }).start();
    }

    @FXML
    private void handleLogin() {
        String emailInput = emailField.getText();
        String password = passwordField.getText();

        if (emailInput == null || emailInput.isBlank() || password == null || password.isEmpty()) {
            showError("Пожалуйста, заполните все поля");
            return;
        }

        String email = emailInput.trim();

        if (!isValidEmail(email)) {
            showError("Неверный формат email");
            return;
        }

        loginButton.setDisable(true);
        final String emailForSuccess = email;

        new Thread(() -> {
            try {
                if (networkService == null) {
                    logger.warn("NetworkService not set");
                    Platform.runLater(() -> {
                        loginButton.setDisable(false);
                        showError("Ошибка инициализации. Перезапустите приложение.");
                        updateConnectionStatus();
                    });
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

    private void handleLoginSuccess(Response response) {
        AuthResponse authResponse = null;
        
        if (response.getData() instanceof AuthResponse) {
            authResponse = (AuthResponse) response.getData();
        } else if (response.getData() instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> dataMap = (java.util.Map<String, Object>) response.getData();
            authResponse = objectMapper.convertValue(dataMap, AuthResponse.class);
        }
        
        if (authResponse != null && authResponse.isSuccess()) {
            AppContext.setCurrentUserId(authResponse.getUserId());
            AppContext.setCurrentRole(authResponse.getRole());
            AppContext.setCurrentEmail(emailField.getText().trim());
            AppContext.setNetworkService(networkService);

            logger.info("Login successful. User: {}, Role: {}", authResponse.getUserId(), authResponse.getRole());

            if ("HR".equals(authResponse.getRole())) {
                showSuccess("Вход выполнен (HR). Открытие панели работодателя...");
                switchToHrScene();
            } else if ("ADMIN".equals(authResponse.getRole())) {
                showSuccess("Вход выполнен (ADMIN). Открытие панели администратора...");
                switchToAdminScene();
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
            AppContext.setNetworkService(networkService);
            java.net.URL fxmlUrl = getClass().getResource("/fxml/main.fxml");
            logger.info("FXML URL: {}", fxmlUrl);
            if (fxmlUrl == null) {
                showError("Файл main.fxml не найден в classpath");
                return;
            }
            Parent mainRoot = javafx.fxml.FXMLLoader.load(fxmlUrl);
            javafx.scene.Scene scene = new javafx.scene.Scene(mainRoot, 800, 600);
            javafx.stage.Stage stage = (javafx.stage.Stage) emailField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("JobMatch - Главное окно");
            stage.setResizable(true);
            stage.show();
        } catch (Exception e) {
            logger.error("Failed to switch to main scene", e);
            showError("Не удалось загрузить главное окно: " + e.getMessage());
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
            logger.error("Failed to switch to HR scene: " + e.getMessage());
            showError("Не удалось загрузить панель HR");
        }
    }

    private void switchToAdminScene() {
        try {
            logger.info("Switching to admin scene");
            Parent adminRoot = javafx.fxml.FXMLLoader.load(getClass().getResource("/fxml/admin_panel.fxml"));
            javafx.scene.Scene scene = new javafx.scene.Scene(adminRoot, 900, 650);
            javafx.stage.Stage stage = (javafx.stage.Stage) emailField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("JobMatch - Панель администратора");
            stage.setResizable(true);
            stage.show();
        } catch (Exception e) {
            logger.error("Failed to switch to admin scene: " + e.getMessage());
            showError("Не удалось загрузить панель администратора");
        }
    }

    @FXML
    private void handleRegister() {
        logger.info("Register button clicked");
        openRegisterScene();
    }

    private void openRegisterScene() {
        try {
            logger.info("Loading register.fxml from classpath");
            java.net.URL fxmlUrl = getClass().getResource("/fxml/register.fxml");
            if (fxmlUrl == null) {
                logger.error("register.fxml not found in classpath");
                showError("Файл регистрации не найден");
                return;
            }
            logger.info("Found register.fxml: {}", fxmlUrl);

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(fxmlUrl);
            javafx.scene.Parent root = loader.load();
            RegisterController controller = loader.getController();
            controller.setNetworkService(networkService);
            javafx.scene.Scene scene = new javafx.scene.Scene(root, 450, 550);
            javafx.stage.Stage stage = (javafx.stage.Stage) emailField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("JobMatch - Регистрация");
            stage.show();
            logger.info("Register scene loaded successfully");
        } catch (Exception e) {
            logger.error("Failed to load register scene", e);
            showError("Не удалось загрузить экран регистрации: " + e.getMessage());
        }
    }

    @FXML
    private void handleForgotPassword() {
        logger.info("Forgot password button clicked");
        openForgotPasswordScene();
    }

    private void openForgotPasswordScene() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/forgot_password.fxml"));
            javafx.scene.Parent root = loader.load();
            ForgotPasswordController controller = loader.getController();
            controller.setNetworkService(networkService);
            javafx.scene.Scene scene = new javafx.scene.Scene(root, 400, 350);
            javafx.stage.Stage stage = (javafx.stage.Stage) emailField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("JobMatch - Восстановление пароля");
            stage.show();
        } catch (Exception e) {
            logger.error("Failed to load forgot password scene: {}", e.getMessage());
            showError("Не удалось загрузить экран восстановления пароля");
        }
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
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