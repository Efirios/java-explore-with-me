package ru.practicum.ewm.location;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.location.dto.LocationDto;
import ru.practicum.ewm.location.dto.NewLocationDto;
import ru.practicum.ewm.location.dto.UpdateLocationDto;

@RestController
@RequestMapping("/admin/locations")
@RequiredArgsConstructor
@Validated
public class LocationAdminController {

    private final LocationService locationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LocationDto addLocation(@Valid @RequestBody NewLocationDto dto) {
        return locationService.addLocation(dto);
    }

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

    @PatchMapping("/{locId}")
    public LocationDto updateLocation(@PathVariable Long locId, @Valid @RequestBody UpdateLocationDto dto) {
        return locationService.updateLocation(locId, dto);
    }

    @DeleteMapping("/{locId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLocation(@PathVariable Long locId) {
        locationService.deleteLocation(locId);
    }
}
