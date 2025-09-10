package com.team.gate.core.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class KongStatsController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String kongAdminUrl = "http://localhost:8001"; // adjust for your Kong Admin API

    @GetMapping("/routes-by-service")
    public Map<String, Integer> getRoutesByService() {
        Map<String, Integer> result = new HashMap<>();

// 1. fetch services
        JsonNode services = restTemplate.getForObject(kongAdminUrl + "/services", JsonNode.class);
        if (services == null || !services.has("data")) {
            return result;
        }

// 2. loop through each service and count routes
        Iterator<JsonNode> it = services.get("data").elements();
        while (it.hasNext()) {
            JsonNode service = it.next();
            String serviceName = service.get("name").asText();

            JsonNode routes = restTemplate.getForObject(
                    kongAdminUrl + "/services/" + serviceName + "/routes",
                    JsonNode.class
            );

            int count = (routes != null && routes.has("data"))
                    ? routes.get("data").size()
                    : 0;

            result.put(serviceName, count);
        }
        return result;
    }
}
