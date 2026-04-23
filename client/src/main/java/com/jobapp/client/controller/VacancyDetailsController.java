package com.jobapp.client.controller;

import com.jobapp.client.AppContext;
import com.jobapp.client.NetworkService;
import com.jobapp.client.service.VacancyClientService;
import com.jobapp.common.dto.TagDto;
import com.jobapp.common.dto.VacancyDto;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class VacancyDetailsController {

    private static final Logger logger = LoggerFactory.getLogger(VacancyDetailsController.class);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @FXML
    private Label titleLabel;

    @FXML
    private Label companyLabel;

    @FXML
    private Label cityLabel;

    @FXML
    private Label employmentTypeLabel;

    @FXML
    private Label salaryLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private Label createdAtLabel;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private FlowPane tagsFlowPane;

    @FXML
    private Button applyButton;

    @FXML
    private Button favoriteButton;

    @FXML
    private Button complainButton;

    @FXML
    private Button closeButton;

    private VacancyClientService vacancyClientService;
    private VacancyDto vacancy;
    private Integer vacancyId;

    public VacancyDetailsController() {
    }

    public void setNetworkService(NetworkService networkService) {
        this.vacancyClientService = new VacancyClientService(networkService);
    }

    public void setVacancyId(Integer vacancyId) {
        this.vacancyId = vacancyId;
    }

    @FXML
    private void initialize() {
        logger.info("VacancyDetailsController initialized with vacancyId: {}", vacancyId);

        if (vacancyId != null) {
            loadVacancyDetails();
        }

        updateButtonVisibility();
    }

    private void loadVacancyDetails() {
        titleLabel.setText("Загрузка...");

        new Thread(() -> {
            try {
                VacancyDto vacancy = vacancyClientService.getDetails(vacancyId);
                this.vacancy = vacancy;

                Platform.runLater(() -> displayVacancy(vacancy));
            } catch (Exception e) {
                logger.error("Failed to load vacancy details: {}", e.getMessage());
                Platform.runLater(() -> {
                    titleLabel.setText("Ошибка загрузки");
                    showError("Не удалось загрузить данные вакансии: " + e.getMessage());
                });
            }
        }).start();
    }

    private void displayVacancy(VacancyDto vacancy) {
        titleLabel.setText(vacancy.getTitle());
        companyLabel.setText(vacancy.getCompanyName() != null ? vacancy.getCompanyName() : "Не указана");
        cityLabel.setText(vacancy.getCity() != null ? vacancy.getCity() : "Не указан");
        employmentTypeLabel.setText(vacancy.getEmploymentType() != null ? vacancy.getEmploymentType() : "Не указан");

        String salary = formatSalary(vacancy.getSalaryMin(), vacancy.getSalaryMax());
        salaryLabel.setText(salary);

        if (vacancy.getIsActive() != null && vacancy.getIsActive()) {
            statusLabel.setText("Активна");
            statusLabel.setStyle("-fx-text-fill: #388E3C;");
        } else {
            statusLabel.setText("Закрыта");
            statusLabel.setStyle("-fx-text-fill: #D32F2F;");
        }

        LocalDateTime createdAt = vacancy.getCreatedAt();
        createdAtLabel.setText(createdAt != null ? createdAt.format(DATE_FORMATTER) : "Не указана");

        descriptionArea.setText(vacancy.getDescription() != null ? vacancy.getDescription() : "Описание не указано");

        displayTags(vacancy.getTags());
    }

    private String formatSalary(Integer salaryMin, Integer salaryMax) {
        if (salaryMin == null && salaryMax == null) {
            return "Не указана";
        }
        StringBuilder sb = new StringBuilder();
        if (salaryMin != null) {
            sb.append("от ").append(formatNumber(salaryMin));
        }
        if (salaryMax != null) {
            if (sb.length() > 0) {
                sb.append(" - ");
            }
            sb.append("до ").append(formatNumber(salaryMax));
        }
        return sb.toString();
    }

    private String formatNumber(Integer number) {
        if (number == null) return "";
        return String.format("%,d", number).replace(",", " ") + " ₽";
    }

    private void displayTags(List<TagDto> tags) {
        tagsFlowPane.getChildren().clear();

        List<TagDto> tagsToDisplay = tags;
        if (tagsToDisplay != null && !tagsToDisplay.isEmpty()) {
            for (TagDto tag : tagsToDisplay) {
                Label tagLabel = new Label(tag.getName());
                tagLabel.setStyle("-fx-background-color: #E3F2FD; -fx-text-fill: #1976D2; -fx-padding: 4px 12px; " +
                           "-fx-background-radius: 16px; -fx-font-size: 12px;");
                tagsFlowPane.getChildren().add(tagLabel);
            }
        } else {
            Label noTagsLabel = new Label("Нет навыков");
            noTagsLabel.setStyle("-fx-text-fill: #757575; -fx-font-style: italic;");
            tagsFlowPane.getChildren().add(noTagsLabel);
        }
    }

    private void updateButtonVisibility() {
        String role = AppContext.getCurrentRole();
        boolean isLoggedIn = AppContext.getCurrentUserId() > 0;
        boolean isHr = "HR".equals(role);

        applyButton.setVisible(isLoggedIn && !isHr);
        favoriteButton.setVisible(isLoggedIn && !isHr);
        complainButton.setVisible(isLoggedIn);

        if (!isLoggedIn) {
            applyButton.setText("Войдите, чтобы откликнуться");
            applyButton.setDisable(true);
            favoriteButton.setText("Войдите, чтобы добавить");
            favoriteButton.setDisable(true);
        } else if (isHr) {
            applyButton.setText("Доступно соискателям");
            applyButton.setDisable(true);
            favoriteButton.setVisible(false);
        }
    }

    @FXML
    private void handleApply() {
        logger.info("Apply button clicked for vacancy: {}", vacancyId);
        showInfo("Функция отклика на вакансию будет реализована позже.");
    }

    @FXML
    private void handleFavorite() {
        logger.info("Favorite button clicked for vacancy: {}", vacancyId);
        showInfo("Функция добавления в избранное будет реализована позже.");
    }

    @FXML
    private void handleComplain() {
        logger.info("Complain button clicked for vacancy: {}", vacancyId);
        showInfo("Функция жалобы на вакансию будет реализована позже.");
    }

    @FXML
    private void handleClose() {
        logger.info("Close button clicked");
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    private void showError(String message) {
        logger.error(message);
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("Информация");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}