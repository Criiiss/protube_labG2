package com.tecnocampus.LS2.protube_back.profile.adapter.out.persistence;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UserJpaEntityTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-10-10T08:00:00Z");
    private static final OffsetDateTime UPDATED_AT =OffsetDateTime.parse("2026-10-11T09:00:00Z");

    private UserJpaEntity createUser(OffsetDateTime createdAt,OffsetDateTime updatedAt) {
        return new UserJpaEntity((long)1D,"maria_23","maria@example.com","password","LOCAL",null,createdAt,updatedAt);
    }
    @Test
    void onCreateSetsBothDatesWhenTheyAreNull() {
        UserJpaEntity user = createUser(null, null);
        user.onCreate();
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
    }

    @Test
    void onCreateSetsCreatedAtWhenItIsNull() {
        UserJpaEntity user = createUser(null, UPDATED_AT);
        user.onCreate();
        assertNotNull(user.getCreatedAt());
        assertEquals(UPDATED_AT, user.getUpdatedAt());
    }

    @Test
    void onCreateSetsUpdatedAtWhenItIsNull() {
        UserJpaEntity user = createUser(CREATED_AT, null);
        user.onCreate();
        assertEquals(CREATED_AT, user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
    }

    @Test
    void onCreateKeepsBothDatesWhenTheyAlreadyExist() {
        UserJpaEntity user = createUser(CREATED_AT, UPDATED_AT);
        user.onCreate();
        assertEquals(CREATED_AT, user.getCreatedAt());
        assertEquals(UPDATED_AT, user.getUpdatedAt());
    }
}