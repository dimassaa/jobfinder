package com.jobapp.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);
    private static final SessionManager instance = new SessionManager();

    private final ConcurrentHashMap<Integer, PrintWriter> activeSessions = new ConcurrentHashMap<>();

    public static SessionManager getInstance() {
        return instance;
    }

    private SessionManager() {
    }

    public void addSession(int userId, PrintWriter out) {
        activeSessions.put(userId, out);
        logger.info("Session added for userId: {}", userId);
    }

    public void removeSession(int userId) {
        activeSessions.remove(userId);
        logger.info("Session removed for userId: {}", userId);
    }

    public PrintWriter getSession(int userId) {
        return activeSessions.get(userId);
    }

    public boolean isOnline(int userId) {
        return activeSessions.containsKey(userId);
    }
}