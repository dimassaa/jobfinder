package com.jobapp.client.controller;

import com.jobapp.client.AppContext;
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
    }
}