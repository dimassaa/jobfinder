package com.jobapp.client.controller;

import com.jobapp.client.AppContext;
import com.jobapp.client.NetworkService;
import com.jobapp.client.service.RecommendationClientService;
import com.jobapp.common.dto.RecommendedVacancyDto;
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

import java.util.List;

public class RecommendationsController {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationsController.class);

    @FXML
    private TableView<RecommendedVacancyDto> recommendationsTable;

    @FXML
    private TableColumn<RecommendedVacancyDto, String> titleColumn;

    @FXML
    private TableColumn<RecommendedVacancyDto, String> companyColumn;

    @FXML
    private TableColumn<RecommendedVacancyDto, String> cityColumn;

    @FXML
    private TableColumn<RecommendedVacancyDto, String> salaryColumn;

    @FXML
    private TableColumn<RecommendedVacancyDto, Double> matchPercentColumn;

    @FXML
    private Label statusLabel;

    private RecommendationClientService recommendationClientService;
    private ObservableList<RecommendedVacancyDto> recommendationsList = FXCollections.observableArrayList();

    public RecommendationsController() {
    }

    public void setNetworkService(NetworkService networkService) {
        this.recommendationClientService = new RecommendationClientService(networkService);
    }

    @FXML
    private void initialize() {
        logger.info("RecommendationsController initialized");

        if (recommendationClientService == null) {
            NetworkService ns = AppContext.getNetworkService();
            if (ns != null && ns.isConnected()) {
                recommendationClientService = new RecommendationClientService(ns);
            } else {
                logger.error("NetworkService not available or disconnected");
                return;
            }
        }

        setupTableColumns();
        recommendationsTable.setItems(recommendationsList);

        loadRecommendations();
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

        matchPercentColumn.setCellValueFactory(new PropertyValueFactory<>("matchPercent"));
        matchPercentColumn.setCellFactory(col -> new javafx.scene.control.TableCell<RecommendedVacancyDto, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("%.1f%%", item));
                    double percentage = item / 100.0;
                    if (percentage >= 0.7) {
                        setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                    } else if (percentage >= 0.4) {
                        setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #2196F3;");
                    }
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
        return String.format("%,d", number).replace(",", " ") + " ₽";
    }

    private void loadRecommendations() {
        statusLabel.setVisible(false);

        new Thread(() -> {
            try {
                List<RecommendedVacancyDto> recommendations = recommendationClientService.getRecommendations();
                Platform.runLater(() -> {
                    recommendationsList.clear();
                    recommendationsList.addAll(recommendations);
                    statusLabel.setText("Рекомендаций: " + recommendations.size());
                    statusLabel.setVisible(true);
                });
            } catch (Exception e) {
                logger.error("Failed to load recommendations: {}", e.getMessage());
                Platform.runLater(() -> {
                    statusLabel.setText("Ошибка загрузки: " + e.getMessage());
                    statusLabel.setVisible(true);
                });
            }
        }).start();
    }

    @FXML
    private void handleRefresh() {
        loadRecommendations();
    }

    @FXML
    private void handleTableClick(MouseEvent event) {
        if (event.getClickCount() == 2) {
            RecommendedVacancyDto selectedVacancy = recommendationsTable.getSelectionModel().getSelectedItem();
            if (selectedVacancy != null) {
                openVacancyDetails(selectedVacancy);
            }
        }
    }

    private void openVacancyDetails(RecommendedVacancyDto vacancy) {
        logger.info("Opening vacancy details from recommendations: {} (id={})", vacancy.getTitle(), vacancy.getId());

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