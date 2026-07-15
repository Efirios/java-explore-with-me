package ru.practicum.stats.server.service;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.server.exception.ValidationException;
import ru.practicum.stats.server.mapper.StatsMapper;
import ru.practicum.stats.server.repository.StatsRepository;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final StatsRepository repository;

    @Override
    @Transactional
    public void hit(EndpointHitDto endpointHitDto) {
        repository.save(StatsMapper.toEndpointHit(endpointHitDto));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        if (start == null || end == null || start.isAfter(end)) {
            throw new ValidationException("Start of the range must not be after its end.");
        }
        boolean hasUris = uris != null && !uris.isEmpty();
        if (unique) {
            return hasUris
                    ? repository.getUniqueStatsByUris(start, end, uris)
                    : repository.getUniqueStats(start, end);
        }
        return hasUris
                ? repository.getStatsByUris(start, end, uris)
                : repository.getStats(start, end);
    }
}
