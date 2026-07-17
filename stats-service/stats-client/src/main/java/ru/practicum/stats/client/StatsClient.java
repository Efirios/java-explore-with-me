package ru.practicum.stats.client;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.practicum.stats.dto.Constants;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;

@Service
public class StatsClient {

    private final RestTemplate rest;

    public StatsClient(@Value("${stats-server.url}") String serverUrl, RestTemplateBuilder builder) {
        this.rest = builder
                .rootUri(serverUrl)
                .build();
    }

    public void hit(EndpointHitDto endpointHitDto) {
        rest.postForEntity("/hit", endpointHitDto, Void.class);
    }

    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("start", start.format(Constants.FORMATTER));
        parameters.put("end", end.format(Constants.FORMATTER));
        parameters.put("unique", unique);

        StringBuilder path = new StringBuilder("/stats?start={start}&end={end}&unique={unique}");
        if (uris != null && !uris.isEmpty()) {
            parameters.put("uris", String.join(",", uris));
            path.append("&uris={uris}");
        }

        ResponseEntity<ViewStatsDto[]> response =
                rest.getForEntity(path.toString(), ViewStatsDto[].class, parameters);
        ViewStatsDto[] body = response.getBody();
        return body == null ? List.of() : List.of(body);
    }
}
