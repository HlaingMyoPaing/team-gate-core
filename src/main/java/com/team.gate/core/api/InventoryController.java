package com.team.gate.core.api;

import com.team.gate.core.kong.KongService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class InventoryController {

    private final KongService kong;

    public InventoryController(KongService kong) {
        this.kong = kong;
    }

    @GetMapping("/inventory")
    public Mono<Map<String, Integer>> inventory() {
        return kong.entityCounts();
    }

    @GetMapping("/services")
    public Mono<List<?>> services() {
        return kong.servicesWithRouteCounts().map(list -> list);
    }

    @GetMapping("/upstreams")
    public Mono<List<?>> upstreams() {
        return kong.upstreamsWithHealth().map(list -> list);
    }

    @GetMapping("/upstreams/{name}/targets")
    public Flux<?> targets(@PathVariable String name) {
        return kong.targetsForUpstream(name);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/ping")
    public Map<String, String> admin() {
        return Map.of("status", "ok");
    }
}
