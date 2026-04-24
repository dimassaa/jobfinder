package com.jobapp.client.controller;

import com.jobapp.client.AppContext;
import com.jobapp.client.NetworkService;
import com.jobapp.client.service.NotificationClientService;
import com.jobapp.client.service.ToastService;
import com.jobapp.common.dto.NotificationDto;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.shape.Circle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    private static MainController instance;

    @FXML
    private Label welcomeLabel;

    @FXML
    private TabPane mainTabPane;

    @FXML
    private Label notificationBell;

    @FXML
    private Circle badgeCircle;

    @FXML
    private Label badgeLabel;

    private NotificationClientService notificationClientService;
    private int unreadCount = 0;

    @FXML
    private void initialize() {
        logger.info("MainController initialized");
        
        instance = this;

        String email = AppContext.getCurrentEmail();
        if (email != null && !email.isEmpty()) {
            welcomeLabel.setText("Добро пожаловать, " + email);
        } else {
            welcomeLabel.setText("Добро пожаловать");
        }

        notificationBell.setCursor(Cursor.HAND);

        registerNotificationCallback();
        loadUnreadCount();
        
        setupTabSelectionListener();
    }

    private void setupTabSelectionListener() {
        if (mainTabPane != null) {
            mainTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
                if (newTab != null && "Уведомления".equals(newTab.getText())) {
                    refreshUnreadCount();
                }
            });
        }
    }

    private void registerNotificationCallback() {
        NetworkService networkService = AppContext.getNetworkService();
        if (networkService != null) {
            notificationClientService = new NotificationClientService(networkService);
            networkService.setNotificationCallback(notification -> {
                incrementUnreadCount();
                ToastService.showNotification(notification);
            });
            logger.info("Registered notification callback for toast display");
        } else {
            logger.warn("NetworkService not available, cannot register notification callback");
        }
    }

    private void loadUnreadCount() {
        if (notificationClientService == null) {
            NetworkService networkService = AppContext.getNetworkService();
            if (networkService != null) {
                notificationClientService = new NotificationClientService(networkService);
            } else {
                return;
            }
        }

        new Thread(() -> {
            try {
                int count = notificationClientService.getUnreadCount();
                Platform.runLater(() -> updateBadge(count));
            } catch (Exception e) {
                logger.error("Failed to load unread count: {}", e.getMessage());
            }
        }).start();
    }

    private void updateBadge(int count) {
        unreadCount = count;
        if (count > 0) {
            badgeCircle.setVisible(true);
            badgeLabel.setVisible(true);
            badgeLabel.setText(count > 99 ? "99+" : String.valueOf(count));
        } else {
            badgeCircle.setVisible(false);
            badgeLabel.setVisible(false);
        }
    }

    private void incrementUnreadCount() {
        Platform.runLater(() -> updateBadge(unreadCount + 1));
    }

    public void decrementUnreadCount() {
        Platform.runLater(() -> {
            if (unreadCount > 0) {
                updateBadge(unreadCount - 1);
            }
        });
    }

    @FXML
    private void handleNotificationClick() {
        if (mainTabPane != null) {
            mainTabPane.getSelectionModel().select(6);
        }
    }

    public void refreshUnreadCount() {
        loadUnreadCount();
    }

    public static MainController getInstance() {
        return instance;
    }
}
