package com.jobapp.server.service;

import com.jobapp.common.dto.NotificationDto;
import com.jobapp.server.model.User;
import com.jobapp.server.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ProfileReminderTask implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ProfileReminderTask.class);

    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public ProfileReminderTask(UserRepository userRepository, NotificationService notificationService) {
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Override
    public void run() {
        logger.info("Running profile reminder task");

        try {
            List<User> usersWithIncompleteProfile = userRepository.findUsersWithIncompleteProfile();

            for (User user : usersWithIncompleteProfile) {
                NotificationDto notification = new NotificationDto();
                notification.setType("PROFILE_REMINDER");
                notification.setTitle("Complete Your Profile");
                notification.setContent("Please fill in your city and add skills to get better job recommendations.");
                notification.setLink("/profile");

                notificationService.sendToUser(user.getId(), notification);
                logger.info("Sent profile reminder to user: {}", user.getId());
            }

            logger.info("Profile reminder task completed. Sent {} reminders", usersWithIncompleteProfile.size());
        } catch (Exception e) {
            logger.error("Error in profile reminder task", e);
        }
    }
}