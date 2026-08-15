package hanzner.zebrakapp.repository;

import hanzner.zebrakapp.entity.PlaceVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlaceVerificationRepository extends JpaRepository<PlaceVerification, Long> {
    Optional<PlaceVerification> findByPlaceIdAndUserId(Long placeId, Long userId);
    boolean existsByPlaceIdAndUserId(Long placeId, Long userId);
    Optional<PlaceVerification> findByPlaceIdAndIpAddress(Long placeId, String ipAddress);
}
