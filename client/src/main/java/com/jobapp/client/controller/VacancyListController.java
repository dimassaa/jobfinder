package com.jobapp.client.controller;

import com.jobapp.client.service.VacancyClientService;
import com.jobapp.common.dto.VacancyDto;
import com.jobapp.common.dto.VacancyFilterDto;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class VacancyListController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(VacancyListController.class);

    @FXML private StackPane root;
    @FXML private TextField cityField;
    @FXML private TextField keywordField;
    @FXML private ComboBox<String> employmentTypeCombo;
    @FXML private TextField salaryFromField;
    @FXML private TextField salaryToField;
    @FXML private Button searchButton;
    @FXML private Button last24HoursButton;
    @FXML private Label statusLabel;
    @FXML private TableView<VacancyDto> vacanciesTable;
    @FXML private TableColumn<VacancyDto, String> titleColumn;
    @FXML private TableColumn<VacancyDto, String> companyColumn;
    @FXML private TableColumn<VacancyDto, String> cityColumn;
    @FXML private TableColumn<VacancyDto, String> salaryColumn;
    @FXML private TableColumn<VacancyDto, String> employmentTypeColumn;
    @FXML private TableColumn<VacancyDto, String> createdAtColumn;
    @FXML private Button refreshButton;

    private VacancyClientService vacancyClientService;
    private ObservableList<VacancyDto> vacancies = FXCollections.observableArrayList();
    private ProgressIndicator loadingIndicator;

    private static final List<String> EMPLOYMENT_TYPES = Arrays.asList(
            "Полная занятость",
            "Частичная занятость",
            "Удалённая работа",
            "Стажировка"
    );

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        com.jobapp.client.NetworkService networkService = new com.jobapp.client.NetworkService();
        com.jobapp.client.AppContext.setNetworkService(networkService);
        vacancyClientService = new VacancyClientService(networkService);

        setupTableColumns();
        setupEmploymentTypeCombo();
        setupLoadingIndicator();
        setupStatusLabel();

        vacanciesTable.setItems(vacancies);

        Platform.runLater(() -> loadVacancies(null));
    }

    private void setupTableColumns() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        companyColumn.setCellValueFactory(new PropertyValueFactory<>("companyName"));
        cityColumn.setCellValueFactory(new PropertyValueFactory<>("city"));
        employmentTypeColumn.setCellValueFactory(new PropertyValueFactory<>("employmentType"));
        createdAtColumn.setCellValueFactory(cellData -> {
            LocalDateTime dateTime = cellData.getValue().getCreatedAt();
            return new javafx.beans.property.SimpleStringProperty(
                    dateTime != null ? dateTime.format(DATE_FORMATTER) : ""
            );
        });
        salaryColumn.setCellValueFactory(cellData -> {
            VacancyDto vacancy = cellData.getValue();
            String salary = formatSalary(vacancy.getSalaryMin(), vacancy.getSalaryMax());
            return new javafx.beans.property.SimpleStringProperty(salary);
        });
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

    private void setupEmploymentTypeCombo() {
        ObservableList<String> types = FXCollections.observableArrayList(EMPLOYMENT_TYPES);
        employmentTypeCombo.setItems(types);
    }

    private void setupLoadingIndicator() {
        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setMaxSize(24, 24);
        loadingIndicator.setStyle("-fx-progress-color: #1976D2;");

        StackPane.setAlignment(loadingIndicator, javafx.geometry.Pos.BOTTOM_RIGHT);
        StackPane.setMargin(loadingIndicator, new javafx.geometry.Insets(0, 16, 16, 0));
        root.getChildren().add(loadingIndicator);
    }

    private void setupStatusLabel() {
        statusLabel.setWrapText(true);
    }

    @FXML
    private void handleSearch() {
        VacancyFilterDto filter = buildFilterFromFields();
        loadVacancies(filter);
    }

    @FXML
    private void handleLast24Hours() {
        VacancyFilterDto filter = new VacancyFilterDto();
        filter.setCreatedAfter(LocalDateTime.now().minusHours(24));

        clearFields();
        loadVacancies(filter);
    }

    @FXML
    private void handleTableClick(javafx.scene.input.MouseEvent event) {
        if (event.getClickCount() == 2) {
            VacancyDto selectedVacancy = vacanciesTable.getSelectionModel().getSelectedItem();
            if (selectedVacancy != null) {
                openVacancyDetails(selectedVacancy);
            }
        }
    }

    private void loadVacancies(VacancyFilterDto filter) {
        showLoading(true);
        hideStatus();

        new Thread(() -> {
            try {
                List<VacancyDto> results;
                if (filter == null) {
                    results = vacancyClientService.search(new VacancyFilterDto());
                } else {
                    results = vacancyClientService.search(filter);
                }

                Platform.runLater(() -> {
                    vacancies.clear();
                    vacancies.addAll(results);
                    showStatus("Найдено вакансий: " + results.size(), false);
                });
            } catch (Exception e) {
                logger.error("Error loading vacancies", e);
                Platform.runLater(() -> showStatus("Ошибка загрузки вакансий: " + e.getMessage(), true));
            } finally {
                Platform.runLater(() -> showLoading(false));
            }
        }).start();
    }

    private VacancyFilterDto buildFilterFromFields() {
        VacancyFilterDto filter = new VacancyFilterDto();

        String city = cityField.getText();
        if (city != null && !city.isBlank()) {
            filter.setCity(city.trim());
        }

        String keyword = keywordField.getText();
        if (keyword != null && !keyword.isBlank()) {
            filter.setKeyword(keyword.trim());
        }

        String employmentType = employmentTypeCombo.getValue();
        if (employmentType != null && !employmentType.isBlank()) {
            filter.setEmploymentType(employmentType);
        }

        String salaryFromText = salaryFromField.getText();
        if (salaryFromText != null && !salaryFromText.isBlank()) {
            try {
                filter.setSalaryFrom(parseSalary(salaryFromText));
            } catch (NumberFormatException e) {
                showAlert("Ошибка", "Неверный формат зарплаты 'от'");
            }
        }

        String salaryToText = salaryToField.getText();
        if (salaryToText != null && !salaryToText.isBlank()) {
            try {
                filter.setSalaryTo(parseSalary(salaryToText));
            } catch (NumberFormatException e) {
                showAlert("Ошибка", "Неверный формат зарплаты 'до'");
            }
        }

        return filter;
    }

    private Integer parseSalary(String text) {
        String cleaned = text.replaceAll("[^0-9]", "");
        return cleaned.isEmpty() ? null : Integer.parseInt(cleaned);
    }

    private void clearFields() {
        cityField.clear();
        keywordField.clear();
        employmentTypeCombo.getSelectionModel().clearSelection();
        salaryFromField.clear();
        salaryToField.clear();
    }

    private void showLoading(boolean show) {
        loadingIndicator.setVisible(show);
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setVisible(true);
        statusLabel.getStyleClass().clear();
        if (isError) {
            statusLabel.getStyleClass().add("label-error");
        } else {
            statusLabel.getStyleClass().add("label-secondary");
        }
    }

    private void hideStatus() {
        statusLabel.setVisible(false);
    }

    private void openVacancyDetails(VacancyDto vacancy) {
        logger.info("Opening vacancy details for: {} (id={})", vacancy.getTitle(), vacancy.getId());

        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/vacancy_details.fxml"));

            javafx.scene.Parent root = loader.load();

            VacancyDetailsController controller = loader.getController();
            controller.setVacancyId(vacancy.getId());

            com.jobapp.client.NetworkService networkService = com.jobapp.client.AppContext.getNetworkService();
            if (networkService != null) {
                controller.setNetworkService(networkService);
            }

            javafx.stage.Stage detailsStage = new javafx.stage.Stage();
            detailsStage.setTitle("Вакансия: " + vacancy.getTitle());
            detailsStage.setScene(new javafx.scene.Scene(root, 600, 700));
            detailsStage.initOwner(vacanciesTable.getScene().getWindow());
            detailsStage.setResizable(true);
            detailsStage.show();

        } catch (Exception e) {
            logger.error("Failed to open vacancy details: {}", e.getMessage());
            showAlert("Ошибка", "Не удалось открыть детали вакансии: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}