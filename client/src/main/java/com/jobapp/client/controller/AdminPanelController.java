package com.jobapp.client.controller;

import com.jobapp.client.AppContext;
import com.jobapp.client.NetworkService;
import com.jobapp.client.service.AdminClientService;
import com.jobapp.client.service.ToastService;
import com.jobapp.common.dto.AdminUserDto;
import com.jobapp.common.dto.ChartDataPointDto;
import com.jobapp.common.dto.ComplaintAdminDto;
import com.jobapp.common.dto.NotificationDto;
import com.jobapp.common.dto.TagDto;
import com.jobapp.common.dto.TagUsageDto;
import com.jobapp.common.dto.VacancyDto;
import com.jobapp.client.service.AnalyticsClientService;
import com.jobapp.client.service.NotificationClientService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.shape.Circle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AdminPanelController {

    private static final Logger logger = LoggerFactory.getLogger(AdminPanelController.class);

    @FXML
    private TabPane adminTabPane;

    @FXML
    private ComboBox<String> roleFilterCombo;
    @FXML
    private ComboBox<String> blockFilterCombo;
    @FXML
    private TableView<AdminUserDto> usersTable;

    @FXML
    private ComboBox<String> vacancyFilterCombo;
    @FXML
    private TableView<VacancyDto> vacanciesTable;

    @FXML
    private TableView<ComplaintAdminDto> complaintsTable;

    @FXML
    private TextField newTagField;
    @FXML
    private TableView<TagDto> tagsTable;

    @FXML
    private TextField newAdminEmail;
    @FXML
    private TextField newAdminPassword;
    @FXML
    private TextField newAdminName;

    @FXML
    private Label statusLabel;

    @FXML
    private ComboBox<String> analyticsPeriodCombo;

    @FXML
    private Label adminTotalUsersLabel;

    @FXML
    private Label adminTotalVacanciesLabel;

    @FXML
    private Label adminTotalApplicationsLabel;

    @FXML
    private Label adminTotalApplicantsLabel;

    @FXML
    private Label adminTotalHrLabel;

    @FXML
    private PieChart adminUsersPieChart;

    @FXML
    private BarChart<String, Number> adminTagsBarChart;

    @FXML
    private BarChart<String, Number> adminActivityChart;

    @FXML
    private TableView<NotificationDto> adminNotificationsTable;

    @FXML
    private Circle adminBadgeCircle;

    @FXML
    private Label adminBadgeLabel;

    private AdminClientService adminClientService;
    private AnalyticsClientService analyticsClientService;
    private NotificationClientService notificationClientService;
    private ObservableList<AdminUserDto> usersData = FXCollections.observableArrayList();
    private ObservableList<VacancyDto> vacanciesData = FXCollections.observableArrayList();
    private ObservableList<ComplaintAdminDto> complaintsData = FXCollections.observableArrayList();
    private ObservableList<TagDto> tagsData = FXCollections.observableArrayList();
    private ObservableList<NotificationDto> notificationsData = FXCollections.observableArrayList();
    private int unreadCount = 0;

    @FXML
    private void initialize() {
        logger.info("AdminPanelController initialized");

        NetworkService networkService = AppContext.getNetworkService();
        if (networkService != null && networkService.isConnected()) {
            adminClientService = new AdminClientService(networkService);
            notificationClientService = new NotificationClientService(networkService);
        } else {
            logger.error("NetworkService not available or disconnected");
            return;
        }

        setupUsersTable();
        setupVacanciesTable();
        setupComplaintsTable();
        setupTagsTable();
        setupNotificationsTable();

        roleFilterCombo.getItems().addAll("Все", "USER", "HR", "ADMIN");
        roleFilterCombo.getSelectionModel().selectFirst();
        blockFilterCombo.getItems().addAll("Все", "Активные", "Заблокированные");
        blockFilterCombo.getSelectionModel().selectFirst();
        vacancyFilterCombo.getItems().addAll("Активные", "Все");
        vacancyFilterCombo.getSelectionModel().selectFirst();
        analyticsPeriodCombo.getItems().addAll("Неделя", "Месяц", "Год");
        analyticsPeriodCombo.getSelectionModel().select("Месяц");

        loadNotifications();
    }

    private void setupUsersTable() {
        usersTable.setItems(usersData);
        usersTable.getColumns().clear();
        TableColumn<AdminUserDto, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);

        TableColumn<AdminUserDto, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        emailCol.setPrefWidth(150);

        TableColumn<AdminUserDto, String> nameCol = new TableColumn<>("Имя");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(100);

        TableColumn<AdminUserDto, String> surnameCol = new TableColumn<>("Фамилия");
        surnameCol.setCellValueFactory(new PropertyValueFactory<>("surname"));
        surnameCol.setPrefWidth(100);

        TableColumn<AdminUserDto, String> roleCol = new TableColumn<>("Роль");
        roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));
        roleCol.setPrefWidth(80);

        TableColumn<AdminUserDto, String> cityCol = new TableColumn<>("Город");
        cityCol.setCellValueFactory(new PropertyValueFactory<>("city"));
        cityCol.setPrefWidth(100);

        TableColumn<AdminUserDto, String> blockedCol = new TableColumn<>("Заблокирован");
        blockedCol.setCellValueFactory(cellData -> {
            Boolean isBlocked = cellData.getValue().getIsBlocked();
            return new javafx.beans.property.SimpleStringProperty(isBlocked != null && isBlocked ? "Да" : "Нет");
        });
        blockedCol.setPrefWidth(100);

        usersTable.getColumns().addAll(idCol, emailCol, nameCol, surnameCol, roleCol, cityCol, blockedCol);
    }

    private void setupVacanciesTable() {
        vacanciesTable.setItems(vacanciesData);
        vacanciesTable.getColumns().clear();
        TableColumn<VacancyDto, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);

        TableColumn<VacancyDto, String> titleCol = new TableColumn<>("Название");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleCol.setPrefWidth(200);

        TableColumn<VacancyDto, String> companyCol = new TableColumn<>("Компания");
        companyCol.setCellValueFactory(new PropertyValueFactory<>("companyName"));
        companyCol.setPrefWidth(150);

        TableColumn<VacancyDto, String> cityCol = new TableColumn<>("Город");
        cityCol.setCellValueFactory(new PropertyValueFactory<>("city"));
        cityCol.setPrefWidth(100);

        TableColumn<VacancyDto, String> salaryCol = new TableColumn<>("Зарплата");
        salaryCol.setCellValueFactory(cellData -> {
            Integer min = cellData.getValue().getSalaryMin();
            Integer max = cellData.getValue().getSalaryMax();
            String salary = "";
            if (min != null) salary += min;
            salary += " - ";
            if (max != null) salary += max;
            return new javafx.beans.property.SimpleStringProperty(salary);
        });
        salaryCol.setPrefWidth(100);

        TableColumn<VacancyDto, String> activeCol = new TableColumn<>("Активна");
        activeCol.setCellValueFactory(cellData -> {
            Boolean isActive = cellData.getValue().getIsActive();
            return new javafx.beans.property.SimpleStringProperty(isActive != null && isActive ? "Да" : "Нет");
        });
        activeCol.setPrefWidth(80);

        vacanciesTable.getColumns().addAll(idCol, titleCol, companyCol, cityCol, salaryCol, activeCol);
    }

    private void setupComplaintsTable() {
        complaintsTable.setItems(complaintsData);
        complaintsTable.getColumns().clear();
        TableColumn<ComplaintAdminDto, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);

        TableColumn<ComplaintAdminDto, String> typeCol = new TableColumn<>("Тип");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("targetType"));
        typeCol.setPrefWidth(80);

        TableColumn<ComplaintAdminDto, String> targetCol = new TableColumn<>("Цель");
        targetCol.setCellValueFactory(new PropertyValueFactory<>("targetName"));
        targetCol.setPrefWidth(150);

        TableColumn<ComplaintAdminDto, String> reasonCol = new TableColumn<>("Причина");
        reasonCol.setCellValueFactory(new PropertyValueFactory<>("reason"));
        reasonCol.setPrefWidth(200);

        TableColumn<ComplaintAdminDto, String> statusCol = new TableColumn<>("Статус");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(100);

        TableColumn<ComplaintAdminDto, String> dateCol = new TableColumn<>("Дата");
        dateCol.setCellValueFactory(cellData -> {
            if (cellData.getValue().getCreatedAt() != null) {
                return new javafx.beans.property.SimpleStringProperty(
                    cellData.getValue().getCreatedAt().toString().substring(0, 16));
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });
        dateCol.setPrefWidth(120);

        complaintsTable.getColumns().addAll(idCol, typeCol, targetCol, reasonCol, statusCol, dateCol);
    }

    private void setupTagsTable() {
        tagsTable.setItems(tagsData);
        tagsTable.getColumns().clear();
        TableColumn<TagDto, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);

        TableColumn<TagDto, String> nameCol = new TableColumn<>("Название");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(200);

        TableColumn<TagDto, String> activeCol = new TableColumn<>("Активен");
        activeCol.setCellValueFactory(cellData -> {
            Boolean isActive = cellData.getValue().getIsActive();
            return new javafx.beans.property.SimpleStringProperty(isActive != null && isActive ? "Да" : "Нет");
        });
        activeCol.setPrefWidth(100);

        tagsTable.getColumns().addAll(idCol, nameCol, activeCol);
    }

    private void setupNotificationsTable() {
        adminNotificationsTable.setItems(notificationsData);
        adminNotificationsTable.getColumns().clear();

        TableColumn<NotificationDto, String> typeCol = new TableColumn<>("Тип");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeCol.setPrefWidth(100);

        TableColumn<NotificationDto, String> titleCol = new TableColumn<>("Заголовок");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleCol.setPrefWidth(200);

        TableColumn<NotificationDto, String> contentCol = new TableColumn<>("Содержание");
        contentCol.setCellValueFactory(new PropertyValueFactory<>("content"));
        contentCol.setPrefWidth(300);

        TableColumn<NotificationDto, String> dateCol = new TableColumn<>("Дата");
        dateCol.setCellValueFactory(cellData -> {
            if (cellData.getValue().getCreatedAt() != null) {
                return new javafx.beans.property.SimpleStringProperty(
                    cellData.getValue().getCreatedAt().toString().substring(0, 16));
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });
        dateCol.setPrefWidth(150);

        TableColumn<NotificationDto, String> statusCol = new TableColumn<>("Статус");
        statusCol.setCellValueFactory(cellData -> {
            Boolean isRead = cellData.getValue().getIsRead();
            return new javafx.beans.property.SimpleStringProperty(isRead != null && isRead ? "Прочитано" : "Новое");
        });
        statusCol.setPrefWidth(80);

        adminNotificationsTable.getColumns().addAll(typeCol, titleCol, contentCol, dateCol, statusCol);
    }

    @FXML
    private void handleRoleFilterChanged() {
        loadUsers();
    }

    @FXML
    private void handleBlockFilterChanged() {
        loadUsers();
    }

    @FXML
    private void handleVacancyFilterChanged() {
        loadVacancies();
    }

    @FXML
    private void handleRefreshUsers() {
        loadUsers();
    }

    private void loadUsers() {
        if (adminClientService == null) {
            showError("Сервис недоступен");
            return;
        }

        new Thread(() -> {
            try {
                String roleFilter = roleFilterCombo.getValue();
                if ("Все".equals(roleFilter)) roleFilter = null;

                Boolean blockFilter = null;
                String blockValue = blockFilterCombo.getValue();
                if ("Заблокированные".equals(blockValue)) blockFilter = true;
                else if ("Активные".equals(blockValue)) blockFilter = false;

                java.util.List<AdminUserDto> users = adminClientService.getAllUsers(roleFilter, blockFilter, 0, 100);
                Platform.runLater(() -> {
                    usersData.clear();
                    usersData.addAll(users);
                });
            } catch (Exception e) {
                logger.error("Failed to load users: {}", e.getMessage());
                Platform.runLater(() -> showError("Ошибка загрузки пользователей: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleRefreshVacancies() {
        loadVacancies();
    }

    private void loadVacancies() {
        if (adminClientService == null) {
            showError("Сервис недоступен");
            return;
        }

        new Thread(() -> {
            try {
                boolean includeInactive = "Все".equals(vacancyFilterCombo.getValue());
                java.util.List<VacancyDto> vacancies = adminClientService.getAllVacancies(includeInactive);
                Platform.runLater(() -> {
                    vacanciesData.clear();
                    vacanciesData.addAll(vacancies);
                });
            } catch (Exception e) {
                logger.error("Failed to load vacancies: {}", e.getMessage());
                Platform.runLater(() -> showError("Ошибка загрузки вакансий: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleRefreshComplaints() {
        loadComplaints();
    }

    private void loadComplaints() {
        if (adminClientService == null) {
            showError("Сервис недоступен");
            return;
        }

        new Thread(() -> {
            try {
                java.util.List<ComplaintAdminDto> complaints = adminClientService.getComplaints();
                Platform.runLater(() -> {
                    complaintsData.clear();
                    complaintsData.addAll(complaints);
                });
            } catch (Exception e) {
                logger.error("Failed to load complaints: {}", e.getMessage());
                Platform.runLater(() -> showError("Ошибка загрузки жалоб: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleRefreshTags() {
        loadTags();
    }

    private void loadTags() {
        if (adminClientService == null) {
            showError("Сервис недоступен");
            return;
        }

        new Thread(() -> {
            try {
                java.util.List<TagDto> tags = adminClientService.getAllTags();
                Platform.runLater(() -> {
                    tagsData.clear();
                    tagsData.addAll(tags);
                });
            } catch (Exception e) {
                logger.error("Failed to load tags: {}", e.getMessage());
                Platform.runLater(() -> showError("Ошибка загрузки тегов: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleBlockUser() {
        AdminUserDto selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Выберите пользователя");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Блокировка пользователя");
        dialog.setHeaderText("Введите причину блокировки:");
        dialog.setContentText("Причина:");

        dialog.showAndWait().ifPresent(reason -> {
            if (reason == null || reason.trim().isEmpty()) {
                showError("Причина не указана");
                return;
            }
            blockUser(selected.getId(), reason.trim());
        });
    }

    private void blockUser(Integer userId, String reason) {
        if (adminClientService == null) {
            showError("Сервис недоступен");
            return;
        }

        new Thread(() -> {
            try {
                adminClientService.blockUser(userId, reason, null);
                Platform.runLater(() -> {
                    showSuccess("Пользователь заблокирован");
                    loadUsers();
                });
            } catch (Exception e) {
                logger.error("Failed to block user: {}", e.getMessage());
                Platform.runLater(() -> showError("Ошибка блокировки: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleUnblockUser() {
        AdminUserDto selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Выберите пользователя");
            return;
        }

        unblockUser(selected.getId());
    }

    private void unblockUser(Integer userId) {
        if (adminClientService == null) {
            showError("Сервис недоступен");
            return;
        }

        new Thread(() -> {
            try {
                adminClientService.unblockUser(userId);
                Platform.runLater(() -> {
                    showSuccess("Пользователь разблокирован");
                    loadUsers();
                });
            } catch (Exception e) {
                logger.error("Failed to unblock user: {}", e.getMessage());
                Platform.runLater(() -> showError("Ошибка разблокировки: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleDeleteUser() {
        AdminUserDto selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Выберите пользователя");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Удаление пользователя");
        alert.setHeaderText("Вы уверены, что хотите удалить пользователя " + selected.getEmail() + "?");
        alert.setContentText("Это действие необратимо.");

        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                deleteUser(selected.getId());
            }
        });
    }

    private void deleteUser(Integer userId) {
        if (adminClientService == null) {
            showError("Сервис недоступен");
            return;
        }

        new Thread(() -> {
            try {
                adminClientService.deleteUser(userId);
                Platform.runLater(() -> {
                    showSuccess("Пользователь удален");
                    loadUsers();
                });
            } catch (Exception e) {
                logger.error("Failed to delete user: {}", e.getMessage());
                Platform.runLater(() -> showError("Ошибка удаления: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleCreateAdmin() {
        String email = newAdminEmail.getText().trim();
        String password = newAdminPassword.getText();
        String name = newAdminName.getText().trim();

        if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
            showError("Заполните все поля");
            return;
        }

        if (password.length() < 6) {
            showError("Пароль должен содержать минимум 6 символов");
            return;
        }

        if (adminClientService == null) {
            showError("Сервис недоступен");
            return;
        }

        new Thread(() -> {
            try {
                adminClientService.createAdmin(email, password, name);
                Platform.runLater(() -> {
                    showSuccess("Администратор создан");
                    newAdminEmail.clear();
                    newAdminPassword.clear();
                    newAdminName.clear();
                });
            } catch (Exception e) {
                logger.error("Failed to create admin: {}", e.getMessage());
                Platform.runLater(() -> showError("Ошибка создания админа: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleDeleteVacancy() {
        VacancyDto selected = vacanciesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Выберите вакансию");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Удаление вакансии");
        alert.setHeaderText("Вы уверены, что хотите удалить вакансию '" + selected.getTitle() + "'?");
        alert.setContentText("Это действие необратимо.");

        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                deleteVacancy(selected.getId());
            }
        });
    }

    private void deleteVacancy(Integer vacancyId) {
        if (adminClientService == null) {
            showError("Сервис недоступен");
            return;
        }

        new Thread(() -> {
            try {
                adminClientService.deleteVacancy(vacancyId);
                Platform.runLater(() -> {
                    showSuccess("Вакансия удалена");
                    loadVacancies();
                });
            } catch (Exception e) {
                logger.error("Failed to delete vacancy: {}", e.getMessage());
                Platform.runLater(() -> showError("Ошибка удаления: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleDismissComplaint() {
        ComplaintAdminDto selected = complaintsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Выберите жалобу");
            return;
        }

        resolveComplaint(selected.getId(), "DISMISS");
    }

    @FXML
    private void handleBlockVacancyFromComplaint() {
        ComplaintAdminDto selected = complaintsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Выберите жалобу");
            return;
        }

        resolveComplaint(selected.getId(), "BLOCK_VACANCY");
    }

    @FXML
    private void handleBlockUserFromComplaint() {
        ComplaintAdminDto selected = complaintsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Выберите жалобу");
            return;
        }

        resolveComplaint(selected.getId(), "BLOCK_USER");
    }

    private void resolveComplaint(Integer complaintId, String action) {
        if (adminClientService == null) {
            showError("Сервис недоступен");
            return;
        }

        new Thread(() -> {
            try {
                adminClientService.resolveComplaint(complaintId, action);
                Platform.runLater(() -> {
                    showSuccess("Жалоба обработана");
                    loadComplaints();
                });
            } catch (Exception e) {
                logger.error("Failed to resolve complaint: {}", e.getMessage());
                Platform.runLater(() -> showError("Ошибка обработки жалобы: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleAddTag() {
        String tagName = newTagField.getText().trim();
        if (tagName.isEmpty()) {
            showError("Введите название тега");
            return;
        }

        if (adminClientService == null) {
            showError("Сервис недоступен");
            return;
        }

        new Thread(() -> {
            try {
                adminClientService.addTag(tagName);
                Platform.runLater(() -> {
                    showSuccess("Тег добавлен");
                    newTagField.clear();
                    loadTags();
                });
            } catch (Exception e) {
                logger.error("Failed to add tag: {}", e.getMessage());
                Platform.runLater(() -> showError("Ошибка добавления тега: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleDeactivateTag() {
        TagDto selected = tagsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Выберите тег");
            return;
        }

        deactivateTag(selected.getId());
    }

    private void deactivateTag(Integer tagId) {
        if (adminClientService == null) {
            showError("Сервис недоступен");
            return;
        }

        new Thread(() -> {
            try {
                adminClientService.deactivateTag(tagId);
                Platform.runLater(() -> {
                    showSuccess("Тег деактивирован");
                    loadTags();
                });
            } catch (Exception e) {
                logger.error("Failed to deactivate tag: {}", e.getMessage());
                Platform.runLater(() -> showError("Ошибка деактивации тега: " + e.getMessage()));
            }
        }).start();
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

    public void refreshData() {
        loadUsers();
        loadVacancies();
        loadComplaints();
        loadTags();
    }

    @FXML
    private void handleLoadAnalytics() {
        loadSystemStats();
    }

    private void loadSystemStats() {
        if (analyticsClientService == null) {
            NetworkService networkService = AppContext.getNetworkService();
            if (networkService != null) {
                analyticsClientService = new AnalyticsClientService(networkService);
            } else {
                showError("Сервис недоступен");
                return;
            }
        }

        int periodDays = getAnalyticsPeriodDays();

        new Thread(() -> {
            try {
                com.jobapp.common.dto.SystemStatsDto stats = analyticsClientService.getSystemStats();
                List<ChartDataPointDto> timeline = analyticsClientService.getActivityTimeline(periodDays);
                List<TagUsageDto> topTags = analyticsClientService.getTopTags(periodDays, 10);

                Platform.runLater(() -> {
                    updateAdminStatsCards(stats);
                    updateAdminPieChart(stats);
                    updateAdminTagsChart(topTags);
                    updateAdminActivityChart(timeline);
                });
            } catch (Exception e) {
                logger.error("Failed to load system stats: {}", e.getMessage());
                Platform.runLater(() -> showError("Ошибка загрузки статистики: " + e.getMessage()));
            }
        }).start();
    }

    private int getAnalyticsPeriodDays() {
        String period = analyticsPeriodCombo.getValue();
        if (period == null) return 30;
        return switch (period) {
            case "Неделя" -> 7;
            case "Месяц" -> 30;
            case "Год" -> 365;
            default -> 30;
        };
    }

    private void updateAdminStatsCards(com.jobapp.common.dto.SystemStatsDto stats) {
        if (stats == null) return;

        adminTotalUsersLabel.setText(String.valueOf(stats.getTotalUsers() != null ? stats.getTotalUsers() : 0));
        adminTotalVacanciesLabel.setText(String.valueOf(stats.getTotalVacancies() != null ? stats.getTotalVacancies() : 0));
        adminTotalApplicationsLabel.setText(String.valueOf(stats.getTotalApplications() != null ? stats.getTotalApplications() : 0));
        adminTotalApplicantsLabel.setText(String.valueOf(stats.getTotalApplicants() != null ? stats.getTotalApplicants() : 0));
        adminTotalHrLabel.setText(String.valueOf(stats.getTotalHr() != null ? stats.getTotalHr() : 0));
    }

    private void updateAdminPieChart(com.jobapp.common.dto.SystemStatsDto stats) {
        adminUsersPieChart.getData().clear();

        if (stats == null) return;

        javafx.collections.ObservableList<PieChart.Data> data = javafx.collections.FXCollections.observableArrayList(
            new PieChart.Data("Соискатели", stats.getTotalApplicants() != null ? stats.getTotalApplicants() : 0),
            new PieChart.Data("HR", stats.getTotalHr() != null ? stats.getTotalHr() : 0),
            new PieChart.Data("Админы", stats.getTotalAdmins() != null ? stats.getTotalAdmins() : 0)
        );

        adminUsersPieChart.setData(data);
    }

    private void updateAdminTagsChart(List<TagUsageDto> tags) {
        adminTagsBarChart.getData().clear();

        if (tags == null || tags.isEmpty()) return;

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Теги");

        for (TagUsageDto tag : tags) {
            series.getData().add(new XYChart.Data<>(tag.getName(), tag.getCount()));
        }

        adminTagsBarChart.getData().add(series);
    }

    private void updateAdminActivityChart(List<ChartDataPointDto> timeline) {
        adminActivityChart.getData().clear();

        if (timeline == null || timeline.isEmpty()) return;

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Активность");

        for (ChartDataPointDto point : timeline) {
            series.getData().add(new XYChart.Data<>(point.getLabel(), point.getValue()));
        }

        adminActivityChart.getData().add(series);
    }

    @FXML
    private void handleRefreshNotifications() {
        loadNotifications();
    }

    private void loadNotifications() {
        if (notificationClientService == null) {
            showError("Сервис уведомлений недоступен");
            return;
        }

        new Thread(() -> {
            try {
                List<NotificationDto> notifications = notificationClientService.getNotifications(false, 0, 50);
                int unreadCount = notificationClientService.getUnreadCount();
                Platform.runLater(() -> {
                    notificationsData.clear();
                    notificationsData.addAll(notifications);
                    updateAdminBadge(unreadCount);
                });
            } catch (Exception e) {
                logger.error("Failed to load notifications: {}", e.getMessage());
                Platform.runLater(() -> showError("Ошибка загрузки уведомлений: " + e.getMessage()));
            }
        }).start();
    }

    private void updateAdminBadge(int count) {
        unreadCount = count;
        if (count > 0) {
            adminBadgeCircle.setVisible(true);
            adminBadgeLabel.setVisible(true);
            adminBadgeLabel.setText(count > 99 ? "99+" : String.valueOf(count));
        } else {
            adminBadgeCircle.setVisible(false);
            adminBadgeLabel.setVisible(false);
        }
    }

    @FXML
    private void handleMarkNotificationRead() {
        NotificationDto selected = adminNotificationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Выберите уведомление");
            return;
        }

        if (notificationClientService == null) {
            showError("Сервис уведомлений недоступен");
            return;
        }

        new Thread(() -> {
            try {
                notificationClientService.markAsRead(selected.getId());
                Platform.runLater(() -> {
                    showSuccess("Уведомление отмечено прочитанным");
                    loadNotifications();
                });
            } catch (Exception e) {
                logger.error("Failed to mark notification as read: {}", e.getMessage());
                Platform.runLater(() -> showError("Ошибка: " + e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleDeleteNotification() {
        NotificationDto selected = adminNotificationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Выберите уведомление");
            return;
        }

        if (notificationClientService == null) {
            showError("Сервис уведомлений недоступен");
            return;
        }

        new Thread(() -> {
            try {
                notificationClientService.delete(selected.getId());
                Platform.runLater(() -> {
                    showSuccess("Уведомление удалено");
                    loadNotifications();
                });
            } catch (Exception e) {
                logger.error("Failed to delete notification: {}", e.getMessage());
                Platform.runLater(() -> showError("Ошибка: " + e.getMessage()));
            }
        }).start();
    }
}