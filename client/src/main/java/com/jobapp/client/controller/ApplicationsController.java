package com.jobapp.client.controller;

import com.jobapp.client.AppContext;
import com.jobapp.client.NetworkService;
import com.jobapp.client.service.ApplicationClientService;
import com.jobapp.common.dto.ApplicationDto;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ApplicationsController {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationsController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @FXML
    private TableView<ApplicationDto> applicationsTable;

    @FXML
    private TableColumn<ApplicationDto, String> titleColumn;

    @FXML
    private TableColumn<ApplicationDto, String> companyColumn;

    @FXML
    private TableColumn<ApplicationDto, String> statusColumn;

    @FXML
    private TableColumn<ApplicationDto, String> createdAtColumn;

    @FXML
    private Button refreshButton;

    @FXML
    private Label statusLabel;

    private ApplicationClientService applicationClientService;
    private ObservableList<ApplicationDto> applicationsList = FXCollections.observableArrayList();

    public ApplicationsController() {
    }

    public void setNetworkService(NetworkService networkService) {
        this.applicationClientService = new ApplicationClientService(networkService);
    }

    @FXML
    private void initialize() {
        logger.info("ApplicationsController initialized");

        NetworkService networkService = AppContext.getNetworkService();
        if (networkService == null || !networkService.isConnected()) {
            logger.error("NetworkService not available or disconnected");
            Platform.runLater(() -> {
                statusLabel.setText("Ошибка: нет подключения к серверу");
                statusLabel.setVisible(true);
            });
            return;
        }
        this.applicationClientService = new ApplicationClientService(networkService);

        setupTableColumns();
        applicationsTable.setItems(applicationsList);

        loadApplications();
    }

    private void setupTableColumns() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("vacancyTitle"));
        companyColumn.setCellValueFactory(new PropertyValueFactory<>("userName"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        createdAtColumn.setCellValueFactory(cellData -> {
            LocalDateTime dateTime = cellData.getValue().getCreatedAt();
            return new javafx.beans.property.SimpleStringProperty(
                    dateTime != null ? dateTime.format(DATE_FORMATTER) : "");
        });
    }

    private void loadApplications() {
        statusLabel.setVisible(false);

        new Thread(() -> {
            try {
                List<ApplicationDto> applications = applicationClientService.getMyApplications();
                Platform.runLater(() -> {
                    applicationsList.clear();
                    applicationsList.addAll(applications);
                    statusLabel.setText("Откликов: " + applications.size());
                    statusLabel.setVisible(true);
                });
            } catch (Exception e) {
                logger.error("Failed to load applications: {}", e.getMessage());
                Platform.runLater(() -> {
                    statusLabel.setText("Ошибка загрузки: " + e.getMessage());
                    statusLabel.setVisible(true);
                });
            }
        }).start();
    }

    @FXML
    private void handleTableClick(MouseEvent event) {
        if (event.getClickCount() == 2) {
            ApplicationDto selected = applicationsTable.getSelectionModel().getSelectedItem();
            if (selected != null && selected.getVacancyId() != null) {
                openVacancyDetails(selected.getVacancyId());
            }
        }
    }

    private void openVacancyDetails(int vacancyId) {
        logger.info("Opening vacancy details from applications: {}", vacancyId);

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/vacancy_details.fxml"));

            VacancyDetailsController controller = new VacancyDetailsController();
            controller.setVacancyId(vacancyId);

            NetworkService networkService = AppContext.getNetworkService();
            if (networkService != null) {
                controller.setNetworkService(networkService);
            }
            loader.setController(controller);

            Parent root = loader.load();

            Stage detailsStage = new Stage();
            detailsStage.setTitle("Вакансия");
            detailsStage.setScene(new Scene(root, 600, 700));
            detailsStage.setResizable(true);
            detailsStage.show();

        } catch (Exception e) {
            logger.error("Failed to open vacancy details: {}", e.getMessage());
            showError("Не удалось открыть детали вакансии: " + e.getMessage());
        }
    }

    @FXML
    private void handleRefresh() {
        logger.info("Refreshing applications list");
        loadApplications();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Информация");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}