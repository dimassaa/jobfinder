package com.jobapp.client.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jobapp.client.AppContext;
import com.jobapp.client.NetworkService;
import com.jobapp.common.Message;
import com.jobapp.common.Response;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class HrCompanyController {

    private static final Logger logger = LoggerFactory.getLogger(HrCompanyController.class);

    @FXML
    private TextField companyNameField;

    @FXML
    private TextField websiteField;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private Label statusLabel;

    @FXML
    private Button saveButton;

    private NetworkService networkService;
    private ObjectMapper objectMapper;
    private boolean dataLoaded = false;

    @FXML
    private void initialize() {
        logger.info("HrCompanyController initializing");

        networkService = AppContext.getNetworkService();
        if (networkService == null) {
            logger.error("NetworkService is null in HrCompanyController");
            showStatus("Ошибка подключения к серверу", true);
            return;
        }

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        setupStatusLabel();

        loadCompanyData();
    }

    private void setupStatusLabel() {
        statusLabel.setWrapText(true);
    }

    @FXML
    private void handleSave() {
        String companyName = companyNameField.getText();
        if (companyName == null || companyName.isBlank()) {
            showStatus("Название компании обязательно", true);
            return;
        }

        saveButton.setDisable(true);
        saveCompanyData();
    }

    public void loadCompanyData() {
        if (networkService == null) {
            logger.error("Cannot load company data: networkService is null");
            showStatus("Ошибка: NetworkService равен null", true);
            return;
        }

        if (!networkService.isConnected()) {
            logger.error("Cannot load company data: networkService is not connected");
            showStatus("Ошибка: нет подключения к серверу", true);
            return;
        }

        logger.info("NetworkService connected, sending GET_COMPANY_PROFILE request");
        saveButton.setDisable(true);

        new Thread(() -> {
            try {
                logger.info("Loading company profile data");
                Message message = new Message("REQUEST", "GET_COMPANY_PROFILE", null);
                Response response = networkService.sendAndReceive(message);

                Platform.runLater(() -> {
                    if ("OK".equals(response.getStatus())) {
                        if (response.getData() != null) {
                            Map<String, Object> data = (Map<String, Object>) response.getData();
                            companyNameField.setText((String) data.getOrDefault("companyName", ""));
                            websiteField.setText((String) data.getOrDefault("website", ""));
                            descriptionArea.setText((String) data.getOrDefault("description", ""));
                            showStatus("Данные загружены", false);
                            dataLoaded = true;
                        } else {
                            showStatus("Профиль компании не найден. Заполните данные.", false);
                        }
                    } else {
                        logger.error("Failed to load company profile: {}", response.getMessage());
                        showStatus(response.getMessage(), true);
                    }
                    saveButton.setDisable(false);
                });
            } catch (Exception e) {
                logger.error("Error loading company data", e);
                Platform.runLater(() -> {
                    showStatus("Ошибка загрузки: " + e.getMessage(), true);
                    saveButton.setDisable(false);
                });
            }
        }).start();
    }

    private void saveCompanyData() {
        if (networkService == null) {
            logger.error("Cannot save company data: networkService is null");
            showStatus("Ошибка: NetworkService равен null", true);
            saveButton.setDisable(false);
            return;
        }

        if (!networkService.isConnected()) {
            logger.error("Cannot save company data: networkService is not connected");
            showStatus("Ошибка: нет подключения к серверу", true);
            saveButton.setDisable(false);
            return;
        }

        Map<String, String> data = new HashMap<>();
        data.put("companyName", companyNameField.getText().trim());
        data.put("website", websiteField.getText() != null ? websiteField.getText().trim() : "");
        data.put("description", descriptionArea.getText() != null ? descriptionArea.getText().trim() : "");

        logger.info("Saving company profile: {}", data.get("companyName"));

        new Thread(() -> {
            try {
                Message message = new Message("REQUEST", "SAVE_COMPANY_PROFILE", data);
                Response response = networkService.sendAndReceive(message);

                Platform.runLater(() -> {
                    saveButton.setDisable(false);
                    if ("OK".equals(response.getStatus())) {
                        logger.info("Company profile saved successfully");
                        showStatus("Данные сохранены", false);
                        dataLoaded = true;
                        loadCompanyData();
                    } else {
                        logger.error("Failed to save company profile: {}", response.getMessage());
                        showStatus(response.getMessage(), true);
                    }
                });
            } catch (Exception e) {
                logger.error("Error saving company data", e);
                Platform.runLater(() -> {
                    saveButton.setDisable(false);
                    showStatus("Ошибка сохранения: " + e.getMessage(), true);
                });
            }
        }).start();
    }

    public boolean isDataLoaded() {
        return dataLoaded;
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setVisible(true);
        statusLabel.getStyleClass().clear();
        if (isError) {
            statusLabel.getStyleClass().add("label-error");
        } else {
            statusLabel.getStyleClass().add("label-secondary");
        }
    }
}