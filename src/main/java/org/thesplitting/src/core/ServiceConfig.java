package org.thesplitting.src.core;

import org.thesplitting.src.services.*;
import org.thesplitting.src.services.fileservice.ChatControlFileService;
import org.thesplitting.src.services.itemservice.ItemListenerService;
import org.thesplitting.src.misc.errorhandler.PlayerErrorHandler;
import org.thesplitting.src.events.ItemListener;
import org.thesplitting.src.events.PlayerListener;
import org.thesplitting.src.services.itemservice.ItemService;
import org.thesplitting.src.core.contracts.IService;
import org.thesplitting.src.services.fileservice.PlayerFileService;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

public class ServiceConfig {
    public static List<Function<ServiceRegistry, IService>> getServices() {
        return List.of(
                registry -> new ApiService(registry.getPlugin()),

                ItemService::new,
                registry -> new PlayerErrorHandler(registry.getPlugin()),
                registry -> new WorldService(registry.getPlugin()),

                registry -> new ChatControlFileService(registry, Path.of("chat-control")),
                registry -> new PlayerFileService(registry, Path.of("players")),

                registry -> new PlayerService(
                        registry.getService(PlayerFileService.class),
                        registry.getService(ItemService.class)
                ),
                registry -> new ChatControlService(
                        registry.getPlugin(),
                        registry.getService(ChatControlFileService.class),
                        registry.getService(PlayerService.class)
                ),

                registry -> new InventoryService(
                        registry.getService(ItemService.class),
                        registry.getService(PlayerService.class)
                ),
                registry -> new ScoreboardService(registry.getService(PlayerService.class)),
                registry -> new PlayerListener(
                        registry.getPlugin(),
                        registry.getService(PlayerService.class),
                        registry.getService(ChatControlService.class),
                        registry.getService(InventoryService.class),
                        registry.getService(ScoreboardService.class),
                        registry.getService(PlayerErrorHandler.class),
                        registry.getService(ApiService.class)
                ),

                registry -> new ItemListenerService(registry.getService(InventoryService.class)),
                registry -> new ItemListener(registry.getPlugin(), registry.getService(ItemListenerService.class))
        );
    }
}
