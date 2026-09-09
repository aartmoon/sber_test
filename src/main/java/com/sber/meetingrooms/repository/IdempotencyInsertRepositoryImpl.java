package com.sber.meetingrooms.repository;

import com.sber.meetingrooms.model.BookingIdempotency;
import jakarta.persistence.EntityManager;

public class IdempotencyInsertRepositoryImpl implements IdempotencyInsertRepository {
    private final EntityManager entityManager;

    public IdempotencyInsertRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void insertAndFlush(BookingIdempotency request) {
        entityManager.persist(request);
        entityManager.flush();
    }
}
