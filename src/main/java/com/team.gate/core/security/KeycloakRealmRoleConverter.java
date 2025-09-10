package com.team.gate.core.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;
import java.util.stream.Collectors;

public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    private final String clientName;

    public KeycloakRealmRoleConverter(String clientName) {
        this.clientName = clientName;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<String> roles = new HashSet<>();

        Map<String, Object> realmAccess = (Map<String, Object>) jwt.getClaims().get("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof Collection<?> rr) {
            rr.forEach(r -> roles.add(String.valueOf(r)));
        }

        Map<String, Object> resourceAccess = (Map<String, Object>) jwt.getClaims().get("resource_access");
        if (resourceAccess != null) {
            Map<String, Object> client = (Map<String, Object>) resourceAccess.get(clientName);
            if (client != null && client.get("roles") instanceof Collection<?> cr) {
                cr.forEach(r -> roles.add(String.valueOf(r)));
            }
        }

        return roles.stream()
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r.toUpperCase().replace('.', '_'))
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }
}
