package com.konayuki.statflex.utils.api.Keyless;

import com.google.gson.JsonObject;
import com.konayuki.statflex.utils.Connection;
import com.konayuki.statflex.utils.Debug;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class ProviderUtil {

    private static final long CACHE_TTL_MS = 120_000L;
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 5000;

    private static final Provider[] PROVIDERS = {
            new NadeshikoApi(),
            new AbyssApi(),
            new BordicApi()
    };

    private static final ConcurrentMap<String, CachedPlayer> CACHE = new ConcurrentHashMap<>();
    private static final AtomicInteger LAST_SUCCESS_INDEX = new AtomicInteger(-1);

    private ProviderUtil() {
    }

    public static final class Outcome {
        public final JsonObject player;
        public final String providerName;
        public final Exception error;

        private Outcome(JsonObject player, String providerName, Exception error) {
            this.player = player;
            this.providerName = providerName;
            this.error = error;
        }
    }

    public static Outcome fetch(String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) {
            return new Outcome(null, null, null);
        }

        String normalized = uuid.replace("-", "").trim().toLowerCase();
        int start = LAST_SUCCESS_INDEX.get();

        StringBuilder failures = new StringBuilder();
        for (int offset = 0; offset < PROVIDERS.length; offset++) {
            int index = start >= 0 ? (start + offset) % PROVIDERS.length : offset;
            Provider provider = PROVIDERS[index];

            String cacheKey = index + ":" + normalized;
            CachedPlayer cached = CACHE.get(cacheKey);
            if (cached != null && System.currentTimeMillis() < cached.expiresAt) {
                return new Outcome(cached.player, provider.displayName(), null);
            }

            try {
                JsonObject player = request(provider, normalized);
                if (player == null) {
                    continue;   // reachable, but no data on this provider
                }

                CACHE.put(cacheKey, new CachedPlayer(player, System.currentTimeMillis() + CACHE_TTL_MS));
                LAST_SUCCESS_INDEX.set(index);
                Debug.log("Keyless stats served by " + provider.displayName() + ".");
                return new Outcome(player, provider.displayName(), null);
            } catch (Exception e) {
                String detail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                if (failures.length() > 0) {
                    failures.append("; ");
                }
                failures.append(provider.displayName()).append(": ").append(detail);
            }
        }

        if (failures.length() > 0) {
            return new Outcome(null, null, new Exception(failures.toString()));
        }
        return new Outcome(null, null, null);
    }

    private static JsonObject request(Provider provider, String uuid) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(provider.buildUrl(uuid)).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestProperty("User-Agent", provider.userAgent());
        conn.setRequestProperty("Accept", "application/json");
        Connection.trust(conn);

        int status = conn.getResponseCode();
        if (status < 200 || status >= 300) {
            throw new IOException("HTTP " + status);
        }

        return provider.parsePlayer(readBody(conn));
    }

    private static String readBody(HttpURLConnection conn) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder body = new StringBuilder();
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        } finally {
            reader.close();
        }
        return body.toString();
    }

    private static final class CachedPlayer {
        final JsonObject player;
        final long expiresAt;

        CachedPlayer(JsonObject player, long expiresAt) {
            this.player = player;
            this.expiresAt = expiresAt;
        }
    }
}
