package com.example.Backend.repository;

import com.example.Backend.model.NotificationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRequestRepository extends JpaRepository<NotificationRequest, Long> {
}
