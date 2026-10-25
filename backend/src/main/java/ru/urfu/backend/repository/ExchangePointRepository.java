package ru.urfu.backend.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.urfu.backend.entity.ExchangePoint;
import ru.urfu.backend.entity.enums.ItemType;
import ru.urfu.backend.entity.enums.PointStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ExchangePointRepository extends JpaRepository<ExchangePoint, Long> {

    Optional<ExchangePoint> findByOrganizerId(Long organizerId);

    boolean existsByOrganizerId(Long organizerId);

    @EntityGraph(attributePaths = "acceptedTypes")
    List<ExchangePoint> findByStatus(PointStatus status);

    @EntityGraph(attributePaths = "acceptedTypes")
    @Query("""
            select distinct p from ExchangePoint p
            where p.status = :status
              and exists (select 1 from ExchangePoint p2 join p2.acceptedTypes t
                          where p2 = p and t in :types)
            """)
    List<ExchangePoint> findByStatusAndAcceptedTypesIn(@Param("status") PointStatus status,
                                                       @Param("types") Collection<ItemType> types);
}
