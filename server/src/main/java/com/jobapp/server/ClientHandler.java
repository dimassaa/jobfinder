package com.jobapp.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jobapp.common.Message;
import com.jobapp.common.Response;
import com.jobapp.server.handler.LoginCommand;
import com.jobapp.server.handler.RegisterCommand;
import com.jobapp.server.handler.GetProfileCommand;
import com.jobapp.server.handler.UpdateProfileCommand;
import com.jobapp.server.handler.GetUserTagsCommand;
import com.jobapp.server.handler.UpdateUserTagsCommand;
import com.jobapp.server.handler.GetAllTagsCommand;
import com.jobapp.server.handler.CreateVacancyCommand;
import com.jobapp.server.handler.UpdateVacancyCommand;
import com.jobapp.server.handler.CloseVacancyCommand;
import com.jobapp.server.handler.GetVacancyDetailsCommand;
import com.jobapp.server.handler.SearchVacanciesCommand;
import com.jobapp.server.handler.GetMyVacanciesCommand;
import com.jobapp.server.handler.ApplyCommand;
import com.jobapp.server.handler.GetMyApplicationsCommand;
import com.jobapp.server.handler.GetApplicationsForVacancyCommand;
import com.jobapp.server.handler.UpdateApplicationStatusCommand;
import com.jobapp.server.handler.AddFavoriteCommand;
import com.jobapp.server.handler.RemoveFavoriteCommand;
import com.jobapp.server.handler.GetFavoritesCommand;
import com.jobapp.server.handler.AddViewCommand;
import com.jobapp.server.handler.CreateComplaintCommand;
import com.jobapp.server.handler.GetRecommendationsCommand;
import com.jobapp.server.repository.CompanyRepository;
import com.jobapp.server.repository.TagRepository;
import com.jobapp.server.repository.UserRepository;
import com.jobapp.server.repository.VacancyRepository;
import com.jobapp.server.service.AuthService;
import com.jobapp.server.service.SessionManager;
import com.jobapp.server.service.TagService;
import com.jobapp.server.service.UserService;
import com.jobapp.server.service.VacancyService;
import com.jobapp.server.service.ApplicationService;
import com.jobapp.server.service.FavoriteService;
import com.jobapp.server.service.ViewHistoryService;
import com.jobapp.server.service.ComplaintService;
import com.jobapp.server.service.NotificationService;
import com.jobapp.server.service.RecommendationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.Supplier;

public class ClientHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
    private final Socket socket;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final TagRepository tagRepository;
    private final AuthService authService;
    private final SessionManager sessionManager;
    private final UserService userService;
    private final TagService tagService;
    private final ApplicationService applicationService;
    private final FavoriteService favoriteService;
    private final ViewHistoryService viewHistoryService;
    private final ComplaintService complaintService;
    private final RecommendationService recommendationService;
    private final LoginCommand loginCommand;
    private final RegisterCommand registerCommand;
    private final VacancyService vacancyService;
    private GetProfileCommand getProfileCommand;
    private UpdateProfileCommand updateProfileCommand;
    private GetUserTagsCommand getUserTagsCommand;
    private UpdateUserTagsCommand updateUserTagsCommand;
    private GetAllTagsCommand getAllTagsCommand;
    private CreateVacancyCommand createVacancyCommand;
    private UpdateVacancyCommand updateVacancyCommand;
    private CloseVacancyCommand closeVacancyCommand;
    private GetVacancyDetailsCommand getVacancyDetailsCommand;
    private SearchVacanciesCommand searchVacanciesCommand;
    private GetMyVacanciesCommand getMyVacanciesCommand;
    private ApplyCommand applyCommand;
    private GetMyApplicationsCommand getMyApplicationsCommand;
    private GetApplicationsForVacancyCommand getApplicationsForVacancyCommand;
    private UpdateApplicationStatusCommand updateApplicationStatusCommand;
    private AddFavoriteCommand addFavoriteCommand;
    private RemoveFavoriteCommand removeFavoriteCommand;
    private GetFavoritesCommand getFavoritesCommand;
    private AddViewCommand addViewCommand;
    private CreateComplaintCommand createComplaintCommand;
    private GetRecommendationsCommand getRecommendationsCommand;
    private Integer currentUserId;
    private boolean shouldClose = false;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.userRepository = new UserRepository();
        this.companyRepository = new CompanyRepository();
        this.tagRepository = new TagRepository();
        this.sessionManager = SessionManager.getInstance();
        this.authService = new AuthService(userRepository, companyRepository);
        this.userService = new UserService(userRepository, companyRepository, tagRepository);
        this.tagService = new TagService(tagRepository);
        this.vacancyService = new VacancyService(new VacancyRepository(), companyRepository, tagRepository);
        this.loginCommand = new LoginCommand(authService, sessionManager, objectMapper);
        this.registerCommand = new RegisterCommand(authService, objectMapper);
        this.getVacancyDetailsCommand = new GetVacancyDetailsCommand(vacancyService, objectMapper);
        this.searchVacanciesCommand = new SearchVacanciesCommand(vacancyService, objectMapper);
        this.applicationService = createApplicationService();
        this.favoriteService = createFavoriteService();
        this.viewHistoryService = createViewHistoryService();
        this.complaintService = createComplaintService();
        this.recommendationService = new RecommendationService(
                new VacancyRepository(),
                userRepository,
                tagRepository,
                companyRepository);
    }

    private ApplicationService createApplicationService() {
        return new ApplicationService(
                new com.jobapp.server.repository.ApplicationRepository(),
                new VacancyRepository(),
                userRepository,
                companyRepository,
                new NotificationService(
                        new com.jobapp.server.repository.NotificationRepository(),
                        new VacancyRepository(),
                        companyRepository));
    }

    private FavoriteService createFavoriteService() {
        return new FavoriteService(
                new com.jobapp.server.repository.FavoriteRepository(),
                companyRepository,
                tagRepository);
    }

    private ViewHistoryService createViewHistoryService() {
        return new ViewHistoryService(
                new com.jobapp.server.repository.ViewHistoryRepository(),
                companyRepository,
                tagRepository);
    }

    private ComplaintService createComplaintService() {
        return new ComplaintService(
                new com.jobapp.server.repository.ComplaintRepository(),
                new VacancyRepository(),
                userRepository,
                new NotificationService(
                        new com.jobapp.server.repository.NotificationRepository(),
                        new VacancyRepository(),
                        companyRepository));
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String line;
            while (!shouldClose && (line = in.readLine()) != null) {
                logger.info("Received from {}: {}", socket.getInetAddress(), line);

                if ("exit".equalsIgnoreCase(line)) {
                    logger.info("Client {} requested disconnect", socket.getInetAddress());
                    break;
                }

                try {
                    Message msg = objectMapper.readValue(line, Message.class);
                    String responseJson = processMessage(msg, out);
                    out.println(responseJson);
                    logger.info("Sent to {}: {}", socket.getInetAddress(), responseJson);
                } catch (Exception e) {
                    logger.error("Error processing message from {}", socket.getInetAddress(), e);
                    Response errorResponse = new Response("ERROR", "Invalid message format", null);
                    out.println(objectMapper.writeValueAsString(errorResponse));
                }
            }
        } catch (IOException e) {
            logger.error("Connection error with {}", socket.getInetAddress(), e);
        } finally {
            if (currentUserId != null) {
                sessionManager.removeSession(currentUserId);
            }
            try {
                socket.close();
            } catch (IOException e) {
                logger.error("Error closing socket", e);
            }
        }
    }

    private String processMessage(Message msg, PrintWriter out) throws IOException {
        String command = msg.getCommand();
        logger.info("Received command: {} from {}", command, socket.getInetAddress());

        Response response;

        switch (command != null ? command.toUpperCase() : "") {
            case "PING":
                response = new Response("OK", null, "PONG");
                break;
            case "LOGIN":
                response = loginCommand.execute(msg, out);
                if (response.getData() != null) {
                    var authResp = (com.jobapp.common.dto.AuthResponse) response.getData();
                    if (authResp.isSuccess()) {
                        currentUserId = authResp.getUserId();
                        initProfileCommands();
                    }
                }
                break;
            case "REGISTER":
                response = registerCommand.execute(msg);
                break;
            case "GET_PROFILE":
                response = checkAuthAndExecute(() -> getProfileCommand.execute(), "GET_PROFILE");
                break;
            case "UPDATE_PROFILE":
                response = checkAuthAndExecute(() -> updateProfileCommand.execute(msg), "UPDATE_PROFILE");
                break;
            case "GET_USER_TAGS":
                response = checkAuthAndExecute(() -> getUserTagsCommand.execute(), "GET_USER_TAGS");
                break;
            case "UPDATE_USER_TAGS":
                response = checkAuthAndExecute(() -> updateUserTagsCommand.execute(msg), "UPDATE_USER_TAGS");
                break;
            case "GET_ALL_TAGS":
                response = getAllTagsCommand.execute();
                break;
            case "CREATE_VACANCY":
                response = checkAuthAndExecute(() -> createVacancyCommand.execute(msg), "CREATE_VACANCY");
                break;
            case "UPDATE_VACANCY":
                response = checkAuthAndExecute(() -> updateVacancyCommand.execute(msg), "UPDATE_VACANCY");
                break;
            case "CLOSE_VACANCY":
                response = checkAuthAndExecute(() -> closeVacancyCommand.execute(msg), "CLOSE_VACANCY");
                break;
            case "GET_VACANCY_DETAILS":
                response = getVacancyDetailsCommand.execute(msg);
                break;
            case "SEARCH_VACANCIES":
                response = searchVacanciesCommand.execute(msg);
                break;
            case "GET_MY_VACANCIES":
                response = checkAuthAndExecute(() -> getMyVacanciesCommand.execute(msg), "GET_MY_VACANCIES");
                break;
            case "APPLY":
                response = checkAuthAndExecute(() -> applyCommand.execute(msg), "APPLY");
                break;
            case "GET_MY_APPLICATIONS":
                response = checkAuthAndExecute(() -> getMyApplicationsCommand.execute(), "GET_MY_APPLICATIONS");
                break;
            case "GET_APPLICATIONS_FOR_VACANCY":
                response = checkAuthAndExecute(() -> getApplicationsForVacancyCommand.execute(msg), "GET_APPLICATIONS_FOR_VACANCY");
                break;
            case "UPDATE_APPLICATION_STATUS":
                response = checkAuthAndExecute(() -> updateApplicationStatusCommand.execute(msg), "UPDATE_APPLICATION_STATUS");
                break;
            case "ADD_FAVORITE":
                response = checkAuthAndExecute(() -> addFavoriteCommand.execute(msg), "ADD_FAVORITE");
                break;
            case "REMOVE_FAVORITE":
                response = checkAuthAndExecute(() -> removeFavoriteCommand.execute(msg), "REMOVE_FAVORITE");
                break;
            case "GET_FAVORITES":
                response = checkAuthAndExecute(() -> getFavoritesCommand.execute(), "GET_FAVORITES");
                break;
            case "ADD_VIEW":
                response = checkAuthAndExecute(() -> addViewCommand.execute(msg), "ADD_VIEW");
                break;
            case "CREATE_COMPLAINT":
                response = checkAuthAndExecute(() -> createComplaintCommand.execute(msg), "CREATE_COMPLAINT");
                break;
            case "GET_RECOMMENDATIONS":
                response = checkAuthAndExecute(() -> getRecommendationsCommand.execute(), "GET_RECOMMENDATIONS");
                break;
            case "TEST_DB":
                var testUser = userRepository.findByEmail("test@example.com");
                if (testUser.isPresent()) {
                    response = new Response("OK", "User found: " + testUser.get().getEmail(), testUser.get());
                } else {
                    response = new Response("OK", "No users found in database", null);
                }
                break;
            case "EXIT":
                response = new Response("OK", "Goodbye", null);
                shouldClose = true;
                break;
            case "LOGOUT":
                if (currentUserId != null) {
                    sessionManager.removeSession(currentUserId);
                    logger.info("User {} logged out", currentUserId);
                    currentUserId = null;
                }
                response = new Response("OK", "Logged out successfully", null);
                shouldClose = true;
                break;
            default:
                response = new Response("OK", "Echo: " + command, msg.getData());
        }

        return objectMapper.writeValueAsString(response);
    }

    private void initProfileCommands() {
        this.getProfileCommand = new GetProfileCommand(userService, currentUserId);
        this.updateProfileCommand = new UpdateProfileCommand(userService, objectMapper, currentUserId);
        this.getUserTagsCommand = new GetUserTagsCommand(userService, currentUserId);
        this.updateUserTagsCommand = new UpdateUserTagsCommand(userService, objectMapper, currentUserId);
        this.getAllTagsCommand = new GetAllTagsCommand(tagService);

        this.applyCommand = new ApplyCommand(applicationService, objectMapper, currentUserId);
        this.getMyApplicationsCommand = new GetMyApplicationsCommand(applicationService, currentUserId);
        this.addFavoriteCommand = new AddFavoriteCommand(favoriteService, objectMapper, currentUserId);
        this.removeFavoriteCommand = new RemoveFavoriteCommand(favoriteService, objectMapper, currentUserId);
        this.getFavoritesCommand = new GetFavoritesCommand(favoriteService, currentUserId);
        this.addViewCommand = new AddViewCommand(viewHistoryService, objectMapper, currentUserId);
        this.createComplaintCommand = new CreateComplaintCommand(complaintService, objectMapper, currentUserId);
        this.getRecommendationsCommand = new GetRecommendationsCommand(recommendationService, currentUserId);

        var user = userRepository.findById(currentUserId).orElse(null);
        if (user != null && "HR".equalsIgnoreCase(user.getRole())) {
            this.createVacancyCommand = new CreateVacancyCommand(vacancyService, userRepository,
                    companyRepository, objectMapper, currentUserId);
            this.updateVacancyCommand = new UpdateVacancyCommand(vacancyService, userRepository,
                    companyRepository, objectMapper, currentUserId);
            this.closeVacancyCommand = new CloseVacancyCommand(vacancyService, userRepository,
                    objectMapper, currentUserId);
            this.getMyVacanciesCommand = new GetMyVacanciesCommand(vacancyService, userRepository,
                    companyRepository, currentUserId);
            this.getApplicationsForVacancyCommand = new GetApplicationsForVacancyCommand(applicationService, objectMapper, currentUserId);
            this.updateApplicationStatusCommand = new UpdateApplicationStatusCommand(applicationService, objectMapper, currentUserId);
        }
    }

    private Response checkAuthAndExecute(java.util.function.Supplier<Response> action, String commandName) {
        if (currentUserId == null) {
            logger.warn("Unauthorized attempt to execute: {}", commandName);
            return new Response("ERROR", "Unauthorized. Please login first.", null);
        }
        return action.get();
    }
}