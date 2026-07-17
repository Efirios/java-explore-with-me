package ru.practicum.ewm.event;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDateTime;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;
import ru.practicum.ewm.request.ParticipationRequest;
import ru.practicum.ewm.request.RequestStatus;

@UtilityClass
public class EventSpecification {

    public Specification<Event> alwaysTrue() {
        return (root, query, cb) -> cb.conjunction();
    }

    public Specification<Event> hasState(EventState state) {
        return (root, query, cb) -> cb.equal(root.get("state"), state);
    }

    public Specification<Event> statesIn(List<EventState> states) {
        return (root, query, cb) -> root.get("state").in(states);
    }

    public Specification<Event> initiatorsIn(List<Long> users) {
        return (root, query, cb) -> root.get("initiator").get("id").in(users);
    }

    public Specification<Event> categoriesIn(List<Long> categories) {
        return (root, query, cb) -> root.get("category").get("id").in(categories);
    }

    public Specification<Event> isPaid(Boolean paid) {
        return (root, query, cb) -> cb.equal(root.get("paid"), paid);
    }

    public Specification<Event> textContains(String text) {
        String pattern = "%" + text.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("annotation")), pattern),
                cb.like(cb.lower(root.get("description")), pattern));
    }

    public Specification<Event> eventDateAfter(LocalDateTime dateTime) {
        return (root, query, cb) -> cb.greaterThan(root.get("eventDate"), dateTime);
    }

    public Specification<Event> eventDateFrom(LocalDateTime dateTime) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("eventDate"), dateTime);
    }

    public Specification<Event> eventDateTo(LocalDateTime dateTime) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("eventDate"), dateTime);
    }

    public Specification<Event> onlyAvailable() {
        return (root, query, cb) -> {
            Subquery<Long> confirmed = query.subquery(Long.class);
            Root<ParticipationRequest> request = confirmed.from(ParticipationRequest.class);
            confirmed.select(cb.count(request));
            confirmed.where(
                    cb.equal(request.get("event"), root),
                    cb.equal(request.get("status"), RequestStatus.CONFIRMED));
            return cb.or(
                    cb.equal(root.get("participantLimit"), 0),
                    cb.gt(root.<Integer>get("participantLimit"), confirmed));
        };
    }
}
