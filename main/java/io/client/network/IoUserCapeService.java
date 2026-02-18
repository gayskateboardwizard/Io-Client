package io.client.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import io.client.managers.ModuleManager;
import io.client.modules.settings.CapeSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;

public final class IoUserCapeService {
    public static final Identifier IO_CAPE_TEXTURE = Identifier.of("io_client", "textures/io_cape.png");
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static final Set<String> ACTIVE_USERS = ConcurrentHashMap.newKeySet();
    private static final long POLL_INTERVAL_MS = 15_000L;
    private static final String WORKER_URL = System.getProperty(
            "ioClientCapeWorkerUrl",
            "https://io-capes.typhfun.workers.dev/io-users");

    private static volatile long lastPollMs = 0L;
    private static volatile boolean requestInFlight = false;

    private IoUserCapeService() {
    }

    public static void onClientTick(MinecraftClient client) {
        if (client == null || client.player == null || requestInFlight || !isConfigured())
            return;
        if (!isCapeFeatureEnabled()) {
            ACTIVE_USERS.clear();
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastPollMs < POLL_INTERVAL_MS)
            return;
        lastPollMs = now;
        requestInFlight = true;

        String username = client.player.getGameProfile().getName();
        String payloadName = shouldSendAnonymousPing() ? "." : username;
        String encoded = URLEncoder.encode(payloadName, StandardCharsets.UTF_8);
        String requestUrl = normalizeWorkerUrl(WORKER_URL) + "?username=" + encoded;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(requestUrl))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        updateActiveUsers(response.body());
                    }
                })
                .exceptionally(throwable -> null)
                .whenComplete((value, throwable) -> requestInFlight = false);
    }

    public static boolean shouldUseIoCape(String username) {
        if (!isCapeFeatureEnabled())
            return false;
        if (username == null || username.isBlank())
            return false;
        return ACTIVE_USERS.contains(username.toLowerCase());
    }

    public static SkinTextures withIoCapeIfEligible(String username, SkinTextures original) {
        if (original == null || !shouldUseIoCape(username))
            return original;
        return new SkinTextures(
                original.texture(),
                original.textureUrl(),
                IO_CAPE_TEXTURE,
                original.elytraTexture(),
                original.model(),
                original.secure());
    }

    private static boolean isConfigured() {
        return !WORKER_URL.contains("YOUR_WORKER_SUBDOMAIN");
    }

    private static boolean isCapeFeatureEnabled() {
        CapeSettings settings = ModuleManager.INSTANCE.getModule(CapeSettings.class);
        return settings == null || settings.enabled.isEnabled();
    }

    private static boolean shouldSendAnonymousPing() {
        CapeSettings settings = ModuleManager.INSTANCE.getModule(CapeSettings.class);
        return settings != null && settings.anonymousPing.isEnabled();
    }

    private static String normalizeWorkerUrl(String url) {
        int protocolIndex = url.indexOf("://");
        if (protocolIndex < 0)
            return url;
        int pathStart = url.indexOf('/', protocolIndex + 3);
        if (pathStart < 0)
            return url;
        String head = url.substring(0, pathStart);
        String path = url.substring(pathStart).replaceAll("/{2,}", "/");
        return head + path;
    }

    private static void updateActiveUsers(String jsonBody) {
        try {
            JsonObject root = JsonParser.parseString(jsonBody).getAsJsonObject();
            JsonArray usersArray = root.getAsJsonArray("users");
            if (usersArray == null)
                return;

            Set<String> latest = ConcurrentHashMap.newKeySet();
            for (JsonElement element : usersArray) {
                String name = element.getAsString();
                if (name != null && !name.isBlank()) {
                    latest.add(name.toLowerCase());
                }
            }
            ACTIVE_USERS.clear();
            ACTIVE_USERS.addAll(latest);
        } catch (Exception ignored) {
        }
    }
}



