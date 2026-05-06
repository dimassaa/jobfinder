package com.jobapp.client.controller;

import com.jobapp.client.AppContext;
import com.jobapp.client.NetworkService;
import com.jobapp.client.service.NotificationClientService;
import com.jobapp.common.dto.NotificationDto;
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

public class NotificationsController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationsController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @FXML
    private TableView<NotificationDto> notificationsTable;

    @FXML
    private TableColumn<NotificationDto, String> typeColumn;

    @FXML
    private TableColumn<NotificationDto, String> titleColumn;

    @FXML
    private TableColumn<NotificationDto, String> contentColumn;

    @FXML
    private TableColumn<NotificationDto, String> createdAtColumn;

    @FXML
    private TableColumn<NotificationDto, String> statusColumn;

    @FXML
    private Label statusLabel;

    private NotificationClientService notificationClientService;
    private ObservableList<NotificationDto> notificationsList = FXCollections.observableArrayList();

    public NotificationsController() {
    }

    public void setNetworkService(NetworkService networkService) {
        this.notificationClientService = new NotificationClientService(networkService);
    }

    @FXML
    private void initialize() {
        logger.info("NotificationsController initialized");

        if (notificationClientService == null) {
            NetworkService ns = AppContext.getNetworkService();
            if (ns != null && ns.isConnected()) {
                notificationClientService = new NotificationClientService(ns);
            } else {
                logger.error("NetworkService not available or disconnected");
                return;
            }
        }

        setupTableColumns();
        notificationsTable.setItems(notificationsList);

        loadNotifications();
    }

    private void setupTableColumns() {
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        contentColumn.setCellValueFactory(new PropertyValueFactory<>("content"));

        createdAtColumn.setCellValueFactory(cellData -> {
            LocalDateTime dateTime = cellData.getValue().getCreatedAt();
            return new javafx.beans.property.SimpleStringProperty(
                    dateTime != null ? dateTime.format(DATE_FORMATTER) : "");
        });

        statusColumn.setCellValueFactory(cellData -> {
            Boolean isRead = cellData.getValue().getIsRead();
            return new javafx.beans.property.SimpleStringProperty(
                    Boolean.TRUE.equals(isRead) ? "Прочитано" : "Новое");
        });
    }

    private void loadNotifications() {
        statusLabel.setVisible(false);

        new Thread(() -> {
            try {
                List<NotificationDto> notifications = notificationClientService.getNotifications(false, 0, 100);
                Platform.runLater(() -> {
                    notificationsList.clear();
                    notificationsList.addAll(notifications);
                    statusLabel.setText("Уведомлений: " + notifications.size());
                    statusLabel.setVisible(true);
                });
            } catch (Exception e) {
                logger.error("Failed to load notifications: {}", e.getMessage());
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
            NotificationDto selected = notificationsTable.getSelectionModel().getSelectedItem();
            if (selected != null && selected.getLink() != null && !selected.getLink().isEmpty()) {
                openLink(selected.getLink());
            }
        }
    }

    private void openLink(String link) {
        logger.info("Opening notification link: {}", link);

        try {
            String[] parts = link.split(":");
            if (parts.length < 2) {
                return;
            }

            String type = parts[0];
            int id = Integer.parseInt(parts[1]);

            if ("vacancy".equals(type)) {
                openVacancyDetails(id);
            }
        } catch (Exception e) {
            logger.error("Failed to parse notification link: {}", e.getMessage());
        }
    }

    private void openVacancyDetails(int vacancyId) {
        logger.info("Opening vacancy details from notification: {}", vacancyId);

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
    private void handleMarkRead() {
        NotificationDto selected = notificationsTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showError("Выберите уведомление");
            return;
        }

        final boolean wasUnread = !Boolean.TRUE.equals(selected.getIsRead());

        new Thread(() -> {
            try {
                notificationClientService.markAsRead(selected.getId());
                Platform.runLater(() -> {
                    showInfo("Уведомление отмечено как прочитанное");
                    loadNotifications();
                    if (wasUnread && MainController.getInstance() != null) {
                        MainController.getInstance().decrementUnreadCount();
                    }
                });
            } catch (Exception e) {
                logger.error("Failed to mark notification as read: {}", e.getMessage());
                Platform.runLater(() -> showError("Ошибка: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleDelete() {
        NotificationDto selected = notificationsTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showError("Выберите уведомление");
            return;
        }

        final boolean wasUnread = !Boolean.TRUE.equals(selected.getIsRead());

        new Thread(() -> {
            try {
                notificationClientService.delete(selected.getId());
                Platform.runLater(() -> {
                    showInfo("Уведомление удалено");
                    loadNotifications();
                    if (wasUnread && MainController.getInstance() != null) {
                        MainController.getInstance().decrementUnreadCount();
                    }
                });
            } catch (Exception e) {
                logger.error("Failed to delete notification: {}", e.getMessage());
                Platform.runLater(() -> showError("Ошибка: " + e.getMessage()));
            }
        }).start();
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