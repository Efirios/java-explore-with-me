package ru.practicum.stats.server.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.server.model.EndpointHit;

public interface StatsRepository extends JpaRepository<EndpointHit, Long> {

    @Query("select new ru.practicum.stats.dto.ViewStatsDto(h.app, h.uri, count(h.ip)) "
            + "from EndpointHit h "
            + "where h.timestamp between :start and :end "
            + "group by h.app, h.uri "
            + "order by count(h.ip) desc")
    List<ViewStatsDto> getStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("select new ru.practicum.stats.dto.ViewStatsDto(h.app, h.uri, count(h.ip)) "
            + "from EndpointHit h "
            + "where h.timestamp between :start and :end and h.uri in :uris "
            + "group by h.app, h.uri "
            + "order by count(h.ip) desc")
    List<ViewStatsDto> getStatsByUris(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end,
                                      @Param("uris") List<String> uris);

    @Query("select new ru.practicum.stats.dto.ViewStatsDto(h.app, h.uri, count(distinct h.ip)) "
            + "from EndpointHit h "
            + "where h.timestamp between :start and :end "
            + "group by h.app, h.uri "
            + "order by count(distinct h.ip) desc")
    List<ViewStatsDto> getUniqueStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("select new ru.practicum.stats.dto.ViewStatsDto(h.app, h.uri, count(distinct h.ip)) "
            + "from EndpointHit h "
            + "where h.timestamp between :start and :end and h.uri in :uris "
            + "group by h.app, h.uri "
            + "order by count(distinct h.ip) desc")
    List<ViewStatsDto> getUniqueStatsByUris(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end,
                                            @Param("uris") List<String> uris);
}
