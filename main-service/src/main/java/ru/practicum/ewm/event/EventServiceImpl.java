package ru.practicum.ewm.event;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.category.Category;
import ru.practicum.ewm.category.CategoryRepository;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.dto.LocationDto;
import ru.practicum.ewm.event.dto.NewEventDto;
import ru.practicum.ewm.event.dto.UpdateEventAdminRequest;
import ru.practicum.ewm.event.dto.UpdateEventUserRequest;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.ValidationException;
import ru.practicum.ewm.request.ConfirmedRequestCount;
import ru.practicum.ewm.request.RequestRepository;
import ru.practicum.ewm.request.RequestStatus;
import ru.practicum.ewm.user.User;
import ru.practicum.ewm.user.UserRepository;
import ru.practicum.ewm.util.Constants;
import ru.practicum.ewm.util.OffsetPageRequest;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;

    private final UserRepository userRepository;

    private final CategoryRepository categoryRepository;

    private final RequestRepository requestRepository;

    private final StatsClient statsClient;

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {
        getUserOrThrow(userId);
        Pageable pageable = OffsetPageRequest.of(from, size, Sort.by("id"));
        List<Event> events = eventRepository.findByInitiatorId(userId, pageable);
        return toShortDtos(events);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> toShortDtos(List<Event> events) {
        Map<Long, Long> confirmedMap = getConfirmedMap(events);
        Map<Long, Long> viewsMap = getViewsMap(events);
        return events.stream()
                .map(event -> EventMapper.toShortDto(event,
                        confirmedMap.getOrDefault(event.getId(), 0L),
                        viewsMap.getOrDefault(event.getId(), 0L)))
                .toList();
    }

    @Override
    @Transactional
    public EventFullDto addEvent(Long userId, NewEventDto dto) {
        User initiator = getUserOrThrow(userId);
        Category category = getCategoryOrThrow(dto.getCategory());
        if (dto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("Event date must be at least two hours from now");
        }
        Event event = eventRepository.save(EventMapper.toEvent(dto, category, initiator));
        return EventMapper.toFullDto(event, 0L, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getUserEvent(Long userId, Long eventId) {
        getUserOrThrow(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
        return toFullDtoSingle(event);
    }

    @Override
    @Transactional
    public EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest dto) {
        getUserOrThrow(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }
        if (dto.getEventDate() != null) {
            if (dto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
                throw new ValidationException("Event date must be at least two hours from now");
            }
            event.setEventDate(dto.getEventDate());
        }
        if (dto.getStateAction() != null) {
            switch (dto.getStateAction()) {
                case SEND_TO_REVIEW -> event.setState(EventState.PENDING);
                case CANCEL_REVIEW -> event.setState(EventState.CANCELED);
            }
        }
        applyCommonUpdates(event, dto.getAnnotation(), dto.getCategory(), dto.getDescription(), dto.getLocation(),
                dto.getPaid(), dto.getParticipantLimit(), dto.getRequestModeration(), dto.getTitle());
        return toFullDtoSingle(eventRepository.save(event));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventFullDto> getAdminEvents(List<Long> users, List<EventState> states, List<Long> categories,
                                             LocalDateTime rangeStart, LocalDateTime rangeEnd, int from, int size) {
        Specification<Event> spec = EventSpecification.alwaysTrue();
        if (users != null && !users.isEmpty()) {
            spec = spec.and(EventSpecification.initiatorsIn(users));
        }
        if (states != null && !states.isEmpty()) {
            spec = spec.and(EventSpecification.statesIn(states));
        }
        if (categories != null && !categories.isEmpty()) {
            spec = spec.and(EventSpecification.categoriesIn(categories));
        }
        if (rangeStart != null) {
            spec = spec.and(EventSpecification.eventDateFrom(rangeStart));
        }
        if (rangeEnd != null) {
            spec = spec.and(EventSpecification.eventDateTo(rangeEnd));
        }
        Pageable pageable = OffsetPageRequest.of(from, size, Sort.by("id"));
        List<Event> events = eventRepository.findAll(spec, pageable).getContent();
        return toFullDtoList(events);
    }

    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest dto) {
        Event event = getEventOrThrow(eventId);
        LocalDateTime now = LocalDateTime.now();
        if (dto.getEventDate() != null) {
            if (dto.getEventDate().isBefore(now)) {
                throw new ValidationException("Event date must not be in the past");
            }
            if (dto.getEventDate().isBefore(now.plusHours(1))) {
                throw new ConflictException("Event date must be at least one hour after the publication date");
            }
            event.setEventDate(dto.getEventDate());
        }
        if (dto.getStateAction() != null) {
            switch (dto.getStateAction()) {
                case PUBLISH_EVENT -> {
                    if (event.getState() != EventState.PENDING) {
                        throw new ConflictException(
                                "Cannot publish the event because it's not in the right state: " + event.getState());
                    }
                    if (event.getEventDate().isBefore(now.plusHours(1))) {
                        throw new ConflictException(
                                "Event date must be at least one hour after the publication date");
                    }
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(now);
                }
                case REJECT_EVENT -> {
                    if (event.getState() == EventState.PUBLISHED) {
                        throw new ConflictException("Cannot reject the event because it's already published");
                    }
                    event.setState(EventState.CANCELED);
                }
            }
        }
        applyCommonUpdates(event, dto.getAnnotation(), dto.getCategory(), dto.getDescription(), dto.getLocation(),
                dto.getPaid(), dto.getParticipantLimit(), dto.getRequestModeration(), dto.getTitle());
        return toFullDtoSingle(eventRepository.save(event));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getPublicEvents(String text, List<Long> categories, Boolean paid,
                                               LocalDateTime rangeStart, LocalDateTime rangeEnd, boolean onlyAvailable,
                                               EventSort sort, int from, int size, HttpServletRequest request) {
        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new ValidationException("rangeStart must not be after rangeEnd");
        }
        recordHit(request);

        Specification<Event> spec = EventSpecification.hasState(EventState.PUBLISHED);
        if (text != null && !text.isBlank()) {
            spec = spec.and(EventSpecification.textContains(text));
        }
        if (categories != null && !categories.isEmpty()) {
            spec = spec.and(EventSpecification.categoriesIn(categories));
        }
        if (paid != null) {
            spec = spec.and(EventSpecification.isPaid(paid));
        }
        if (rangeStart == null && rangeEnd == null) {
            spec = spec.and(EventSpecification.eventDateAfter(LocalDateTime.now()));
        } else {
            if (rangeStart != null) {
                spec = spec.and(EventSpecification.eventDateFrom(rangeStart));
            }
            if (rangeEnd != null) {
                spec = spec.and(EventSpecification.eventDateTo(rangeEnd));
            }
        }

        List<Event> events = eventRepository.findAll(spec);
        Map<Long, Long> confirmedMap = getConfirmedMap(events);
        Map<Long, Long> viewsMap = getViewsMap(events);

        List<EventShortDto> result = events.stream()
                .filter(event -> !onlyAvailable
                        || isAvailable(event, confirmedMap.getOrDefault(event.getId(), 0L)))
                .map(event -> EventMapper.toShortDto(event,
                        confirmedMap.getOrDefault(event.getId(), 0L),
                        viewsMap.getOrDefault(event.getId(), 0L)))
                .collect(Collectors.toCollection(ArrayList::new));

        if (sort == EventSort.VIEWS) {
            result.sort(Comparator.comparing(EventShortDto::getViews, Comparator.reverseOrder()));
        } else {
            result.sort(Comparator.comparing(EventShortDto::getEventDate));
        }

        return result.stream().skip(from).limit(size).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EventFullDto getPublicEvent(Long id, HttpServletRequest request) {
        Event event = eventRepository.findByIdAndState(id, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Event with id=" + id + " was not found"));
        recordHit(request);
        return toFullDtoSingle(event);
    }

    private void applyCommonUpdates(Event event, String annotation, Long categoryId, String description,
                                    LocationDto location, Boolean paid, Integer participantLimit,
                                    Boolean requestModeration, String title) {
        if (annotation != null) {
            event.setAnnotation(annotation);
        }
        if (categoryId != null) {
            event.setCategory(getCategoryOrThrow(categoryId));
        }
        if (description != null) {
            event.setDescription(description);
        }
        if (location != null) {
            event.setLocation(EventMapper.toLocation(location));
        }
        if (paid != null) {
            event.setPaid(paid);
        }
        if (participantLimit != null) {
            event.setParticipantLimit(participantLimit);
        }
        if (requestModeration != null) {
            event.setRequestModeration(requestModeration);
        }
        if (title != null) {
            event.setTitle(title);
        }
    }

    private boolean isAvailable(Event event, long confirmed) {
        return event.getParticipantLimit() == 0 || confirmed < event.getParticipantLimit();
    }

    private List<EventFullDto> toFullDtoList(List<Event> events) {
        Map<Long, Long> confirmedMap = getConfirmedMap(events);
        Map<Long, Long> viewsMap = getViewsMap(events);
        return events.stream()
                .map(event -> EventMapper.toFullDto(event,
                        confirmedMap.getOrDefault(event.getId(), 0L),
                        viewsMap.getOrDefault(event.getId(), 0L)))
                .toList();
    }

    private EventFullDto toFullDtoSingle(Event event) {
        long confirmed = requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);
        long views = getViewsMap(List.of(event)).getOrDefault(event.getId(), 0L);
        return EventMapper.toFullDto(event, confirmed, views);
    }

    private Map<Long, Long> getConfirmedMap(List<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = events.stream().map(Event::getId).toList();
        Map<Long, Long> confirmed = new HashMap<>();
        for (ConfirmedRequestCount count : requestRepository.countConfirmedForEvents(ids, RequestStatus.CONFIRMED)) {
            confirmed.put(count.getEventId(), count.getConfirmed());
        }
        return confirmed;
    }

    private Map<Long, Long> getViewsMap(List<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }
        List<String> uris = events.stream().map(event -> "/events/" + event.getId()).toList();
        LocalDateTime start = events.stream()
                .map(Event::getCreatedOn)
                .min(Comparator.naturalOrder())
                .orElse(LocalDateTime.now().minusYears(1));
        List<ViewStatsDto> stats = statsClient.getStats(start, LocalDateTime.now(), uris, true);
        Map<Long, Long> views = new HashMap<>();
        for (ViewStatsDto stat : stats) {
            Long eventId = extractEventId(stat.getUri());
            if (eventId != null) {
                views.put(eventId, stat.getHits());
            }
        }
        return views;
    }

    private Long extractEventId(String uri) {
        try {
            return Long.parseLong(uri.substring(uri.lastIndexOf('/') + 1));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void recordHit(HttpServletRequest request) {
        statsClient.hit(EndpointHitDto.builder()
                .app(Constants.APP_NAME)
                .uri(request.getRequestURI())
                .ip(request.getRemoteAddr())
                .timestamp(LocalDateTime.now())
                .build());
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
    }

    private Category getCategoryOrThrow(Long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category with id=" + catId + " was not found"));
    }

    private Event getEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
    }
}
