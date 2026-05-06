package com.jobapp.client.controller;

import com.jobapp.client.AppContext;
import com.jobapp.client.NetworkService;
import com.jobapp.client.service.NotificationClientService;
import com.jobapp.client.service.ToastService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class HrController {

    private static final Logger logger = LoggerFactory.getLogger(HrController.class);

    private static HrController instance;

    @FXML
    private Label welcomeLabel;

    @FXML
    private TabPane hrTabPane;

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
        logger.info("HrController initialized");

        instance = this;

        try {
            String email = AppContext.getCurrentEmail();
            if (email != null && !email.isEmpty()) {
                welcomeLabel.setText("Панель работодателя - " + email);
            }

            if (notificationBell != null) {
                notificationBell.setCursor(Cursor.HAND);
            }

            registerNotificationCallback();
            loadUnreadCount();

            setupTabSelectionListener();
        } catch (Exception e) {
            logger.error("Error during HrController initialization", e);
        }
    }

    private void setupTabSelectionListener() {
        if (hrTabPane != null) {
            hrTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
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
        if (hrTabPane != null) {
            for (Tab tab : hrTabPane.getTabs()) {
                if ("Уведомления".equals(tab.getText())) {
                    hrTabPane.getSelectionModel().select(tab);
                    return;
                }
            }
        }
    }

    public void refreshUnreadCount() {
        loadUnreadCount();
    }

    public static HrController getInstance() {
        return instance;
    }

    public void selectTab(String tabName) {
        if (hrTabPane != null) {
            for (Tab tab : hrTabPane.getTabs()) {
                if (tabName.equals(tab.getText())) {
                    hrTabPane.getSelectionModel().select(tab);
                    return;
                }
            }
        }
    }
}