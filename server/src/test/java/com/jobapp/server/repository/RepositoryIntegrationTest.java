package com.jobapp.server.repository;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RepositoryIntegrationTest {

    @Test
    void testRepositoriesExist() {
        assertNotNull(new FavoriteRepository());
        assertNotNull(new ComplaintRepository());
        assertNotNull(new ViewHistoryRepository());
    }
}