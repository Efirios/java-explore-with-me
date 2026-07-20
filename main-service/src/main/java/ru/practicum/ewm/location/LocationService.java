package ru.practicum.ewm.location;

import java.util.List;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.location.dto.LocationDto;
import ru.practicum.ewm.location.dto.NewLocationDto;
import ru.practicum.ewm.location.dto.UpdateLocationDto;

public interface LocationService {

    LocationDto addLocation(NewLocationDto dto);

    LocationDto updateLocation(Long locId, UpdateLocationDto dto);

    void deleteLocation(Long locId);

    List<LocationDto> getLocations(int from, int size);

    LocationDto getLocation(Long locId);

    List<EventShortDto> getEventsInLocation(Long locId);
}
