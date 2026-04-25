package com.jobapp.client.controller;

import com.jobapp.client.AppContext;
import com.jobapp.client.NetworkService;
import com.jobapp.client.service.PasswordResetClientService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResetPasswordController {

    private static final Logger logger = LoggerFactory.getLogger(ResetPasswordController.class);

    @FXML
    private TextField tokenField;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label statusLabel;

    @FXML
    private Button resetButton;

    private PasswordResetClientService passwordResetService;

    public void setNetworkService(NetworkService networkService) {
        this.passwordResetService = new PasswordResetClientService(networkService);
    }

    @FXML
    private void initialize() {
        logger.info("ResetPasswordController initialized");
    }

    @FXML
    private void handleResetPassword() {
        String token = tokenField.getText().trim();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (token.isEmpty()) {
            showError("Пожалуйста, введите код из email");
            return;
        }

        if (newPassword.isEmpty()) {
            showError("Пожалуйста, введите новый пароль");
            return;
        }

        if (newPassword.length() < 6) {
            showError("Пароль должен содержать минимум 6 символов");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showError("Пароли не совпадают");
            return;
        }

        resetButton.setDisable(true);

        new Thread(() -> {
            try {
                passwordResetService.resetPassword(token, newPassword);
                Platform.runLater(() -> {
                    showSuccess("Пароль успешно сброшен!");
                    openLoginScene();
                });
            } catch (Exception e) {
                logger.error("Password reset failed: {}", e.getMessage());
                Platform.runLater(() -> {
                    showError(e.getMessage());
                    resetButton.setDisable(false);
                });
            }
        }).start();
    }

    @FXML
    private void handleBackToLogin() {
        openLoginScene();
    }

    private void openLoginScene() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/login.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = new javafx.scene.Scene(root, 400, 500);
            javafx.stage.Stage stage = (javafx.stage.Stage) tokenField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("JobMatch - Вход");
            stage.show();
        } catch (Exception e) {
            logger.error("Failed to load login scene: {}", e.getMessage());
            showError("Не удалось загрузить экран входа");
        }
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