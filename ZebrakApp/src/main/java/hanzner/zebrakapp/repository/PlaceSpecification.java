package hanzner.zebrakapp.repository;

import hanzner.zebrakapp.entity.Category;
import hanzner.zebrakapp.entity.DiscountType;
import hanzner.zebrakapp.entity.Place;
import hanzner.zebrakapp.entity.PlaceStatus;
import hanzner.zebrakapp.entity.PriceLevel;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class PlaceSpecification {

    public static Specification<Place> filterPlaces(
            PlaceStatus status,
            Category category,
            PriceLevel priceLevel,
            DiscountType discountType,
            Double minLat,
            Double maxLat,
            Double minLng,
            Double maxLng,
            String query
    ) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }

            if (priceLevel != null) {
                predicates.add(cb.equal(root.get("priceLevel"), priceLevel));
            }

            if (discountType != null) {
                predicates.add(cb.equal(root.get("discountType"), discountType));
            }

            if (minLat != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("latitude"), minLat));
            }

            if (maxLat != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("latitude"), maxLat));
            }

            if (minLng != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("longitude"), minLng));
            }

            if (maxLng != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("longitude"), maxLng));
            }

            if (query != null && !query.trim().isEmpty()) {
                String pattern = "%" + query.trim().toLowerCase() + "%";
                Predicate titleLike = cb.like(cb.lower(root.get("title")), pattern);
                Predicate descLike = cb.like(cb.lower(root.get("description")), pattern);
                Predicate cityLike = cb.like(cb.lower(root.get("city")), pattern);
                Predicate addressLike = cb.like(cb.lower(root.get("address")), pattern);
                predicates.add(cb.or(titleLike, descLike, cityLike, addressLike));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
