package com.jobapp.client.controller;

import com.jobapp.client.AppContext;
import com.jobapp.client.service.AnalyticsClientService;
import com.jobapp.client.service.VacancyClientService;
import com.jobapp.common.dto.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

public class AnalyticsController {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsController.class);

    @FXML
    private TabPane analyticsTabPane;

    @FXML
    private Tab salariesTab;

    @FXML
    private Tab tagsTab;

    @FXML
    private Tab geoTab;

    @FXML
    private Tab conversionTab;

    @FXML
    private Tab salaryCompareTab;

    @FXML
    private Tab systemStatsTab;

    @FXML
    private ComboBox<String> salaryPeriodCombo;

    @FXML
    private ComboBox<String> tagsPeriodCombo;

    @FXML
    private ComboBox<String> geoPeriodCombo;

    @FXML
    private ComboBox<String> systemPeriodCombo;

    @FXML
    private LineChart<String, Number> salaryChart;

    @FXML
    private BarChart<String, Number> tagsChart;

    @FXML
    private BarChart<String, Number> geoChart;

    @FXML
    private BarChart<String, Number> conversionChart;

    @FXML
    private PieChart usersPieChart;

    @FXML
    private BarChart<String, Number> activityChart;

    @FXML
    private BarChart<String, Number> adminTagsChart;

    @FXML
    private Label totalUsersLabel;

    @FXML
    private Label totalVacanciesLabel;

    @FXML
    private Label totalApplicationsLabel;

    @FXML
    private Label totalApplicantsLabel;

    @FXML
    private Label totalHrLabel;

    @FXML
    private ComboBox<VacancyDto> vacancySelectCombo;

    @FXML
    private BarChart<String, Number> salaryCompareChart;

    @FXML
    private ComboBox<TagDto> tagCompareCombo1;

    @FXML
    private ComboBox<TagDto> tagCompareCombo2;

    private AnalyticsClientService analyticsService;
    private VacancyClientService vacancyService;
    private String userRole;
    private ObservableList<VacancyDto> hrVacancies = FXCollections.observableArrayList();
    private ObservableList<TagDto> availableTags = FXCollections.observableArrayList();
    private int selectedVacancyId = -1;

    @FXML
    private void initialize() {
        logger.info("AnalyticsController initialized");

        userRole = AppContext.getCurrentRole();
        analyticsService = new AnalyticsClientService(AppContext.getNetworkService());
        vacancyService = new VacancyClientService(AppContext.getNetworkService());

        salaryPeriodCombo.getItems().addAll("Неделя", "Месяц", "Год");
        tagsPeriodCombo.getItems().addAll("Неделя", "Месяц", "Год");
        geoPeriodCombo.getItems().addAll("Неделя", "Месяц", "Год");
        systemPeriodCombo.getItems().addAll("Неделя", "Месяц", "Год");

        configureTabsByRole();
        setDefaultPeriods();
    }

    private void configureTabsByRole() {
        boolean isApplicant = "USER".equals(userRole);
        boolean isHr = "HR".equals(userRole);
        boolean isAdmin = "ADMIN".equals(userRole);

        conversionTab.setDisable(!isHr);
        salaryCompareTab.setDisable(!isHr);
        systemStatsTab.setDisable(!isAdmin);

        if (isApplicant) {
            loadInitialApplicantData();
        } else if (isHr) {
            loadInitialHrData();
        } else if (isAdmin) {
            loadInitialAdminData();
        }
    }

    private void loadInitialAdminData() {
        logger.info("Loading initial analytics data for admin");
        int systemPeriod = getPeriodDays(systemPeriodCombo.getValue());
        loadSystemStats(systemPeriod);

        int tagsPeriod = getPeriodDays(tagsPeriodCombo.getValue());
        loadAdminTopTags(tagsPeriod);

        int geoPeriod = getPeriodDays(geoPeriodCombo.getValue());
        loadGeoDataForAdmin(geoPeriod);
    }

    private void loadAdminTopTags(int periodDays) {
        new Thread(() -> {
            try {
                List<TagUsageDto> tags = analyticsService.getTopTags(periodDays, 10);
                Platform.runLater(() -> updateAdminTagsChart(tags));
            } catch (Exception e) {
                logger.error("Failed to load top tags for admin: {}", e.getMessage());
            }
        }).start();
    }

    private void updateAdminTagsChart(List<TagUsageDto> tags) {
        adminTagsChart.getData().clear();

        if (tags == null || tags.isEmpty()) {
            logger.info("No admin tags data available");
            return;
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Теги");

        for (TagUsageDto tag : tags) {
            series.getData().add(new XYChart.Data<>(tag.getName(), tag.getCount()));
        }

        adminTagsChart.getData().add(series);
    }

    private void loadGeoDataForAdmin(int periodDays) {
        new Thread(() -> {
            try {
                GeoDistributionDto geo = analyticsService.getGeoStats("VACANCY", periodDays);
                Platform.runLater(() -> updateGeoChart(geo));
            } catch (Exception e) {
                logger.error("Failed to load geo stats for admin: {}", e.getMessage());
            }
        }).start();
    }

    private void loadInitialHrData() {
        logger.info("Loading initial analytics data for HR");
        loadHrVacancies();
    }

    private void loadHrVacancies() {
        new Thread(() -> {
            try {
                List<VacancyDto> vacancies = vacancyService.getMyVacancies();
                Platform.runLater(() -> {
                    hrVacancies.setAll(vacancies);
                    vacancySelectCombo.setItems(hrVacancies);
                    if (!vacancies.isEmpty()) {
                        vacancySelectCombo.getSelectionModel().selectFirst();
                        selectedVacancyId = vacancies.get(0).getId();
                        loadConversion(selectedVacancyId);
                    }
                });
            } catch (Exception e) {
                logger.error("Failed to load HR vacancies: {}", e.getMessage());
            }
        }).start();
    }

    @FXML
    private void onVacancySelected() {
        VacancyDto selected = vacancySelectCombo.getValue();
        if (selected != null) {
            selectedVacancyId = selected.getId();
            loadConversion(selectedVacancyId);
        }
    }

    @FXML
    private void onRefreshConversion() {
        if (selectedVacancyId > 0) {
            loadConversion(selectedVacancyId);
        }
    }

    @FXML
    private void onCompareSalaries() {
        TagDto tag1 = tagCompareCombo1.getValue();
        TagDto tag2 = tagCompareCombo2.getValue();
        if (tag1 != null) {
            List<Integer> tagIds = new java.util.ArrayList<>();
            tagIds.add(tag1.getId());
            if (tag2 != null) {
                tagIds.add(tag2.getId());
            }
            loadSalaryComparison(tagIds);
        }
    }

    private void loadSalaryComparison(List<Integer> tagIds) {
        new Thread(() -> {
            try {
                List<ChartDataPointDto> comparison = analyticsService.getSalaryComparison(tagIds);
                Platform.runLater(() -> updateSalaryCompareChart(comparison));
            } catch (Exception e) {
                logger.error("Failed to load salary comparison: {}", e.getMessage());
            }
        }).start();
    }

    private void updateSalaryCompareChart(List<ChartDataPointDto> data) {
        salaryCompareChart.getData().clear();

        if (data == null || data.isEmpty()) {
            logger.info("No salary comparison data available");
            return;
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Средняя зарплата");

        for (ChartDataPointDto point : data) {
            series.getData().add(new XYChart.Data<>(point.getLabel(), point.getValue()));
        }

        salaryCompareChart.getData().add(series);
    }

    private void loadInitialApplicantData() {
        logger.info("Loading initial analytics data for applicant");

        int salaryPeriod = getPeriodDays(salaryPeriodCombo.getValue());
        loadSalaryData(salaryPeriod);

        int tagsPeriod = getPeriodDays(tagsPeriodCombo.getValue());
        loadTagsData(tagsPeriod);

        int geoPeriod = getPeriodDays(geoPeriodCombo.getValue());
        loadGeoData(geoPeriod);
    }

    private void setDefaultPeriods() {
        if (salaryPeriodCombo != null) {
            salaryPeriodCombo.getSelectionModel().select("Месяц");
        }
        if (tagsPeriodCombo != null) {
            tagsPeriodCombo.getSelectionModel().select("Месяц");
        }
        if (geoPeriodCombo != null) {
            geoPeriodCombo.getSelectionModel().select("Месяц");
        }
        if (systemPeriodCombo != null) {
            systemPeriodCombo.getSelectionModel().select("Месяц");
        }
    }

    @FXML
    private void onSalaryPeriodChanged() {
        int periodDays = getPeriodDays(salaryPeriodCombo.getValue());
        loadSalaryData(periodDays);
    }

    @FXML
    private void onTagsPeriodChanged() {
        int periodDays = getPeriodDays(tagsPeriodCombo.getValue());
        loadTagsData(periodDays);
    }

    @FXML
    private void onGeoPeriodChanged() {
        int periodDays = getPeriodDays(geoPeriodCombo.getValue());
        loadGeoData(periodDays);
    }

    @FXML
    private void onSystemPeriodChanged() {
        int periodDays = getPeriodDays(systemPeriodCombo.getValue());
        loadSystemStats(periodDays);
    }

    private int getPeriodDays(String periodName) {
        if (periodName == null) return 30;
        return switch (periodName) {
            case "Неделя" -> 7;
            case "Месяц" -> 30;
            case "Год" -> 365;
            default -> 30;
        };
    }

    private void loadSalaryData(int periodDays) {
        new Thread(() -> {
            try {
                SalaryStatsDto stats = analyticsService.getSalaryStats(periodDays);
                Platform.runLater(() -> updateSalaryChart(stats));
            } catch (Exception e) {
                logger.error("Failed to load salary stats: {}", e.getMessage());
            }
        }).start();
    }

    private void updateSalaryChart(SalaryStatsDto stats) {
        salaryChart.getData().clear();

        if (stats == null || stats.getDataPoints() == null || stats.getDataPoints().isEmpty()) {
            logger.info("No salary data available");
            return;
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Средняя зарплата");

        for (ChartDataPointDto point : stats.getDataPoints()) {
            series.getData().add(new XYChart.Data<>(point.getLabel(), point.getValue()));
        }

        salaryChart.getData().add(series);
    }

    private void loadTagsData(int periodDays) {
        new Thread(() -> {
            try {
                List<TagUsageDto> tags = analyticsService.getTopTags(periodDays, 10);
                Platform.runLater(() -> updateTagsChart(tags));
            } catch (Exception e) {
                logger.error("Failed to load top tags: {}", e.getMessage());
            }
        }).start();
    }

    private void updateTagsChart(List<TagUsageDto> tags) {
        tagsChart.getData().clear();

        if (tags == null || tags.isEmpty()) {
            logger.info("No tags data available");
            return;
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Теги");

        for (TagUsageDto tag : tags) {
            series.getData().add(new XYChart.Data<>(tag.getName(), tag.getCount()));
        }

        tagsChart.getData().add(series);
    }

    private void loadGeoData(int periodDays) {
        new Thread(() -> {
            try {
                String type = "VACANCY";
                if ("HR".equals(userRole)) {
                    type = "APPLICANT";
                } else if (!"ADMIN".equals(userRole)) {
                    type = "VACANCY";
                }

                GeoDistributionDto geo = analyticsService.getGeoStats(type, periodDays);
                Platform.runLater(() -> updateGeoChart(geo));
            } catch (Exception e) {
                logger.error("Failed to load geo stats: {}", e.getMessage());
            }
        }).start();
    }

    private void updateGeoChart(GeoDistributionDto geo) {
        geoChart.getData().clear();

        if (geo == null || geo.getDistribution() == null || geo.getDistribution().isEmpty()) {
            logger.info("No geo data available");
            return;
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Вакансии");

        for (CityCountDto city : geo.getDistribution()) {
            series.getData().add(new XYChart.Data<>(city.getCity(), city.getCount()));
        }

        geoChart.getData().add(series);
    }

    private void loadConversion(int vacancyId) {
        new Thread(() -> {
            try {
                ConversionStatsDto conversion = analyticsService.getConversion(vacancyId);
                Platform.runLater(() -> updateConversionChart(conversion));
            } catch (Exception e) {
                logger.error("Failed to load conversion stats: {}", e.getMessage());
            }
        }).start();
    }

    private void updateConversionChart(ConversionStatsDto conversion) {
        conversionChart.getData().clear();

        if (conversion == null) {
            logger.info("No conversion data available");
            return;
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Конверсия");

        series.getData().add(new XYChart.Data<>("Просмотры", conversion.getViews()));
        series.getData().add(new XYChart.Data<>("Отклики", conversion.getResponses()));
        series.getData().add(new XYChart.Data<>("Приглашения", conversion.getInvites()));

        conversionChart.getData().add(series);

        Double rate = conversion.getConversionRate();
        if (rate != null) {
            logger.info("Conversion rate: {}%", String.format("%.2f", rate * 100));
        }
    }

    private void loadSystemStats(int periodDays) {
        new Thread(() -> {
            try {
                SystemStatsDto stats = analyticsService.getSystemStats();
                List<ChartDataPointDto> timeline = analyticsService.getActivityTimeline(periodDays);
                List<TagUsageDto> topTags = analyticsService.getTopTags(periodDays, 10);
                Platform.runLater(() -> {
                    updateStatsCards(stats);
                    updateSystemPieChart(stats);
                    updateActivityChart(timeline);
                    updateAdminTagsChart(topTags);
                });
            } catch (Exception e) {
                logger.error("Failed to load system stats: {}", e.getMessage());
            }
        }).start();
    }

    private void updateStatsCards(SystemStatsDto stats) {
        if (stats == null) return;

        if (totalUsersLabel != null) {
            totalUsersLabel.setText(String.valueOf(stats.getTotalUsers() != null ? stats.getTotalUsers() : 0));
        }
        if (totalVacanciesLabel != null) {
            totalVacanciesLabel.setText(String.valueOf(stats.getTotalVacancies() != null ? stats.getTotalVacancies() : 0));
        }
        if (totalApplicationsLabel != null) {
            totalApplicationsLabel.setText(String.valueOf(stats.getTotalApplications() != null ? stats.getTotalApplications() : 0));
        }
        if (totalApplicantsLabel != null) {
            totalApplicantsLabel.setText(String.valueOf(stats.getTotalApplicants() != null ? stats.getTotalApplicants() : 0));
        }
        if (totalHrLabel != null) {
            totalHrLabel.setText(String.valueOf(stats.getTotalHr() != null ? stats.getTotalHr() : 0));
        }
    }

    private void updateSystemPieChart(SystemStatsDto stats) {
        usersPieChart.getData().clear();

        if (stats == null) {
            logger.info("No system stats available");
            return;
        }

        ObservableList<PieChart.Data> data = FXCollections.observableArrayList(
            new PieChart.Data("Соискатели", stats.getTotalUsers()),
            new PieChart.Data("HR", stats.getTotalHr()),
            new PieChart.Data("Админы", stats.getTotalAdmins())
        );

        usersPieChart.setData(data);
    }

    private void updateActivityChart(List<ChartDataPointDto> timeline) {
        activityChart.getData().clear();

        if (timeline == null || timeline.isEmpty()) {
            logger.info("No activity data available");
            return;
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Активность");

        for (ChartDataPointDto point : timeline) {
            series.getData().add(new XYChart.Data<>(point.getLabel(), point.getValue()));
        }

        activityChart.getData().add(series);
    }

    public void loadConversionForVacancy(int vacancyId) {
        loadConversion(vacancyId);
    }
}