package com.smartcommerce.inventory.repository;

import com.smartcommerce.inventory.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.*;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    Optional<Reservation> findByReservationKey(String reservationKey);
    List<Reservation> findByOrderIdAndStatus(UUID orderId, ReservationStatus status);
    List<Reservation> findByStatusAndExpiresAtLessThanEqual(ReservationStatus status, Instant now);
}
