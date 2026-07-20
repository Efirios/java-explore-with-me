package ru.practicum.ewm.location;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.event.Event;
import ru.practicum.ewm.event.EventRepository;
import ru.practicum.ewm.event.EventService;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.location.dto.LocationDto;
import ru.practicum.ewm.location.dto.NewLocationDto;
import ru.practicum.ewm.location.dto.UpdateLocationDto;
import ru.practicum.ewm.util.OffsetPageRequest;

@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;

    private final EventRepository eventRepository;

    private final EventService eventService;

    @Override
    @Transactional
    public LocationDto addLocation(NewLocationDto dto) {
        Location location = locationRepository.save(LocationMapper.toLocation(dto));
        return LocationMapper.toDto(location);
    }

    @Override
    @Transactional
    public LocationDto updateLocation(Long locId, UpdateLocationDto dto) {
        Location location = getLocationOrThrow(locId);
        if (dto.getName() != null) {
            location.setName(dto.getName());
        }
        if (dto.getLat() != null) {
            location.setLat(dto.getLat());
        }
        if (dto.getLon() != null) {
            location.setLon(dto.getLon());
        }
        if (dto.getRadius() != null) {
            location.setRadius(dto.getRadius());
        }
        return LocationMapper.toDto(locationRepository.save(location));
    }

    @Override
    @Transactional
    public void deleteLocation(Long locId) {
        getLocationOrThrow(locId);
        locationRepository.deleteById(locId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocationDto> getLocations(int from, int size) {
        Pageable pageable = OffsetPageRequest.of(from, size, Sort.by("id"));
        return locationRepository.findAll(pageable).stream()
                .map(LocationMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LocationDto getLocation(Long locId) {
        return LocationMapper.toDto(getLocationOrThrow(locId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getEventsInLocation(Long locId) {
        Location location = getLocationOrThrow(locId);
        List<Event> events = eventRepository.findPublishedInArea(
                location.getLat(), location.getLon(), location.getRadius());
        return eventService.toShortDtos(events);
    }

    private Location getLocationOrThrow(Long locId) {
        return locationRepository.findById(locId)
                .orElseThrow(() -> new NotFoundException("Location with id=" + locId + " was not found"));
    }
}
