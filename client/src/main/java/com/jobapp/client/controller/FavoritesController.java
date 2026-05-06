package com.jobapp.client.controller;

import com.jobapp.client.AppContext;
import com.jobapp.client.NetworkService;
import com.jobapp.client.service.FavoriteClientService;
import com.jobapp.common.dto.VacancyDto;
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

import java.util.List;

public class FavoritesController {

    private static final Logger logger = LoggerFactory.getLogger(FavoritesController.class);

    @FXML
    private TableView<VacancyDto> favoritesTable;

    @FXML
    private TableColumn<VacancyDto, String> titleColumn;

    @FXML
    private TableColumn<VacancyDto, String> companyColumn;

    @FXML
    private TableColumn<VacancyDto, String> cityColumn;

    @FXML
    private TableColumn<VacancyDto, String> salaryColumn;

    @FXML
    private Button removeButton;

    @FXML
    private Button refreshButton;

    @FXML
    private Label statusLabel;

    private FavoriteClientService favoriteClientService;
    private ObservableList<VacancyDto> favoritesList = FXCollections.observableArrayList();

    public FavoritesController() {
    }

    public void setNetworkService(NetworkService networkService) {
        this.favoriteClientService = new FavoriteClientService(networkService);
    }

    @FXML
    private void initialize() {
        logger.info("FavoritesController initialized");

        if (favoriteClientService == null) {
            NetworkService ns = AppContext.getNetworkService();
            if (ns != null && ns.isConnected()) {
                favoriteClientService = new FavoriteClientService(ns);
            } else {
                logger.error("NetworkService not available or disconnected");
                return;
            }
        }

        setupTableColumns();
        favoritesTable.setItems(favoritesList);

        loadFavorites();
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

    private void loadFavorites() {
        statusLabel.setVisible(false);

        new Thread(() -> {
            try {
                List<VacancyDto> favorites = favoriteClientService.getFavorites();
                Platform.runLater(() -> {
                    favoritesList.clear();
                    favoritesList.addAll(favorites);
                    statusLabel.setText("В избранном: " + favorites.size());
                    statusLabel.setVisible(true);
                });
            } catch (Exception e) {
                logger.error("Failed to load favorites: {}", e.getMessage());
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
            VacancyDto selected = favoritesTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                openVacancyDetails(selected.getId());
            }
        }
    }

    private void openVacancyDetails(int vacancyId) {
        logger.info("Opening vacancy details from favorites: {}", vacancyId);

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
    private void handleRemove() {
        VacancyDto selected = favoritesTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showError("Выберите вакансию");
            return;
        }

        new Thread(() -> {
            try {
                favoriteClientService.remove(selected.getId());
                Platform.runLater(() -> {
                    showInfo("Вакансия удалена из избранного");
                    loadFavorites();
                });
            } catch (Exception e) {
                logger.error("Failed to remove from favorites: {}", e.getMessage());
                Platform.runLater(() -> showError("Ошибка: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleRefresh() {
        loadFavorites();
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