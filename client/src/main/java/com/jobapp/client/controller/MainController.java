package com.jobapp.client.controller;

import com.jobapp.client.AppContext;
import com.jobapp.client.NetworkService;
import com.jobapp.client.service.ToastService;
import com.jobapp.common.dto.NotificationDto;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @FXML
    private Label welcomeLabel;

    @FXML
    private TabPane mainTabPane;

    @FXML
    private void initialize() {
        logger.info("MainController initialized");

        String email = AppContext.getCurrentEmail();
        if (email != null && !email.isEmpty()) {
            welcomeLabel.setText("Добро пожаловать, " + email);
        } else {
            welcomeLabel.setText("Добро пожаловать");
        }

        registerNotificationCallback();
    }

    private void registerNotificationCallback() {
        NetworkService networkService = AppContext.getNetworkService();
        if (networkService != null) {
            networkService.setNotificationCallback(ToastService::showNotification);
            logger.info("Registered notification callback for toast display");
        } else {
            logger.warn("NetworkService not available, cannot register notification callback");
        }
    }
}
