package com.jobapp.client;

import com.jobapp.client.controller.LoginController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.concurrent.Worker;

import java.io.IOException;

public class MainApplication extends Application {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MainApplication.class);
    private static final int MAX_RETRIES = 5;
    private static final int RETRY_DELAY_MS = 2000;

    private NetworkService networkService;

    @Override
    public void start(Stage primaryStage) throws Exception {
        logger.info("Starting JobFinder Client Application");
        logger.info("Server config: {}", AppContext.getServerConfigInfo());

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
        Parent root = loader.load();

        networkService = new NetworkService();

        boolean connected = attemptConnection();

        LoginController loginController = loader.getController();
        loginController.setNetworkService(networkService);
        loginController.updateConnectionStatus();

        primaryStage.setTitle("JobMatch - Вход");
        primaryStage.setScene(new Scene(root, 400, 500));
        primaryStage.setResizable(false);
        primaryStage.show();

        if (!connected) {
            startBackgroundReconnect();
        }

        logger.info("Application started successfully");
    }

    private boolean attemptConnection() {
        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            attempt++;
            try {
                logger.info("Attempting to connect to server (attempt {}/{})", attempt, MAX_RETRIES);
                networkService.connect();
                logger.info("Successfully connected to server at {}:{}",
                    AppContext.getServerHost(), AppContext.getServerPort());
                return true;
            } catch (IOException e) {
                logger.warn("Connection attempt {} failed: {}", attempt, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        logger.error("Failed to connect to server after {} attempts. Please check if server is running.", MAX_RETRIES);
        return false;
    }

    private void startBackgroundReconnect() {
        logger.info("Starting background reconnection task");
        new Thread(() -> {
            int attempt = 1;
            while (attempt <= 10) {
                try {
                    logger.info("Background reconnect attempt {}/10", attempt);
                    Thread.sleep(3000);
                    if (!networkService.isConnected()) {
                        try {
                            networkService.connect();
                            logger.info("Background reconnection successful!");
                            Platform.runLater(() -> {
                                LoginController controller = getLoginController();
                                if (controller != null) {
                                    controller.updateConnectionStatus();
                                }
                            });
                            break;
                        } catch (IOException e) {
                            logger.warn("Background reconnect attempt failed: {}", e.getMessage());
                        }
                    } else {
                        break;
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
                attempt++;
            }
        }).start();
    }

    private LoginController getLoginController() {
        return null;
    }

    public static void main(String[] args) {
        launch(args);
    }
}