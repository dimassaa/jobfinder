package com.jobapp.client.controller;

import com.jobapp.client.NetworkService;
import com.jobapp.client.service.ProfileClientService;
import com.jobapp.client.service.VacancyClientService;
import com.jobapp.common.dto.TagDto;
import com.jobapp.common.dto.VacancyCreateRequest;
import com.jobapp.common.dto.VacancyDto;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class VacancyEditController {

    private static final Logger logger = LoggerFactory.getLogger(VacancyEditController.class);

    private static final List<String> EMPLOYMENT_TYPES = Arrays.asList(
            "FULL_TIME",
            "PART_TIME",
            "REMOTE",
            "CONTRACT"
    );

    private static final List<String> EMPLOYMENT_TYPES_RU = Arrays.asList(
            "Полная занятость",
            "Частичная занятость",
            "Удалённая работа",
            "Стажировка"
    );

    @FXML
    private TextField titleField;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private TextField cityField;

    @FXML
    private ComboBox<String> employmentTypeCombo;

    @FXML
    private TextField salaryMinField;

    @FXML
    private TextField salaryMaxField;

    @FXML
    private ListView<TagDto> tagsListView;

    @FXML
    private Label statusLabel;

    @FXML
    private Button saveButton;

    @FXML
    private Button cancelButton;

    private VacancyClientService vacancyClientService;
    private ProfileClientService profileClientService;
    private Runnable onCloseCallback;

    private VacancyDto editingVacancy;
    private ObservableList<TagDto> availableTags = FXCollections.observableArrayList();

    public VacancyEditController() {
    }

    public void setNetworkService(NetworkService networkService) {
        this.vacancyClientService = new VacancyClientService(networkService);
        this.profileClientService = new ProfileClientService(networkService);
    }

    public void setOnCloseCallback(Runnable callback) {
        this.onCloseCallback = callback;
    }

    public void setEditingVacancy(VacancyDto vacancy) {
        this.editingVacancy = vacancy;
    }

    @FXML
    private void initialize() {
        logger.info("VacancyEditController initialized");
        setupEmploymentTypeCombo();
        saveButton.setDisable(true);

        loadTags();
    }

    private void setupEmploymentTypeCombo() {
        ObservableList<String> types = FXCollections.observableArrayList(EMPLOYMENT_TYPES_RU);
        employmentTypeCombo.setItems(types);
    }

    private void loadTags() {
        new Thread(() -> {
            try {
                List<TagDto> allTags = profileClientService.getAllActiveTags();

                Platform.runLater(() -> {
                    availableTags.addAll(allTags);
                    tagsListView.setItems(availableTags);
                    tagsListView.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
                    saveButton.setDisable(false);

                    if (editingVacancy != null) {
                        populateFieldsForEditing();
                    }

                    logger.info("Loaded {} available tags", allTags.size());
                });
            } catch (Exception e) {
                logger.error("Failed to load tags: {}", e.getMessage());
                Platform.runLater(() -> showError("Не удалось загрузить теги: " + e.getMessage()));
            }
        }).start();
    }

    private void populateFieldsForEditing() {
        titleField.setText(editingVacancy.getTitle());
        descriptionArea.setText(editingVacancy.getDescription());
        cityField.setText(editingVacancy.getCity());

        String empType = editingVacancy.getEmploymentType();
        int empIndex = EMPLOYMENT_TYPES.indexOf(empType);
        employmentTypeCombo.setValue(empIndex >= 0 ? EMPLOYMENT_TYPES_RU.get(empIndex) : empType);

        if (editingVacancy.getSalaryMin() != null) {
            salaryMinField.setText(String.valueOf(editingVacancy.getSalaryMin()));
        }
        if (editingVacancy.getSalaryMax() != null) {
            salaryMaxField.setText(String.valueOf(editingVacancy.getSalaryMax()));
        }

        if (editingVacancy.getTags() != null) {
            List<Integer> vacancyTagIds = editingVacancy.getTags().stream()
                    .map(TagDto::getId)
                    .collect(Collectors.toList());

            for (int i = 0; i < availableTags.size(); i++) {
                TagDto tag = availableTags.get(i);
                if (vacancyTagIds.contains(tag.getId())) {
                    tagsListView.getSelectionModel().select(i);
                }
            }
        }
    }

    @FXML
    private void handleSave() {
        if (!validateFields()) {
            return;
        }

        saveButton.setDisable(true);
        statusLabel.setVisible(false);

        new Thread(() -> {
            try {
                String title = titleField.getText().trim();
                String description = descriptionArea.getText().trim();
                String city = cityField.getText().trim();
                String employmentType = employmentTypeCombo.getValue();
                int empIndex = EMPLOYMENT_TYPES_RU.indexOf(employmentType);
                String empTypeEnum = (empIndex >= 0) ? EMPLOYMENT_TYPES.get(empIndex) : employmentType;

                Integer salaryMin = parseSalary(salaryMinField.getText());
                Integer salaryMax = parseSalary(salaryMaxField.getText());

                List<TagDto> selectedTags = tagsListView.getSelectionModel().getSelectedItems();
                List<Integer> tagIds = selectedTags.stream()
                        .map(TagDto::getId)
                        .collect(Collectors.toList());

                VacancyCreateRequest request = new VacancyCreateRequest();
                request.setTitle(title);
                request.setDescription(description);
                request.setCity(city);
                request.setEmploymentType(empTypeEnum);
                request.setSalaryMin(salaryMin);
                request.setSalaryMax(salaryMax);
                request.setTagIds(tagIds);

                if (editingVacancy != null) {
                    vacancyClientService.update(editingVacancy.getId(), request);
                    logger.info("Vacancy {} updated successfully", editingVacancy.getId());
                } else {
                    vacancyClientService.create(request);
                    logger.info("Vacancy created successfully");
                }

                Platform.runLater(() -> {
                    showSuccess(editingVacancy != null ? "Вакансия обновлена" : "Вакансия создана");

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }

                    closeWindow();
                });
            } catch (Exception e) {
                logger.error("Failed to save vacancy: {}", e.getMessage());
                Platform.runLater(() -> {
                    saveButton.setDisable(false);
                    showError("Ошибка сохранения: " + e.getMessage());
                });
            }
        }).start();
    }

    private boolean validateFields() {
        String title = titleField.getText();
        if (title == null || title.trim().isEmpty()) {
            showError("Укажите заголовок вакансии");
            titleField.requestFocus();
            return false;
        }

        String description = descriptionArea.getText();
        if (description == null || description.trim().isEmpty()) {
            showError("Укажите описание вакансии");
            descriptionArea.requestFocus();
            return false;
        }

        String city = cityField.getText();
        if (city == null || city.trim().isEmpty()) {
            showError("Укажите город");
            cityField.requestFocus();
            return false;
        }

        String employmentType = employmentTypeCombo.getValue();
        if (employmentType == null || employmentType.isEmpty()) {
            showError("Выберите тип занятости");
            employmentTypeCombo.requestFocus();
            return false;
        }

        String salaryMinText = salaryMinField.getText();
        String salaryMaxText = salaryMaxField.getText();

        if (salaryMinText != null && !salaryMinText.trim().isEmpty()) {
            try {
                Integer.parseInt(salaryMinText.replaceAll("[^0-9]", ""));
            } catch (NumberFormatException e) {
                showError("Неверный формат зарплаты 'от'");
                salaryMinField.requestFocus();
                return false;
            }
        }

        if (salaryMaxText != null && !salaryMaxText.trim().isEmpty()) {
            try {
                Integer.parseInt(salaryMaxText.replaceAll("[^0-9]", ""));
            } catch (NumberFormatException e) {
                showError("Неверный формат зарплаты 'до'");
                salaryMaxField.requestFocus();
                return false;
            }
        }

        String cleanedMin = salaryMinText != null ? salaryMinText.replaceAll("[^0-9]", "") : "";
        String cleanedMax = salaryMaxText != null ? salaryMaxText.replaceAll("[^0-9]", "") : "";

        if (!cleanedMin.isEmpty() && !cleanedMax.isEmpty()) {
            int min = Integer.parseInt(cleanedMin);
            int max = Integer.parseInt(cleanedMax);
            if (min > max) {
                showError("Минимальная зарплата не может быть больше максимальной");
                salaryMinField.requestFocus();
                return false;
            }
        }

        return true;
    }

    private Integer parseSalary(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        String cleaned = text.replaceAll("[^0-9]", "");
        return cleaned.isEmpty() ? null : Integer.parseInt(cleaned);
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