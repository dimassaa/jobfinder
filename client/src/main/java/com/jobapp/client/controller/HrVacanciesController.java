package com.jobapp.client.controller;

import com.jobapp.client.AppContext;
import com.jobapp.client.NetworkService;
import com.jobapp.client.service.VacancyClientService;
import com.jobapp.common.dto.VacancyDto;
import com.jobapp.common.dto.VacancyFilterDto;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class HrVacanciesController {

    private static final Logger logger = LoggerFactory.getLogger(HrVacanciesController.class);

    @FXML
    private TableView<VacancyDto> vacanciesTable;

    @FXML
    private TableColumn<VacancyDto, String> titleColumn;

    @FXML
    private TableColumn<VacancyDto, String> cityColumn;

    @FXML
    private TableColumn<VacancyDto, String> salaryColumn;

    @FXML
    private TableColumn<VacancyDto, String> employmentTypeColumn;

    @FXML
    private TableColumn<VacancyDto, String> statusColumn;

    @FXML
    private TableColumn<VacancyDto, String> createdAtColumn;

    @FXML
    private TableColumn<VacancyDto, Void> actionsColumn;

    @FXML
    private Label statusLabel;

    @FXML
    private Button addVacancyButton;

    @FXML
    private Button refreshButton;

    private VacancyClientService vacancyClientService;
    private ObservableList<VacancyDto> vacancies = FXCollections.observableArrayList();
    private ProgressIndicator loadingIndicator;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @FXML
    private void initialize() {
        NetworkService networkService = AppContext.getNetworkService();
        if (networkService == null || !networkService.isConnected()) {
            logger.error("NetworkService not available or disconnected");
            return;
        }
        vacancyClientService = new VacancyClientService(networkService);

        setupTableColumns();
        setupStatusLabel();

        vacanciesTable.setItems(vacancies);

        Platform.runLater(() -> loadVacancies());
    }

    private void setupTableColumns() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        cityColumn.setCellValueFactory(new PropertyValueFactory<>("city"));
        employmentTypeColumn.setCellValueFactory(new PropertyValueFactory<>("employmentType"));
        statusColumn.setCellValueFactory(cellData -> {
            Boolean isActive = cellData.getValue().getIsActive();
            String status = (isActive != null && isActive) ? "Активна" : "Закрыта";
            return new javafx.beans.property.SimpleStringProperty(status);
        });
        createdAtColumn.setCellValueFactory(cellData -> {
            LocalDateTime dateTime = cellData.getValue().getCreatedAt();
            return new javafx.beans.property.SimpleStringProperty(
                    dateTime != null ? dateTime.format(DATE_FORMATTER) : ""
            );
        });
        salaryColumn.setCellValueFactory(cellData -> {
            VacancyDto vacancy = cellData.getValue();
            String salary = formatSalary(vacancy.getSalaryMin(), vacancy.getSalaryMax());
            return new javafx.beans.property.SimpleStringProperty(salary);
        });

        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Ред.");
            private final Button closeBtn = new Button("Закр.");

            {
                editBtn.setOnAction(e -> {
                    VacancyDto vacancy = getTableView().getItems().get(getIndex());
                    handleEditVacancy(vacancy);
                });
                closeBtn.setOnAction(e -> {
                    VacancyDto vacancy = getTableView().getItems().get(getIndex());
                    handleCloseVacancy(vacancy);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    VacancyDto vacancy = getTableView().getItems().get(getIndex());
                    boolean isActive = vacancy.getIsActive() != null && vacancy.getIsActive();
                    closeBtn.setVisible(isActive);
                    setGraphic(new javafx.scene.layout.HBox(editBtn, closeBtn));
                }
            }
        });
    }

    private String formatSalary(Integer salaryMin, Integer salaryMax) {
        if (salaryMin == null && salaryMax == null) {
            return "Не указана";
        }
        StringBuilder sb = new StringBuilder();
        if (salaryMin != null) {
            sb.append("от ").append(formatNumber(salaryMin));
        }
        if (salaryMax != null) {
            if (sb.length() > 0) {
                sb.append(" - ");
            }
            sb.append("до ").append(formatNumber(salaryMax));
        }
        return sb.toString();
    }

    private String formatNumber(Integer number) {
        if (number == null) return "";
        return String.format("%,d", number).replace(",", " ") + " $";
    }

    private void setupStatusLabel() {
        statusLabel.setWrapText(true);
    }

    @FXML
    private void handleAddVacancy() {
        logger.info("Opening vacancy creation window");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/vacancy_edit.fxml"));

            VacancyEditController controller = new VacancyEditController();
            NetworkService networkService = AppContext.getNetworkService();
            if (networkService != null) {
                controller.setNetworkService(networkService);
            }
            loader.setController(controller);

            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Создание вакансии");
            stage.setScene(new Scene(root, 700, 650));
            stage.initOwner(vacanciesTable.getScene().getWindow());
            stage.setResizable(true);
            stage.show();

            logger.info("Vacancy creation window opened");
        } catch (Exception e) {
            logger.error("Failed to open vacancy creation: {}", e.getMessage());
            showStatus("Ошибка открытия формы: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleRefresh() {
        loadVacancies();
    }

    @FXML
    private void handleTableClick(javafx.scene.input.MouseEvent event) {
        if (event.getClickCount() == 2) {
            VacancyDto selectedVacancy = vacanciesTable.getSelectionModel().getSelectedItem();
            if (selectedVacancy != null) {
                handleEditVacancy(selectedVacancy);
            }
        }
    }

    private void handleEditVacancy(VacancyDto vacancy) {
        logger.info("Opening vacancy edit for: {} (id={})", vacancy.getTitle(), vacancy.getId());
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/vacancy_edit.fxml"));

            VacancyEditController controller = new VacancyEditController();
            controller.setEditingVacancy(vacancy);

            NetworkService networkService = AppContext.getNetworkService();
            if (networkService != null) {
                controller.setNetworkService(networkService);
            }
            loader.setController(controller);

            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Редактирование вакансии: " + vacancy.getTitle());
            stage.setScene(new Scene(root, 700, 650));
            stage.initOwner(vacanciesTable.getScene().getWindow());
            stage.setResizable(true);
            stage.show();

        } catch (Exception e) {
            logger.error("Failed to open vacancy edit: {}", e.getMessage());
            showStatus("Ошибка открытия формы: " + e.getMessage(), true);
        }
    }

    private void handleCloseVacancy(VacancyDto vacancy) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Закрытие вакансии");
        confirmAlert.setHeaderText("Вы уверены, что хотите закрыть вакансию?");
        confirmAlert.setContentText("Вакансия: " + vacancy.getTitle());

        confirmAlert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                closeVacancy(vacancy.getId());
            }
        });
    }

    private void closeVacancy(int vacancyId) {
        new Thread(() -> {
            try {
                vacancyClientService.close(vacancyId);
                Platform.runLater(() -> {
                    showStatus("Вакансия закрыта", false);
                    loadVacancies();
                });
            } catch (Exception e) {
                logger.error("Error closing vacancy", e);
                Platform.runLater(() -> showStatus("Ошибка закрытия: " + e.getMessage(), true));
            }
        }).start();
    }

    private void loadVacancies() {
        showLoading(true);
        hideStatus();

        new Thread(() -> {
            try {
                List<VacancyDto> results = vacancyClientService.getMyVacancies();

                Platform.runLater(() -> {
                    vacancies.clear();
                    vacancies.addAll(results);
                    showStatus("Загружено вакансий: " + results.size(), false);
                });
            } catch (Exception e) {
                logger.error("Error loading vacancies", e);
                Platform.runLater(() -> showStatus("Ошибка загрузки: " + e.getMessage(), true));
            } finally {
                Platform.runLater(() -> showLoading(false));
            }
        }).start();
    }

    private void showLoading(boolean show) {
        // Простая индикация загрузки через статус
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