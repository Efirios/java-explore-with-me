package ru.practicum.ewm.event;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    boolean existsByCategoryId(Long categoryId);

    List<Event> findByInitiatorId(Long initiatorId, Pageable pageable);

    Optional<Event> findByIdAndInitiatorId(Long id, Long initiatorId);

    Optional<Event> findByIdAndState(Long id, EventState state);

    @Query(value = "SELECT * FROM events e WHERE e.state = 'PUBLISHED' AND "
            + "6371 * acos(LEAST(1.0, sin(radians(:lat)) * sin(radians(e.lat)) "
            + "+ cos(radians(:lat)) * cos(radians(e.lat)) * cos(radians(e.lon) - radians(:lon)))) <= :radius",
            nativeQuery = true)
    List<Event> findPublishedInArea(@Param("lat") Float lat, @Param("lon") Float lon, @Param("radius") Float radius);
}
