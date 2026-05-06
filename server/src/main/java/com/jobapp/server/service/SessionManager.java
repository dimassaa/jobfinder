package com.jobapp.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SessionManager {

    private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);
    private static final SessionManager instance = new SessionManager();
    private static final long SESSION_TIMEOUT_MINUTES = 30;
    private static final long CLEANUP_INTERVAL_MINUTES = 5;

    private final ConcurrentHashMap<Integer, PrintWriter> activeSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Long> sessionTimestamps = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupScheduler;

    public static SessionManager getInstance() {
        return instance;
    }

    private SessionManager() {
        cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SessionCleanup");
            t.setDaemon(true);
            return t;
        });
        cleanupScheduler.scheduleAtFixedRate(this::cleanupExpiredSessions,
                CLEANUP_INTERVAL_MINUTES, CLEANUP_INTERVAL_MINUTES, TimeUnit.MINUTES);
        logger.info("Session cleanup scheduler started, interval: {} minutes", CLEANUP_INTERVAL_MINUTES);
    }

    private void cleanupExpiredSessions() {
        long now = System.currentTimeMillis();
        long timeoutMs = SESSION_TIMEOUT_MINUTES * 60 * 1000;
        int cleaned = 0;
        Iterator<Integer> iterator = sessionTimestamps.keySet().iterator();
        while (iterator.hasNext()) {
            Integer userId = iterator.next();
            Long timestamp = sessionTimestamps.get(userId);
            if (timestamp != null && (now - timestamp) > timeoutMs) {
                removeSession(userId);
                cleaned++;
            }
        }
        if (cleaned > 0) {
            logger.info("Cleaned up {} expired sessions", cleaned);
        }
    }

    public void shutdown() {
        cleanupScheduler.shutdown();
        logger.info("SessionManager shutdown");
    }

    public void addSession(int userId, PrintWriter out) {
        activeSessions.put(userId, out);
        sessionTimestamps.put(userId, System.currentTimeMillis());
        logger.info("Session added for userId: {}", userId);
    }

    public void removeSession(int userId) {
        activeSessions.remove(userId);
        sessionTimestamps.remove(userId);
        logger.info("Session removed for userId: {}", userId);
    }

    public PrintWriter getSession(int userId) {
        return activeSessions.get(userId);
    }

    public boolean isOnline(int userId) {
        return activeSessions.containsKey(userId);
    }

    public boolean isSessionExpired(int userId) {
        Long timestamp = sessionTimestamps.get(userId);
        if (timestamp == null) {
            return true;
        }
        long elapsed = System.currentTimeMillis() - timestamp;
        boolean expired = elapsed > SESSION_TIMEOUT_MINUTES * 60 * 1000;
        if (expired) {
            logger.info("Session expired for userId: {}", userId);
            removeSession(userId);
        }
        return expired;
    }

    public void refreshSession(int userId) {
        sessionTimestamps.put(userId, System.currentTimeMillis());
    }
}