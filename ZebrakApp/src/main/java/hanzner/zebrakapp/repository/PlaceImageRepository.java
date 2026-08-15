package hanzner.zebrakapp.repository;

import hanzner.zebrakapp.entity.PlaceImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlaceImageRepository extends JpaRepository<PlaceImage, Long> {
    List<PlaceImage> findByPlaceIdOrderByIsPrimaryDescCreatedAtAsc(Long placeId);
    Optional<PlaceImage> findByFilename(String filename);
}
