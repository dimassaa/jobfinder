package com.jobapp.client.controller;

import com.jobapp.client.AppContext;
import com.jobapp.client.NetworkService;
import com.jobapp.client.service.NotificationClientService;
import com.jobapp.client.service.ToastService;
import com.jobapp.common.dto.NotificationDto;
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
    private List<Tab> tabsToRemove = new ArrayList<>();

    @FXML
    private void initialize() {
        logger.info("MainController initialized");

        instance = this;

        try {
            String email = AppContext.getCurrentEmail();
            if (email != null && !email.isEmpty()) {
                welcomeLabel.setText("Добро пожаловать, " + email);
            } else {
                welcomeLabel.setText("Добро пожаловать");
            }

            if (notificationBell != null) {
                notificationBell.setCursor(Cursor.HAND);
            }

            registerNotificationCallback();
            loadUnreadCount();

            setupTabSelectionListener();
            setupRoleBasedTabs();
        } catch (Exception e) {
            logger.error("Error during MainController initialization", e);
        }
    }

    private void setupRoleBasedTabs() {
        if (mainTabPane == null) return;

        String role = AppContext.getCurrentRole();
        boolean isHr = "HR".equals(role);
        boolean isAdmin = "ADMIN".equals(role);

        for (Tab tab : mainTabPane.getTabs()) {
            String tabText = tab.getText();

            if ("Мои отклики".equals(tabText) || "Избранное".equals(tabText) ||
                "История просмотров".equals(tabText) || "Рекомендации".equals(tabText)) {
                tab.setDisable(isHr || isAdmin);
            }

            if ("Вакансии".equals(tabText)) {
                tab.setDisable(false);
            }

            if ("Администрирование".equals(tabText)) {
                tab.setDisable(!isAdmin);
                if (!isAdmin) {
                    tabsToRemove.add(tab);
                }
            }
        }
        
        mainTabPane.getTabs().removeAll(tabsToRemove);
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
    private void handleProfileClick() {
        logger.info("Opening profile window");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/profile.fxml"));

            ProfileController controller = new ProfileController();
            NetworkService networkService = AppContext.getNetworkService();
            if (networkService != null) {
                controller.setNetworkService(networkService);
            }
            loader.setController(controller);

            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Мой профиль");
            stage.setScene(new Scene(root, 650, 550));
            stage.initOwner(mainTabPane.getScene().getWindow());
            stage.setResizable(true);
            stage.show();

            logger.info("Profile window opened");
        } catch (Exception e) {
            logger.error("Failed to open profile: {}", e.getMessage());
        }
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
