package com.jobapp.client.controller;

import com.jobapp.client.AppContext;
import com.jobapp.client.NetworkService;
import com.jobapp.client.service.PasswordResetClientService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ForgotPasswordController {

    private static final Logger logger = LoggerFactory.getLogger(ForgotPasswordController.class);

    @FXML
    private TextField emailField;

    @FXML
    private Label statusLabel;

    @FXML
    private Button sendButton;

    private PasswordResetClientService passwordResetService;

    public void setNetworkService(NetworkService networkService) {
        this.passwordResetService = new PasswordResetClientService(networkService);
    }

    @FXML
    private void initialize() {
        logger.info("ForgotPasswordController initialized");
    }

    @FXML
    private void handleSendCode() {
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            showError("Пожалуйста, введите email");
            return;
        }

        if (!isValidEmail(email)) {
            showError("Неверный формат email");
            return;
        }

        sendButton.setDisable(true);

        new Thread(() -> {
            try {
                passwordResetService.requestPasswordReset(email);
                Platform.runLater(() -> {
                    showSuccess("Код отправлен на email. Проверьте почту.");
                    openResetPasswordScene();
                });
            } catch (Exception e) {
                logger.error("Password reset request failed: {}", e.getMessage());
                Platform.runLater(() -> {
                    showError(e.getMessage());
                    sendButton.setDisable(false);
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
            javafx.scene.Scene scene = new javafx.scene.Scene(root, 400, 500);
            javafx.stage.Stage stage = (javafx.stage.Stage) emailField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("JobMatch - Вход");
            stage.show();
        } catch (Exception e) {
            logger.error("Failed to load login scene: {}", e.getMessage());
            showError("Не удалось загрузить экран входа");
        }
    }

    private void openResetPasswordScene() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/reset_password.fxml"));
            javafx.scene.Parent root = loader.load();
            ResetPasswordController controller = loader.getController();
            controller.setNetworkService(AppContext.getNetworkService());
            javafx.scene.Scene scene = new javafx.scene.Scene(root, 400, 450);
            javafx.stage.Stage stage = (javafx.stage.Stage) emailField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("JobMatch - Сброс пароля");
            stage.show();
        } catch (Exception e) {
            logger.error("Failed to load reset password scene: {}", e.getMessage());
            showError("Не удалось загрузить экран сброса пароля");
        }
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