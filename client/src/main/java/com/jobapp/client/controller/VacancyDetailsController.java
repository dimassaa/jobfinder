package com.jobapp.client.controller;

import com.jobapp.client.AppContext;
import com.jobapp.client.NetworkService;
import com.jobapp.client.service.ApplicationClientService;
import com.jobapp.client.service.ComplaintClientService;
import com.jobapp.client.service.FavoriteClientService;
import com.jobapp.client.service.VacancyClientService;
import com.jobapp.client.service.ViewHistoryClientService;
import com.jobapp.common.dto.ApplicationDto;
import com.jobapp.common.dto.TagDto;
import com.jobapp.common.dto.VacancyDto;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
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
    private ApplicationClientService applicationClientService;
    private FavoriteClientService favoriteClientService;
    private ComplaintClientService complaintClientService;
    private ViewHistoryClientService viewHistoryClientService;
    private VacancyDto vacancy;
    private Integer vacancyId;
    private boolean hasApplied = false;
    private boolean isFavorite = false;

    public VacancyDetailsController() {
    }

    public void setNetworkService(NetworkService networkService) {
        this.vacancyClientService = new VacancyClientService(networkService);
        this.applicationClientService = new ApplicationClientService(networkService);
        this.favoriteClientService = new FavoriteClientService(networkService);
        this.complaintClientService = new ComplaintClientService(networkService);
        this.viewHistoryClientService = new ViewHistoryClientService(networkService);
    }

    public void setVacancyId(Integer vacancyId) {
        this.vacancyId = vacancyId;
    }

    @FXML
    private void initialize() {
        logger.info("VacancyDetailsController initialized with vacancyId: {}", vacancyId);

        NetworkService networkService = AppContext.getNetworkService();
        if (networkService != null) {
            this.vacancyClientService = new VacancyClientService(networkService);
            this.applicationClientService = new ApplicationClientService(networkService);
            this.favoriteClientService = new FavoriteClientService(networkService);
            this.complaintClientService = new ComplaintClientService(networkService);
            this.viewHistoryClientService = new ViewHistoryClientService(networkService);
        }

        if (vacancyId != null && vacancyClientService != null) {
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

                recordView();

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

    private void recordView() {
        try {
            if (viewHistoryClientService != null && vacancyId != null) {
                viewHistoryClientService.addView(vacancyId);
                logger.debug("View recorded for vacancy {}", vacancyId);
            }
        } catch (Exception e) {
            logger.warn("Failed to record view: {}", e.getMessage());
        }
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
        checkIfAlreadyApplied();
        checkIfFavorite();
        updateButtonStates();
    }

    private void updateButtonStates() {
        if (hasApplied) {
            applyButton.setText("Отклик отправлен");
            applyButton.setDisable(true);
        }
        if (isFavorite) {
            favoriteButton.setText("В избранном");
        }
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

    private void checkIfAlreadyApplied() {
        if (applicationClientService == null || vacancyId == null) return;
        try {
            List<ApplicationDto> applications = applicationClientService.getMyApplications();
            for (ApplicationDto app : applications) {
                if (app.getVacancyId() != null && app.getVacancyId().equals(vacancyId)) {
                    hasApplied = true;
                    break;
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to check applications: {}", e.getMessage());
        }
    }

    private void checkIfFavorite() {
        if (favoriteClientService == null || vacancyId == null) return;
        try {
            isFavorite = favoriteClientService.isFavorite(vacancyId);
        } catch (Exception e) {
            logger.warn("Failed to check favorite status: {}", e.getMessage());
        }
    }

    @FXML
    private void handleApply() {
        logger.info("Apply button clicked for vacancy: {}", vacancyId);

        if (hasApplied) {
            showInfo("Вы уже откликались на эту вакансию");
            return;
        }

        applyButton.setDisable(true);
        applyButton.setText("Отправка...");

        new Thread(() -> {
            try {
                ApplicationDto application = applicationClientService.apply(vacancyId);
                Platform.runLater(() -> {
                    hasApplied = true;
                    applyButton.setText("Отклик отправлен");
                    showInfo("Ваш отклик успешно отправлен!");
                });
            } catch (Exception e) {
                logger.error("Failed to apply: {}", e.getMessage());
                Platform.runLater(() -> {
                    applyButton.setDisable(false);
                    applyButton.setText("Откликнуться");
                    showError("Не удалось отправить отклик: " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void handleFavorite() {
        logger.info("Favorite button clicked for vacancy: {}", vacancyId);

        new Thread(() -> {
            try {
                if (isFavorite) {
                    favoriteClientService.remove(vacancyId);
                    Platform.runLater(() -> {
                        isFavorite = false;
                        favoriteButton.setText("В избранное");
                        showInfo("Вакансия удалена из избранного");
                    });
                } else {
                    favoriteClientService.add(vacancyId);
                    Platform.runLater(() -> {
                        isFavorite = true;
                        favoriteButton.setText("В избранном");
                        showInfo("Вакансия добавлена в избранное");
                    });
                }
            } catch (Exception e) {
                logger.error("Failed to toggle favorite: {}", e.getMessage());
                Platform.runLater(() -> showError("Ошибка: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleComplain() {
        logger.info("Complain button clicked for vacancy: {}", vacancyId);

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Пожаловаться на вакансию");
        dialog.setHeaderText("Укажите причину жалобы");
        dialog.setContentText("Причина:");

        dialog.showAndWait().ifPresent(reason -> {
            if (reason == null || reason.trim().isEmpty()) {
                showError("Необходимо указать причину жалобы");
                return;
            }

            new Thread(() -> {
                try {
                    complaintClientService.create("VACANCY", vacancyId, reason.trim());
                    Platform.runLater(() -> showInfo("Жалоба отправлена модератору"));
                } catch (Exception e) {
                    logger.error("Failed to submit complaint: {}", e.getMessage());
                    Platform.runLater(() -> showError("Не удалось отправить жалобу: " + e.getMessage()));
                }
            }).start();
        });
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