package com.sprint.otboo.weather.integration.kma.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.stream.Collectors;

final class UrlMasker {
    private UrlMasker() {}

    static String maskAuthKey(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) return rawUrl;
        try {
            URI uri = new URI(rawUrl);
            String query = uri.getRawQuery();
            if (query == null) return rawUrl;

            String masked = Arrays.stream(query.split("&"))
                .map(kv -> {
                    int idx = kv.indexOf('=');
                    if (idx < 0) return kv;
                    String k = kv.substring(0, idx);
                    String v = kv.substring(idx + 1);
                    if ("authKey".equalsIgnoreCase(k) || "serviceKey".equalsIgnoreCase(k)) {
                        return k + "=" + maskValue(v);
                    }
                    return kv;
                })
                .collect(Collectors.joining("&"));

            return new URI(
                uri.getScheme(), uri.getAuthority(), uri.getPath(), masked, uri.getFragment()
            ).toString();
        } catch (URISyntaxException e) {

            return rawUrl
                .replaceAll("(authKey|serviceKey)=[^&]+", "$1=****");
        }
    }

    private static String maskValue(String v) {
        if (v == null || v.isBlank()) return "****";
        int n = v.length();
        return (n <= 4) ? "****" : "****" + v.substring(n - 4);
    }
}
