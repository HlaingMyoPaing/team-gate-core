package com.team.gate.core.kong;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class KongService {

    private final WebClient kong;
    private final int pageSize;
    private final long cacheTtlSeconds;

    private final Map<String, Object> cache = new HashMap<>();
    private final Map<String, Long> cacheTime = new HashMap<>();

    public record Page(List<Map<String, Object>> data, String next) {
    }

    public record ServiceDto(String id, String name, String host, Integer port, String protocol, long routeCount) {
    }

    public record TargetDto(String target, String weight, String id, String health) {
    }

    public record UpstreamDto(String id, String name, long targetCount, long healthy, long unhealthy, long dnsErrored) {
    }

    public KongService(WebClient kongAdminClient,
                       @Value("${kong.admin.page-size:1000}") int pageSize,
                       @Value("${cache.kong.ttl-seconds:15}") long cacheTtlSeconds) {
        this.kong = kongAdminClient;
        this.pageSize = pageSize;
        this.cacheTtlSeconds = cacheTtlSeconds;
    }

    private <T> Mono<T> cached(String key, Mono<T> supplier) {
        Long t = cacheTime.get(key);
        if (t != null && (Instant.now().getEpochSecond() - t) < cacheTtlSeconds) {
            @SuppressWarnings("unchecked")
            T v = (T) cache.get(key);
            return Mono.just(v);
        }
        return supplier.doOnNext(v -> {
            cache.put(key, v);
            cacheTime.put(key, Instant.now().getEpochSecond());
        });
    }

    private Mono<Page> fetchPage(String url) {
        return kong.get().uri(url).retrieve().bodyToMono(Page.class)
                .map(p -> new Page(p.data() == null ? List.of() : p.data(), p.next()));
    }

    private Flux<Map<String, Object>> listPaged(String path) {
        String firstUrl = path + "?size=" + pageSize;
        return fetchPage(firstUrl)
                .expand(p -> (p.next() == null || p.next().isBlank()) ? Mono.empty() : fetchPage(p.next()))
                .flatMapIterable(Page::data);
    }

    public Mono<Map<String, Integer>> entityCounts() {
        return cached("counts",
                Mono.zip(
                        listPaged("/services").count(),
                        listPaged("/routes").count(),
                        listPaged("/upstreams").count()
                ).map(t3 -> Map.of(
                        "services", t3.getT1().intValue(),
                        "routes", t3.getT2().intValue(),
                        "upstreams", t3.getT3().intValue()
                ))
        );
    }

    public Mono<List<ServiceDto>> servicesWithRouteCounts() {
        return cached("servicesWithRoutes",
                listPaged("/services")
                        .map(s -> {
                            String id = Objects.toString(s.get("id"), null);
                            String name = Objects.toString(s.getOrDefault("name", id), null);
                            String host = Objects.toString(s.get("host"), null);
                            Integer port = s.get("port") instanceof Number n ? n.intValue() : null;
                            String protocol = Objects.toString(s.get("protocol"), null);
                            return new ServiceDto(id, name, host, port, protocol, 0);
                        })
                        .collectList()
                        .flatMap(services ->
                                listPaged("/routes")
                                        .collectMultimap(r -> {
                                            Object svc = r.get("service");
                                            if (svc instanceof Map<?, ?> m) return Objects.toString(m.get("id"), null);
                                            return null;
                                        })
                                        .map(routesBySvc -> services.stream().map(s ->
                                                new ServiceDto(
                                                        s.id(), s.name(), s.host(), s.port(), s.protocol(),
                                                        Optional.ofNullable(routesBySvc.get(s.id())).map(Collection::size).orElse(0)
                                                )
                                        ).collect(Collectors.toList()))
                        )
        );
    }

    public Mono<List<UpstreamDto>> upstreamsWithHealth() {
        return cached("upstreamsWithHealth",
                listPaged("/upstreams")
                        .map(u -> new UpstreamDto(
                                Objects.toString(u.get("id"), null),
                                Objects.toString(u.get("name"), null),
                                0, 0, 0, 0
                        ))
                        .collectList()
                        .flatMap(upstreams ->
                                Flux.fromIterable(upstreams)
                                        .flatMap(u -> targetsForUpstream(u.name())
                                                .collectList()
                                                .map(list -> {
                                                    long healthy = list.stream().filter(t -> "healthy".equalsIgnoreCase(t.health())).count();
                                                    long unhealthy = list.stream().filter(t -> "unhealthy".equalsIgnoreCase(t.health())).count();
                                                    long dnsErr = list.stream().filter(t -> "dns_error".equalsIgnoreCase(t.health())).count();
                                                    return new UpstreamDto(u.id(), u.name(), list.size(), healthy, unhealthy, dnsErr);
                                                })
                                        ).collectList()
                        )
        );
    }

    public Flux<TargetDto> targetsForUpstream(String upstreamName) {
        Mono<Map> health = kong.get()
                .uri("/upstreams/{name}/health", upstreamName)
                .retrieve().bodyToMono(Map.class)
                .onErrorReturn(Map.of("data", List.of()));

        Flux<Map<String, Object>> targets = listPaged("/upstreams/" + upstreamName + "/targets");

        return health.flatMapMany(h -> {
            Map<String, String> healthByTarget = new HashMap<>();
            Object nodes = h.get("data");
            if (nodes instanceof Collection<?> col) {
                for (Object o : col) {
                    if (o instanceof Map<?, ?> m) {
                        String target = Objects.toString(m.get("target"), null);
                        String status = Objects.toString(m.get("health"), null);
                        if (target != null && status != null) healthByTarget.put(target, status);
                    }
                }
            }
            return targets.map(t -> new TargetDto(
                    Objects.toString(t.get("target"), null),
                    Objects.toString(t.get("weight"), null),
                    Objects.toString(t.get("id"), null),
                    healthByTarget.getOrDefault(Objects.toString(t.get("target"), null), "unknown")
            ));
        });
    }
}
