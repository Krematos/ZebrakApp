package hanzner.zebrakapp.repository;

import hanzner.zebrakapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query(value = "SELECT * FROM users WHERE deleted_at IS NOT NULL AND deleted_at <= :threshold", nativeQuery = true)
    List<User> findExpiredSoftDeletedUsers(@Param("threshold") LocalDateTime threshold);

    @Modifying
    @Query(value = "DELETE FROM users WHERE id IN (:ids)", nativeQuery = true)
    int hardDeleteUsersByIds(@Param("ids") List<Long> ids);

    @Query(value = "SELECT * FROM users WHERE id = :id", nativeQuery = true)
    Optional<User> findByIdIncludingDeleted(@Param("id") Long id);
}
