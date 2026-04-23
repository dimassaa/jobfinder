package com.jobapp.client.controller;

import com.jobapp.client.AppContext;
import com.jobapp.client.NetworkService;
import com.jobapp.client.model.UserProfile;
import com.jobapp.client.service.ProfileClientService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.Runnable;;

public class ProfileController {

    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);

    @FXML
    private TextField nameField;

    @FXML
    private TextField surnameField;

    @FXML
    private TextField cityField;

    @FXML
    private TextField phoneField;

    @FXML
    private TextArea additionalInfoArea;

    @FXML
    private VBox companySection;

    @FXML
    private TextField companyNameField;

    @FXML
    private TextField companyWebsiteField;

    @FXML
    private TextArea companyDescriptionArea;

    @FXML
    private Label statusLabel;

    @FXML
    private Button saveButton;

    @FXML
    private Button cancelButton;

    private ProfileClientService profileClientService;
    private Runnable onCancelCallback;
    private boolean isHr;

    public ProfileController() {
    }

    public void setNetworkService(NetworkService networkService) {
        this.profileClientService = new ProfileClientService(networkService);
    }

    public void setOnCancelCallback(Runnable callback) {
        this.onCancelCallback = callback;
    }

    @FXML
    private void initialize() {
        logger.info("ProfileController initialized");
        isHr = AppContext.isHr();
        
        if (isHr) {
            companySection.setVisible(true);
            companySection.setManaged(true);
        }
        
        loadProfile();
    }

    private void loadProfile() {
        saveButton.setDisable(true);
        statusLabel.setVisible(false);

        new Thread(() -> {
            try {
                UserProfile profile = profileClientService.getProfile();
                Platform.runLater(() -> populateFields(profile));
            } catch (Exception e) {
                logger.error("Failed to load profile: {}", e.getMessage());
                Platform.runLater(() -> showError("Не удалось загрузить профиль: " + e.getMessage()));
            }
        }).start();
    }

    private void populateFields(UserProfile profile) {
        nameField.setText(profile.getName() != null ? profile.getName() : "");
        surnameField.setText(profile.getSurname() != null ? profile.getSurname() : "");
        cityField.setText(profile.getCity() != null ? profile.getCity() : "");
        phoneField.setText(profile.getPhone() != null ? profile.getPhone() : "");
        additionalInfoArea.setText(profile.getAdditionalInfo() != null ? profile.getAdditionalInfo() : "");

        if (isHr) {
            companyNameField.setText(profile.getCompanyName() != null ? profile.getCompanyName() : "");
            companyWebsiteField.setText(profile.getCompanyWebsite() != null ? profile.getCompanyWebsite() : "");
            companyDescriptionArea.setText(profile.getCompanyDescription() != null ? profile.getCompanyDescription() : "");
        }

        saveButton.setDisable(false);
        logger.info("Profile fields populated");
    }

    @FXML
    private void handleSave() {
        String name = nameField.getText().trim();
        String surname = surnameField.getText().trim();
        String city = cityField.getText().trim();
        String phone = phoneField.getText().trim();
        String additionalInfo = additionalInfoArea.getText().trim();

        if (name.isEmpty() || surname.isEmpty()) {
            showError("Имя и фамилия обязательны");
            return;
        }

        saveButton.setDisable(true);
        statusLabel.setVisible(false);

        UserProfile profile = new UserProfile();
        profile.setName(name);
        profile.setSurname(surname);
        profile.setCity(city);
        profile.setPhone(phone);
        profile.setAdditionalInfo(additionalInfo);

        if (isHr) {
            profile.setCompanyName(companyNameField.getText().trim());
            profile.setCompanyWebsite(companyWebsiteField.getText().trim());
            profile.setCompanyDescription(companyDescriptionArea.getText().trim());
        }

        new Thread(() -> {
            try {
                profileClientService.updateProfile(profile);
                Platform.runLater(() -> showSuccess("Профиль сохранён"));
                logger.info("Profile updated successfully");
            } catch (Exception e) {
                logger.error("Failed to update profile: {}", e.getMessage());
                Platform.runLater(() -> {
                    saveButton.setDisable(false);
                    showError("Ошибка сохранения: " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void handleCancel() {
        logger.info("Cancel button clicked");
        if (onCancelCallback != null) {
            onCancelCallback.run();
        } else {
            goBack();
        }
    }

    private void goBack() {
        try {
            Stage stage = (Stage) cancelButton.getScene().getWindow();
            Scene scene = stage.getScene();
            
            if (isHr) {
                Parent hrRoot = javafx.fxml.FXMLLoader.load(getClass().getResource("/fxml/hr_main.fxml"));
                scene.setRoot(hrRoot);
            } else {
                Parent mainRoot = javafx.fxml.FXMLLoader.load(getClass().getResource("/fxml/main.fxml"));
                scene.setRoot(mainRoot);
            }
        } catch (Exception e) {
            logger.error("Failed to go back: {}", e.getMessage());
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