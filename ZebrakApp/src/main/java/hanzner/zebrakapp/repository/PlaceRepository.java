package hanzner.zebrakapp.repository;

import hanzner.zebrakapp.entity.Place;
import hanzner.zebrakapp.entity.PlaceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlaceRepository extends JpaRepository<Place, Long>, JpaSpecificationExecutor<Place> {

    List<Place> findByStatusOrderByCreatedAtDesc(PlaceStatus status);

    List<Place> findByUserIdOrderByCreatedAtDesc(Long userId);
}
