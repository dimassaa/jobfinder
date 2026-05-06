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

    @FXML
    private Button refreshButton;

    private NetworkService networkService;
    private ObjectMapper objectMapper;

    @FXML
    private void initialize() {
        networkService = AppContext.getNetworkService();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        setupStatusLabel();

        Platform.runLater(() -> loadCompanyData());
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

        saveCompanyData();
    }

    @FXML
    private void handleRefresh() {
        loadCompanyData();
    }

    private void loadCompanyData() {
        new Thread(() -> {
            try {
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
                        } else {
                            showStatus("Профиль компании не найден. Заполните данные.", false);
                        }
                    } else {
                        showStatus(response.getMessage(), true);
                    }
                });
            } catch (Exception e) {
                logger.error("Error loading company data", e);
                Platform.runLater(() -> showStatus("Ошибка загрузки: " + e.getMessage(), true));
            }
        }).start();
    }

    private void saveCompanyData() {
        Map<String, String> data = new HashMap<>();
        data.put("companyName", companyNameField.getText());
        data.put("website", websiteField.getText() != null ? websiteField.getText() : "");
        data.put("description", descriptionArea.getText() != null ? descriptionArea.getText() : "");

        new Thread(() -> {
            try {
                Message message = new Message("REQUEST", "SAVE_COMPANY_PROFILE", data);
                Response response = networkService.sendAndReceive(message);

                Platform.runLater(() -> {
                    if ("OK".equals(response.getStatus())) {
                        showStatus("Данные сохранены", false);
                    } else {
                        showStatus(response.getMessage(), true);
                    }
                });
            } catch (Exception e) {
                logger.error("Error saving company data", e);
                Platform.runLater(() -> showStatus("Ошибка сохранения: " + e.getMessage(), true));
            }
        }).start();
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