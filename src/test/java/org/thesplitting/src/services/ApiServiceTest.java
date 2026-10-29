package org.thesplitting.src.services;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiServiceTest {

    record UserResponse(String name, int level) {}

    record JoinRequest(String uuid, String name) {}

    @Mock
    private JavaPlugin plugin;

    @Mock
    private FileConfiguration config;

    private HttpServer server;
    private ApiService apiService;
    private final AtomicReference<HttpExchange> lastExchange = new AtomicReference<>();
    private final AtomicReference<String> lastBody = new AtomicReference<>();
    private volatile int responseStatus = 200;
    private volatile String responseBody = "";

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            lastExchange.set(exchange);
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(responseStatus, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(bytes);
                }
            }
            exchange.close();
        });
        server.start();

        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        when(plugin.getConfig()).thenReturn(config);
        when(config.getString("api.base-url", "http://localhost:8080/")).thenReturn(baseUrl);
        when(config.getString("api.key", "")).thenReturn("secret-key");

        apiService = new ApiService(plugin);
        apiService.onEnable();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private <T> T await(CompletableFuture<T> future) throws Exception {
        return future.get(5, TimeUnit.SECONDS);
    }

    @Test
    void onEnable_savesDefaultConfig() {
        verify(plugin).saveDefaultConfig();
    }

    @Test
    void get_successfulResponse_isParsedIntoResponseType() throws Exception {
        responseBody = "{\"name\":\"Steve\",\"level\":3}";

        UserResponse response = await(apiService.get("/users/steve", UserResponse.class));

        assertEquals(new UserResponse("Steve", 3), response);
        assertEquals("GET", lastExchange.get().getRequestMethod());
        assertEquals("/users/steve", lastExchange.get().getRequestURI().getPath());
    }

    @Test
    void request_containsApiKeyAndAcceptHeader() throws Exception {
        responseBody = "{\"name\":\"Steve\",\"level\":3}";

        await(apiService.get("/users/steve", UserResponse.class));

        assertEquals("secret-key", lastExchange.get().getRequestHeaders().getFirst("X-Api-Key"));
        assertEquals("application/json", lastExchange.get().getRequestHeaders().getFirst("Accept"));
    }

    @Test
    void post_sendsBodyAsJson() throws Exception {
        responseBody = "{\"name\":\"Steve\",\"level\":1}";

        await(apiService.post("/minecraft/users/join", new JoinRequest("1234", "Steve"), UserResponse.class));

        assertEquals("POST", lastExchange.get().getRequestMethod());
        assertEquals("application/json", lastExchange.get().getRequestHeaders().getFirst("Content-Type"));
        assertEquals("{\"uuid\":\"1234\",\"name\":\"Steve\"}", lastBody.get());
    }

    @Test
    void errorStatus_failsWithStatusAndBody() {
        responseStatus = 500;
        responseBody = "server exploded";

        ExecutionException exception = assertThrows(ExecutionException.class,
                () -> await(apiService.get("/users/steve", UserResponse.class)));

        assertInstanceOf(IllegalStateException.class, exception.getCause());
        assertEquals("API 500: server exploded", exception.getCause().getMessage());
    }

    @Test
    void voidResponseType_completesWithNull() throws Exception {
        responseStatus = 204;

        assertNull(await(apiService.post("/minecraft/users/leave", new JoinRequest("1234", "Steve"), Void.class)));
    }

    @Test
    void runSync_schedulesTaskOnMainThread() {
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        Runnable task = () -> {};

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);

            apiService.runSync(task);
        }

        verify(scheduler).runTask(plugin, task);
    }
}
