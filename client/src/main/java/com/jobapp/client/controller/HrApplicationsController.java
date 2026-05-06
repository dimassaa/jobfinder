package com.jobapp.client.controller;

import com.jobapp.client.AppContext;
import com.jobapp.client.NetworkService;
import com.jobapp.client.service.ApplicationClientService;
import com.jobapp.common.dto.ApplicationDto;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class HrApplicationsController {

    private static final Logger logger = LoggerFactory.getLogger(HrApplicationsController.class);

    @FXML
    private TableView<ApplicationDto> applicationsTable;

    @FXML
    private TableColumn<ApplicationDto, String> vacancyTitleColumn;

    @FXML
    private TableColumn<ApplicationDto, String> candidateColumn;

    @FXML
    private TableColumn<ApplicationDto, String> statusColumn;

    @FXML
    private TableColumn<ApplicationDto, String> createdAtColumn;

    @FXML
    private Label statusLabel;

    @FXML
    private ComboBox<String> statusCombo;

    @FXML
    private Button updateStatusButton;

    @FXML
    private Button refreshButton;

    private ApplicationClientService applicationClientService;
    private ObservableList<ApplicationDto> applications = FXCollections.observableArrayList();

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private static final List<String> STATUSES = Arrays.asList(
            "PENDING",
            "INVITED",
            "REJECTED",
            "ACCEPTED"
    );

    @FXML
    private void initialize() {
        NetworkService networkService = AppContext.getNetworkService();
        if (networkService == null || !networkService.isConnected()) {
            logger.error("NetworkService not available or disconnected");
            return;
        }
        applicationClientService = new ApplicationClientService(networkService);

        setupTableColumns();
        setupStatusCombo();
        setupStatusLabel();

        applicationsTable.setItems(applications);

        Platform.runLater(() -> loadApplications());
    }

    private void setupTableColumns() {
        vacancyTitleColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getVacancyTitle() != null) {
                return new javafx.beans.property.SimpleStringProperty(cellData.getValue().getVacancyTitle());
            }
            return new javafx.beans.property.SimpleStringProperty("Вакансия #" + cellData.getValue().getVacancyId());
        });
        candidateColumn.setCellValueFactory(cellData -> {
            String name = cellData.getValue().getUserName();
            if (name != null && !name.isEmpty()) {
                return new javafx.beans.property.SimpleStringProperty(name);
            }
            return new javafx.beans.property.SimpleStringProperty("Соискатель #" + cellData.getValue().getUserId());
        });
        statusColumn.setCellValueFactory(cellData -> {
            String status = cellData.getValue().getStatus();
            String displayStatus = translateStatus(status);
            return new javafx.beans.property.SimpleStringProperty(displayStatus);
        });
        createdAtColumn.setCellValueFactory(cellData -> {
            LocalDateTime dateTime = cellData.getValue().getCreatedAt();
            return new javafx.beans.property.SimpleStringProperty(
                    dateTime != null ? dateTime.format(DATE_FORMATTER) : ""
            );
        });
    }

    private String translateStatus(String status) {
        if (status == null) return "Неизвестно";
        switch (status) {
            case "PENDING": return "На рассмотрении";
            case "INVITED": return "Приглашён";
            case "REJECTED": return "Отклонён";
            case "ACCEPTED": return "Принят";
            default: return status;
        }
    }

    private void setupStatusCombo() {
        ObservableList<String> statuses = FXCollections.observableArrayList(STATUSES);
        statusCombo.setItems(statuses);
    }

    private void setupStatusLabel() {
        statusLabel.setWrapText(true);
    }

    @FXML
    private void handleRefresh() {
        loadApplications();
    }

    @FXML
    private void handleTableClick(javafx.scene.input.MouseEvent event) {
        if (event.getClickCount() == 2) {
            ApplicationDto selectedApp = applicationsTable.getSelectionModel().getSelectedItem();
            if (selectedApp != null) {
                showApplicationDetails(selectedApp);
            }
        }
    }

    @FXML
    private void handleUpdateStatus() {
        ApplicationDto selectedApp = applicationsTable.getSelectionModel().getSelectedItem();
        String newStatus = statusCombo.getValue();

        if (selectedApp == null) {
            showStatus("Выберите отклик из таблицы", true);
            return;
        }

        if (newStatus == null || newStatus.isEmpty()) {
            showStatus("Выберите новый статус", true);
            return;
        }

        updateApplicationStatus(selectedApp.getId(), newStatus);
    }

    private void showApplicationDetails(ApplicationDto application) {
        Alert detailsAlert = new Alert(Alert.AlertType.INFORMATION);
        detailsAlert.setTitle("Детали отклика");
        detailsAlert.setHeaderText("Информация о соискателе");

        String content = "Вакансия: " + (application.getVacancyTitle() != null ? 
                application.getVacancyTitle() : "#" + application.getVacancyId()) + "\n" +
                "ID соискателя: " + application.getUserId() + "\n" +
                "Имя: " + (application.getUserName() != null ? 
                application.getUserName() : "не указано") + "\n" +
                "Статус: " + translateStatus(application.getStatus()) + "\n" +
                "Дата отклика: " + (application.getCreatedAt() != null ? 
                application.getCreatedAt().format(DATE_FORMATTER) : "неизвестно") + "\n\n" +
                "Для получения контактных данных свяжитесь с администратором.";

        detailsAlert.setContentText(content);
        detailsAlert.showAndWait();
    }

    private void updateApplicationStatus(int applicationId, String newStatus) {
        new Thread(() -> {
            try {
                applicationClientService.updateStatus(applicationId, newStatus);
                Platform.runLater(() -> {
                    showStatus("Статус обновлён", false);
                    loadApplications();
                });
            } catch (Exception e) {
                logger.error("Error updating application status", e);
                Platform.runLater(() -> showStatus("Ошибка обновления: " + e.getMessage(), true));
            }
        }).start();
    }

    private void loadApplications() {
        hideStatus();

        new Thread(() -> {
            try {
                List<ApplicationDto> results = applicationClientService.getAllApplicationsForHr();
                final List<ApplicationDto> appList = results != null ? results : new java.util.ArrayList<>();

                Platform.runLater(() -> {
                    applications.clear();
                    applications.addAll(appList);
                    showStatus("Загружено откликов: " + appList.size(), false);
                });
            } catch (Exception e) {
                logger.error("Error loading applications", e);
                Platform.runLater(() -> showStatus("Ошибка загрузки: " + e.getMessage(), true));
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

    private void hideStatus() {
        statusLabel.setVisible(false);
    }
}