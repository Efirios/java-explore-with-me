package ru.practicum.ewm.location;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.location.dto.LocationDto;

@RestController
@RequestMapping("/locations")
@RequiredArgsConstructor
@Validated
public class LocationPublicController {

    private final LocationService locationService;

    @GetMapping
    public List<LocationDto> getLocations(
            @RequestParam(defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(defaultValue = "10") @Positive int size) {
        return locationService.getLocations(from, size);
    }

    @GetMapping("/{locId}")
    public LocationDto getLocation(@PathVariable Long locId) {
        return locationService.getLocation(locId);
    }

    @GetMapping("/{locId}/events")
    public List<EventShortDto> getEventsInLocation(@PathVariable Long locId) {
        return locationService.getEventsInLocation(locId);
    }
}
