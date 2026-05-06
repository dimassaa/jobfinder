package com.jobapp.client.controller;

import com.jobapp.client.NetworkService;
import com.jobapp.client.service.ProfileClientService;
import com.jobapp.common.dto.TagDto;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TagsSelectorController {

    private static final Logger logger = LoggerFactory.getLogger(TagsSelectorController.class);

    @FXML
    private ListView<TagDto> tagsListView;

    @FXML
    private Label statusLabel;

    @FXML
    private Label selectionCountLabel;

    @FXML
    private Button saveButton;

    @FXML
    private Button cancelButton;

    @FXML
    private FlowPane selectedTagsPane;

    private ProfileClientService profileClientService;
    private Runnable onCloseCallback;

    public TagsSelectorController() {
    }

    public void setNetworkService(NetworkService networkService) {
        this.profileClientService = new ProfileClientService(networkService);
    }

    public void setOnCloseCallback(Runnable callback) {
        this.onCloseCallback = callback;
    }

    @FXML
    private void initialize() {
        logger.info("TagsSelectorController initialized");
        saveButton.setDisable(true);
        tagsListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        tagsListView.setCellFactory(new Callback<ListView<TagDto>, ListCell<TagDto>>() {
            @Override
            public ListCell<TagDto> call(ListView<TagDto> param) {
                return new ListCell<TagDto>() {
                    @Override
                    protected void updateItem(TagDto item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            setText(item.getName());
                        }
                    }
                };
            }
        });

        tagsListView.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener<TagDto>) c -> {
            updateSelectionCount();
        });

        loadTags();
    }

    private void updateSelectionCount() {
        List<TagDto> selectedItems = tagsListView.getSelectionModel().getSelectedItems();
        int count = selectedItems.size();
        selectionCountLabel.setText("Выбрано: " + count + "/5");

        if (count < 1) {
            saveButton.setDisable(true);
            selectionCountLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #F57C00;");
        } else if (count > 5) {
            saveButton.setDisable(true);
            selectionCountLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #D32F2F;");
        } else {
            saveButton.setDisable(false);
            selectionCountLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #388E3C;");
        }

        updateSelectedTagsDisplay(selectedItems);
    }

    private void updateSelectedTagsDisplay(List<TagDto> selectedItems) {
        selectedTagsPane.getChildren().clear();
        for (TagDto tag : selectedItems) {
            Label chip = new Label(tag.getName());
            chip.setStyle(
                "-fx-background-color: #E3F2FD; " +
                "-fx-background-radius: 15; " +
                "-fx-padding: 5 12 5 12; " +
                "-fx-text-fill: #1565C0; " +
                "-fx-font-size: 12px;"
            );
            selectedTagsPane.getChildren().add(chip);
        }
    }

    private void loadTags() {
        new Thread(() -> {
            try {
                List<TagDto> allTags = profileClientService.getAllActiveTags();
                List<TagDto> userTags = profileClientService.getUserTags();

                Set<Integer> userTagIds = userTags.stream()
                        .map(TagDto::getId)
                        .collect(Collectors.toSet());

                Platform.runLater(() -> {
                    tagsListView.getItems().addAll(allTags);

                    for (int i = 0; i < tagsListView.getItems().size(); i++) {
                        TagDto tag = tagsListView.getItems().get(i);
                        if (userTagIds.contains(tag.getId())) {
                            tagsListView.getSelectionModel().select(i);
                        }
                    }

                    updateSelectionCount();
                    logger.info("Loaded {} tags, user has {} selected", allTags.size(), userTags.size());
                });
            } catch (Exception e) {
                logger.error("Failed to load tags: {}", e.getMessage());
                Platform.runLater(() -> showError("Не удалось загрузить теги: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleSave() {
        List<TagDto> selectedItems = tagsListView.getSelectionModel().getSelectedItems();
        int count = selectedItems.size();

        if (count < 1 || count > 5) {
            showError("Выберите от 1 до 5 навыков");
            return;
        }

        List<Integer> tagIds = selectedItems.stream()
                .map(TagDto::getId)
                .collect(Collectors.toList());

        saveButton.setDisable(true);
        statusLabel.setVisible(false);

        new Thread(() -> {
            try {
                profileClientService.updateUserTags(tagIds);
                Platform.runLater(() -> {
                    showSuccess("Навыки сохранены");
                    logger.info("User tags updated: {} tags", tagIds.size());

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }

                    closeWindow();
                });
            } catch (Exception e) {
                logger.error("Failed to update tags: {}", e.getMessage());
                Platform.runLater(() -> {
                    saveButton.setDisable(false);
                    showError("Ошибка сохранения: " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void handleCancel() {
        logger.info("Cancel button clicked");
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
        stage.close();
    }

    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #D32F2F;");
        statusLabel.setVisible(true);
    }

    private void showSuccess(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #388E3C;");
        statusLabel.setVisible(true);
    }
}