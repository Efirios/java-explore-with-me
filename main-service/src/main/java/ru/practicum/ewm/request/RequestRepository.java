package ru.practicum.ewm.request;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RequestRepository extends JpaRepository<ParticipationRequest, Long> {

    List<ParticipationRequest> findByRequesterId(Long requesterId);

    List<ParticipationRequest> findByEventId(Long eventId);

    List<ParticipationRequest> findByEventIdAndStatus(Long eventId, RequestStatus status);

    List<ParticipationRequest> findByEventIdAndIdIn(Long eventId, List<Long> ids);

    Optional<ParticipationRequest> findByIdAndRequesterId(Long id, Long requesterId);

    boolean existsByEventIdAndRequesterId(Long eventId, Long requesterId);

    long countByEventIdAndStatus(Long eventId, RequestStatus status);

    @Query("select r.event.id as eventId, count(r.id) as confirmed from ParticipationRequest r "
            + "where r.event.id in :eventIds and r.status = :status "
            + "group by r.event.id")
    List<ConfirmedRequestCount> countConfirmedForEvents(@Param("eventIds") List<Long> eventIds,
                                                        @Param("status") RequestStatus status);
}
