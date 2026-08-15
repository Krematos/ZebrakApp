package hanzner.zebrakapp.repository;

import hanzner.zebrakapp.entity.Category;
import hanzner.zebrakapp.entity.DiscountType;
import hanzner.zebrakapp.entity.Place;
import hanzner.zebrakapp.entity.PlaceStatus;
import hanzner.zebrakapp.entity.PriceLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlaceRepository extends JpaRepository<Place, Long> {

    List<Place> findByStatusOrderByCreatedAtDesc(PlaceStatus status);

    List<Place> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT p FROM Place p WHERE p.status = :status " +
           "AND (:category IS NULL OR p.category = :category) " +
           "AND (:priceLevel IS NULL OR p.priceLevel = :priceLevel) " +
           "AND (:discountType IS NULL OR p.discountType = :discountType) " +
           "AND (:minLat IS NULL OR p.latitude >= :minLat) " +
           "AND (:maxLat IS NULL OR p.latitude <= :maxLat) " +
           "AND (:minLng IS NULL OR p.longitude >= :minLng) " +
           "AND (:maxLng IS NULL OR p.longitude <= :maxLng) " +
           "AND (:query IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "     OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "     OR LOWER(p.city) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "     OR LOWER(p.address) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY p.createdAt DESC")
    List<Place> searchApprovedPlaces(
            @Param("status") PlaceStatus status,
            @Param("category") Category category,
            @Param("priceLevel") PriceLevel priceLevel,
            @Param("discountType") DiscountType discountType,
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLng") Double minLng,
            @Param("maxLng") Double maxLng,
            @Param("query") String query
    );
}
