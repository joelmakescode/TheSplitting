package org.thesplitting.src.core;

import org.bukkit.plugin.java.JavaPlugin;
import org.thesplitting.src.core.contracts.IService;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ServiceRegistry {
    private final JavaPlugin plugin;
    private final List<IService> services = new ArrayList<>();

    public ServiceRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Registriert alle Services aus Service Config.
     */
    public void registerAll() {
        registerAll(ServiceConfig.getServices());
    }

    void registerAll(List<Function<ServiceRegistry, IService>> factories) {
        for (var factory : factories) {
            services.add(factory.apply(this));
        }
    }

    /**
     * Startet alle Services in Registrierungs-Reihenfolge.
     */
    public void enableAll() {
        for (IService service : services) {
            try {
                service.onEnable();
                plugin.getLogger().info("Service started: " + service.getClass().getSimpleName());
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to start service " + service.getClass().getSimpleName());
                e.printStackTrace();
            }
        }
    }

    /**
     * Stoppt alle Services in invertierter Registrierungs-Reihenfolge.
     */
    public void disableAll() {
        var reversed = new ArrayList<>(services);
        java.util.Collections.reverse(reversed);

        for (IService service : reversed) {
            try {
                service.onDisable();
                plugin.getLogger().info("Service stopped: " + service.getClass().getSimpleName());
            }  catch (Exception e) {
                plugin.getLogger().severe("Failed to stop service " + service.getClass().getSimpleName());
                e.printStackTrace();
            }
        }
    }

    public <T extends IService> T getService(Class<T> serviceClass) {
        for (IService service : services) {
            if (serviceClass.isInstance(service)) {
                return serviceClass.cast(service);
            }
        }

        throw new IllegalArgumentException("Service not found: " + serviceClass.getSimpleName());
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }
}
