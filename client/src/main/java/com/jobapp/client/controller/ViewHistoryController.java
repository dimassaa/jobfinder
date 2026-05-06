package com.jobapp.client.controller;

import com.jobapp.client.AppContext;
import com.jobapp.client.NetworkService;
import com.jobapp.client.service.ViewHistoryClientService;
import com.jobapp.common.dto.VacancyDto;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
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

public class ViewHistoryController {

    private static final Logger logger = LoggerFactory.getLogger(ViewHistoryController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @FXML
    private TableView<VacancyDto> historyTable;

    @FXML
    private TableColumn<VacancyDto, String> titleColumn;

    @FXML
    private TableColumn<VacancyDto, String> companyColumn;

    @FXML
    private TableColumn<VacancyDto, String> cityColumn;

    @FXML
    private TableColumn<VacancyDto, String> salaryColumn;

    @FXML
    private TableColumn<VacancyDto, String> viewedAtColumn;

    @FXML
    private Label statusLabel;

    private ViewHistoryClientService viewHistoryClientService;
    private ObservableList<VacancyDto> historyList = FXCollections.observableArrayList();

    public ViewHistoryController() {
    }

    public void setNetworkService(NetworkService networkService) {
        this.viewHistoryClientService = new ViewHistoryClientService(networkService);
    }

    @FXML
    private void initialize() {
        logger.info("ViewHistoryController initialized");

        if (viewHistoryClientService == null) {
            NetworkService ns = AppContext.getNetworkService();
            if (ns != null && ns.isConnected()) {
                viewHistoryClientService = new ViewHistoryClientService(ns);
            } else {
                logger.error("NetworkService not available or disconnected");
                return;
            }
        }

        setupTableColumns();
        historyTable.setItems(historyList);

        loadHistory();
    }

    private void setupTableColumns() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        companyColumn.setCellValueFactory(new PropertyValueFactory<>("companyName"));
        cityColumn.setCellValueFactory(new PropertyValueFactory<>("city"));

        salaryColumn.setCellValueFactory(cellData -> {
            VacancyDto vacancy = cellData.getValue();
            String salary = formatSalary(vacancy.getSalaryMin(), vacancy.getSalaryMax());
            return new javafx.beans.property.SimpleStringProperty(salary);
        });

        viewedAtColumn.setCellValueFactory(cellData -> {
            LocalDateTime dateTime = cellData.getValue().getCreatedAt();
            return new javafx.beans.property.SimpleStringProperty(
                    dateTime != null ? dateTime.format(DATE_FORMATTER) : "");
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
        return String.format("%,d", number).replace(",", " ") + " ₽";
    }

    private void loadHistory() {
        statusLabel.setVisible(false);

        new Thread(() -> {
            try {
                List<VacancyDto> views = viewHistoryClientService.getRecentViews();
                final List<VacancyDto> viewList = views != null ? views : new java.util.ArrayList<>();
                Platform.runLater(() -> {
                    historyList.clear();
                    historyList.addAll(viewList);
                    statusLabel.setText("Просмотрено вакансий: " + viewList.size());
                    statusLabel.setVisible(true);
                });
            } catch (Exception e) {
                logger.error("Failed to load view history: {}", e.getMessage());
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
            VacancyDto selectedVacancy = historyTable.getSelectionModel().getSelectedItem();
            if (selectedVacancy != null) {
                openVacancyDetails(selectedVacancy);
            }
        }
    }

    private void openVacancyDetails(VacancyDto vacancy) {
        logger.info("Opening vacancy details from history: {} (id={})", vacancy.getTitle(), vacancy.getId());

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/vacancy_details.fxml"));

            VacancyDetailsController controller = new VacancyDetailsController();
            controller.setVacancyId(vacancy.getId());

            NetworkService networkService = AppContext.getNetworkService();
            if (networkService != null) {
                controller.setNetworkService(networkService);
            }
            loader.setController(controller);

            Parent root = loader.load();

            Stage detailsStage = new Stage();
            detailsStage.setTitle("Вакансия: " + vacancy.getTitle());
            detailsStage.setScene(new Scene(root, 600, 700));
            detailsStage.setResizable(true);
            detailsStage.show();

        } catch (Exception e) {
            logger.error("Failed to open vacancy details: {}", e.getMessage());
            showError("Не удалось открыть детали вакансии: " + e.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}