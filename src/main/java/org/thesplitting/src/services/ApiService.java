package org.thesplitting.src.services;

import com.google.gson.Gson;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.thesplitting.src.core.contracts.IService;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class ApiService implements IService {
    private final JavaPlugin plugin;
    private final Gson gson = new Gson();
    private HttpClient httpClient;
    private String baseUrl;
    private String apiKey;

    public ApiService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onEnable() {
        plugin.saveDefaultConfig();
        baseUrl = plugin.getConfig().getString("api.base-url", "http://localhost:8080/");
        apiKey = plugin.getConfig().getString("api.key", "");
        httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @Override
    public void onDisable() {

    }

    public <T> CompletableFuture<T> get(String path, Class<T> responseType) {
        HttpRequest request = baseRequest(path).GET().build();
        return send(request, responseType);
    }

    public <T> CompletableFuture<T> post(String path, Object body, Class<T> responseType) {
        HttpRequest request = baseRequest(path)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();
        return send(request, responseType);
    }

    public void runSync(Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    private HttpRequest.Builder baseRequest(String path) {
        return HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .header("X-Api-Key", apiKey);
    }

    private <T> CompletableFuture<T> send(HttpRequest request, Class<T> responseType) {
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                   if (response.statusCode() / 100 != 2) {
                       throw new IllegalStateException("API " + response.statusCode() + ": " + response.body());
                   }
                   return responseType == Void.class ? null : gson.fromJson(response.body(), responseType);
                });
    }
}
