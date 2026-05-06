package com.jobapp.client.controller;

import com.jobapp.client.AppContext;
import com.jobapp.client.NetworkService;
import com.jobapp.client.model.UserProfile;
import com.jobapp.client.service.ProfileClientService;
import com.jobapp.client.service.RecommendationClientService;
import com.jobapp.common.dto.RecommendedVacancyDto;
import com.jobapp.common.dto.TagDto;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.Runnable;
import java.util.Arrays;
import java.util.List;

public class ProfileController {

    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);

    private static final List<String> CITIES = Arrays.asList(
            "Минск", "Гродно", "Гомель", "Брест", "Витебск", "Могилёв"
    );

    @FXML
    private TextField nameField;

    @FXML
    private TextField surnameField;

    @FXML
    private ComboBox<String> cityComboBox;

    @FXML
    private TextField phoneField;

    @FXML
    private TextArea additionalInfoArea;

    @FXML
    private Label statusLabel;

    @FXML
    private Button saveButton;

    @FXML
    private Button selectTagsButton;

    @FXML
    private Label tagsCountLabel;

    private ProfileClientService profileClientService;
    private RecommendationClientService recommendationClientService;
    private Runnable onCloseCallback;
    private int userTagsCount = 0;

    public ProfileController() {
    }

    public void setNetworkService(NetworkService networkService) {
        this.profileClientService = new ProfileClientService(networkService);
    }

    public void setOnCloseCallback(Runnable callback) {
        this.onCloseCallback = callback;
    }

    @FXML
    private void initialize() {
        logger.info("ProfileController initialized");

        NetworkService networkService = AppContext.getNetworkService();
        if (networkService == null || !networkService.isConnected()) {
            logger.error("NetworkService not available or disconnected");
            return;
        }
        this.profileClientService = new ProfileClientService(networkService);

        ObservableList<String> cities = FXCollections.observableArrayList(CITIES);
        cityComboBox.setItems(cities);

        loadProfile();
        loadUserTagsCount();
    }

    private void loadUserTagsCount() {
        if (profileClientService == null) {
            logger.warn("profileClientService not initialized yet");
            return;
        }

        new Thread(() -> {
            try {
                List<TagDto> userTags = profileClientService.getUserTags();
                Platform.runLater(() -> {
                    userTagsCount = userTags.size();
                    if (userTagsCount > 0) {
                        tagsCountLabel.setText("Выбрано: " + userTagsCount + " навыков");
                    } else {
                        tagsCountLabel.setText("Навыки не выбраны");
                    }
                });
            } catch (Exception e) {
                logger.error("Failed to load user tags: {}", e.getMessage());
            }
        }).start();
    }

    private void refreshRecommendations() {
        NetworkService networkService = AppContext.getNetworkService();
        if (networkService == null) {
            logger.warn("NetworkService not available");
            return;
        }
        recommendationClientService = new RecommendationClientService(networkService);

        new Thread(() -> {
            try {
                List<RecommendedVacancyDto> recommendations = recommendationClientService.getRecommendations();
                logger.info("Recommendations refreshed after tags update: {} recommendations", recommendations.size());
            } catch (Exception e) {
                logger.error("Failed to refresh recommendations: {}", e.getMessage());
            }
        }).start();
    }

    private void loadProfile() {
        if (profileClientService == null) {
            logger.warn("profileClientService not initialized yet");
            return;
        }
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

        String city = profile.getCity();
        if (city != null && !city.isEmpty()) {
            cityComboBox.setValue(city);
        }

        phoneField.setText(profile.getPhone() != null ? profile.getPhone() : "");
        additionalInfoArea.setText(profile.getAdditionalInfo() != null ? profile.getAdditionalInfo() : "");

        saveButton.setDisable(false);
        logger.info("Profile fields populated");
    }

    @FXML
    private void handleSave() {
        String name = nameField.getText().trim();
        String surname = surnameField.getText().trim();
        String city = cityComboBox.getValue();
        String phone = phoneField.getText().trim();
        String additionalInfo = additionalInfoArea.getText().trim();

        if (name.isEmpty() || surname.isEmpty()) {
            showError("Имя и фамилия обязательны");
            return;
        }

        if (city == null || city.isEmpty()) {
            showError("Пожалуйста, выберите город");
            return;
        }

        saveButton.setDisable(true);
        statusLabel.setVisible(false);

        UserProfile profile = new UserProfile();
        profile.setName(name);
        profile.setSurname(surname);
        profile.setCity(city != null ? city : "");
        profile.setPhone(phone);
        profile.setAdditionalInfo(additionalInfo);

        new Thread(() -> {
            try {
                if (profileClientService == null) {
                    logger.error("profileClientService is null during save");
                    Platform.runLater(() -> {
                        saveButton.setDisable(false);
                        showError("Ошибка: сервис не инициализирован");
                    });
                    return;
                }
                profileClientService.updateProfile(profile);
                Platform.runLater(() -> {
                    showSuccess("Профиль сохранён");
                    logger.info("Profile updated successfully");
                });
            } catch (Exception e) {
                logger.error("Failed to update profile: {}", e.getMessage(), e);
                Platform.runLater(() -> {
                    saveButton.setDisable(false);
                    showError("Ошибка сохранения: " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void handleSelectTags() {
        logger.info("Opening tags selector");

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/tags_selector.fxml"));
            Parent root = loader.load();

            TagsSelectorController controller = loader.getController();
            NetworkService networkService = AppContext.getNetworkService();
            if (networkService != null) {
                controller.setNetworkService(networkService);
            }

            controller.setOnCloseCallback(() -> {
                loadUserTagsCount();
                refreshRecommendations();
            });

            Stage stage = new Stage();
            stage.setTitle("Выбор навыков");
            stage.setScene(new Scene(root, 450, 450));
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(selectTagsButton.getScene().getWindow());
            stage.showAndWait();

        } catch (Exception e) {
            logger.error("Failed to open tags selector: {}", e.getMessage());
            showError("Не удалось открыть выбор навыков: " + e.getMessage());
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