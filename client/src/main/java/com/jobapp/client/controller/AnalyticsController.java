package com.jobapp.client.controller;

import com.jobapp.client.AppContext;
import com.jobapp.client.service.AnalyticsClientService;
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
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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

    private AnalyticsClientService analyticsService;
    private String userRole;

    @FXML
    private void initialize() {
        logger.info("AnalyticsController initialized");

        userRole = AppContext.getCurrentRole();
        analyticsService = new AnalyticsClientService(AppContext.getNetworkService());

        configureTabsByRole();
        setDefaultPeriods();
    }

    private void configureTabsByRole() {
        salariesTab.setDisable(false);
        tagsTab.setDisable(false);
        geoTab.setDisable(false);

        boolean isApplicant = "USER".equals(userRole);
        boolean isHr = "HR".equals(userRole);
        boolean isAdmin = "ADMIN".equals(userRole);

        conversionTab.setDisable(!isHr);
        systemStatsTab.setDisable(!isAdmin);

        if (isApplicant) {
            loadInitialApplicantData();
        }
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
    }

    private void loadSystemStats(int periodDays) {
        new Thread(() -> {
            try {
                SystemStatsDto stats = analyticsService.getSystemStats();
                List<ChartDataPointDto> timeline = analyticsService.getActivityTimeline(periodDays);
                Platform.runLater(() -> {
                    updateSystemPieChart(stats);
                    updateActivityChart(timeline);
                });
            } catch (Exception e) {
                logger.error("Failed to load system stats: {}", e.getMessage());
            }
        }).start();
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