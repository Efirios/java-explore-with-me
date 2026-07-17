package ru.practicum.ewm.event;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class EventSpecification {

    private EventSpecification() {
    }

    public static Specification<Event> alwaysTrue() {
        return (root, query, cb) -> cb.conjunction();
    }

    public static Specification<Event> hasState(EventState state) {
        return (root, query, cb) -> cb.equal(root.get("state"), state);
    }

    public static Specification<Event> statesIn(List<EventState> states) {
        return (root, query, cb) -> root.get("state").in(states);
    }

    public static Specification<Event> initiatorsIn(List<Long> users) {
        return (root, query, cb) -> root.get("initiator").get("id").in(users);
    }

    public static Specification<Event> categoriesIn(List<Long> categories) {
        return (root, query, cb) -> root.get("category").get("id").in(categories);
    }

    public static Specification<Event> isPaid(Boolean paid) {
        return (root, query, cb) -> cb.equal(root.get("paid"), paid);
    }

    public static Specification<Event> textContains(String text) {
        String pattern = "%" + text.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("annotation")), pattern),
                cb.like(cb.lower(root.get("description")), pattern));
    }

    public static Specification<Event> eventDateAfter(LocalDateTime dateTime) {
        return (root, query, cb) -> cb.greaterThan(root.get("eventDate"), dateTime);
    }

    public static Specification<Event> eventDateFrom(LocalDateTime dateTime) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("eventDate"), dateTime);
    }

    public static Specification<Event> eventDateTo(LocalDateTime dateTime) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("eventDate"), dateTime);
    }
}
