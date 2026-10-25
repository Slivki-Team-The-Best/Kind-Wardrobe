package ru.urfu.backend.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.urfu.backend.entity.Listing;
import ru.urfu.backend.entity.enums.ListingStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ListingRepository extends JpaRepository<Listing, Long>, JpaSpecificationExecutor<Listing> {

    List<Listing> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    List<Listing> findByAuthorIdAndStatusOrderByCreatedAtDesc(Long authorId, ListingStatus status);

    @EntityGraph(attributePaths = {"photos", "author"})
    Optional<Listing> findWithPhotosAndAuthorById(Long id);

    long countByAuthorIdAndStatus(Long authorId, ListingStatus status);

    long countByStatus(ListingStatus status);

    @Query("""
            select coalesce(sum(l.weightKg), 0) from Listing l
            where l.author.id = :authorId and l.status = :status
            """)
    BigDecimal sumWeightByAuthorAndStatus(@Param("authorId") Long authorId,
                                          @Param("status") ListingStatus status);

    @Query("select coalesce(sum(l.weightKg), 0) from Listing l where l.status = :status")
    BigDecimal sumWeightByStatus(@Param("status") ListingStatus status);

    default BigDecimal sumSavedKgByAuthor(Long authorId) {
        return sumWeightByAuthorAndStatus(authorId, ListingStatus.TRANSFERRED);
    }

    default BigDecimal sumSavedKgTotal() {
        return sumWeightByStatus(ListingStatus.TRANSFERRED);
    }
}
