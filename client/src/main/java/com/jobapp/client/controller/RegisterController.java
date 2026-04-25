package com.jobapp.client.controller;

import com.jobapp.client.NetworkService;
import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.common.dto.AuthResponse;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class RegisterController {

    private static final Logger logger = LoggerFactory.getLogger(RegisterController.class);

    @FXML
    private TextField nameField;

    @FXML
    private TextField surnameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private ComboBox<String> roleComboBox;

    @FXML
    private Label statusLabel;

    @FXML
    private Button registerButton;

    private NetworkService networkService;

    public void setNetworkService(NetworkService networkService) {
        this.networkService = networkService;
    }

    @FXML
    private void initialize() {
        logger.info("RegisterController initialized");
        roleComboBox.getItems().addAll("APPLICANT", "HR");
        roleComboBox.setValue("APPLICANT");
    }

    @FXML
    private void handleRegister() {
        String name = nameField.getText().trim();
        String surname = surnameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String role = roleComboBox.getValue();

        if (name.isEmpty() || surname.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("Пожалуйста, заполните все поля");
            return;
        }

        if (!isValidEmail(email)) {
            showError("Неверный формат email");
            return;
        }

        if (password.length() < 6) {
            showError("Пароль должен содержать минимум 6 символов");
            return;
        }

        if (!isValidName(name)) {
            showError("Имя должно содержать только буквы");
            return;
        }

        if (!isValidName(surname)) {
            showError("Фамилия должна содержать только буквы");
            return;
        }

        registerButton.setDisable(true);

        new Thread(() -> {
            try {
                if (networkService == null || !networkService.isConnected()) {
                    Platform.runLater(() -> {
                        showError("Не подключено к серверу");
                        registerButton.setDisable(false);
                    });
                    return;
                }

                Map<String, String> data = new HashMap<>();
                data.put("name", name);
                data.put("surname", surname);
                data.put("email", email);
                data.put("password", password);
                data.put("role", role);

                Message message = new Message("REQUEST", "REGISTER", data);
                Response response = networkService.sendAndReceive(message);

                Platform.runLater(() -> {
                    registerButton.setDisable(false);
                    if ("OK".equals(response.getStatus())) {
                        logger.info("Registration successful for: {}", email);
                        showSuccess("Регистрация успешна! Теперь вы можете войти.");
                        new Thread(() -> {
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            Platform.runLater(this::handleBackToLogin);
                        }).start();
                    } else {
                        showError(response.getMessage());
                    }
                });
            } catch (Exception e) {
                logger.error("Registration failed: {}", e.getMessage());
                Platform.runLater(() -> {
                    showError("Ошибка регистрации: " + e.getMessage());
                    registerButton.setDisable(false);
                });
            }
        }).start();
    }

    @FXML
    private void handleBackToLogin() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/login.fxml"));
            javafx.scene.Parent root = loader.load();
            LoginController controller = loader.getController();
            controller.setNetworkService(networkService);
            javafx.scene.Scene scene = new javafx.scene.Scene(root, 400, 550);
            javafx.stage.Stage stage = (javafx.stage.Stage) nameField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("JobMatch - Вход");
            stage.show();
        } catch (Exception e) {
            logger.error("Failed to load login scene: {}", e.getMessage());
            showError("Не удалось загрузить экран входа");
        }
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    private boolean isValidName(String name) {
        return name != null && name.matches("^[А-Яа-яA-Za-z]+$");
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