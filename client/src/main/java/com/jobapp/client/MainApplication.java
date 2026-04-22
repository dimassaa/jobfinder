package com.jobapp.client;

import com.jobapp.client.controller.LoginController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApplication extends Application {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MainApplication.class);

    @Override
    public void start(Stage primaryStage) throws Exception {
        logger.info("Starting JobFinder Client Application");

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
        Parent root = loader.load();

        LoginController loginController = loader.getController();
        loginController.setNetworkService(new NetworkService());

        primaryStage.setTitle("JobMatch - Вход");
        primaryStage.setScene(new Scene(root, 400, 500));
        primaryStage.setResizable(false);
        primaryStage.show();

        logger.info("Application started successfully");
    }

    public static void main(String[] args) {
        launch(args);
    }
}