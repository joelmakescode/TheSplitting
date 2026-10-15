package org.thesplitting;
import org.bukkit.plugin.java.JavaPlugin;
import org.thesplitting.src.core.ServiceRegistry;

public class Plugin extends JavaPlugin {
    private ServiceRegistry serviceRegistry;

    @Override
    public void onEnable() {
        getLogger().info("=================================");
        getLogger().info("The Splitting wird geladen...");
        getLogger().info("=================================");

        serviceRegistry = new ServiceRegistry(this);
        serviceRegistry.registerAll();
        serviceRegistry.enableAll();

        getLogger().info("Plugin erfolgreich aktiviert!");
    }

    @Override
    public void onDisable() {
        getLogger().info("The Splitting wird deaktiviert...");

        if (serviceRegistry != null) {
            serviceRegistry.disableAll();
        }

        getLogger().info("Plugin deaktiviert!");
    }
}