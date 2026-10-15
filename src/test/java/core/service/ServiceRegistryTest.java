package core.service;

import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.thesplitting.src.core.contracts.IService;
import org.thesplitting.src.core.ServiceRegistry;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class ServiceRegistryTest {
    static class TestService implements IService {
        private final String id;
        private final List<String> log;

        TestService(String id, List<String> log) {
            this.id = id;
            this.log = log;
        }

        @Override
        public void onEnable() {
            log.add("enable-" + id);
        }

        @Override
        public void onDisable() {
            log.add("disable-" + id);
        }
    }

    @Test
    void enableAll_and_disableAll_callServicesInCorrectOrder() throws Exception {
        JavaPlugin plugin = Mockito.mock(JavaPlugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test-logger"));

        ServiceRegistry registry = new ServiceRegistry(plugin);

        Field servicesField = ServiceRegistry.class.getDeclaredField("services");
        servicesField.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<IService> services = (List<IService>) servicesField.get(registry);

        List<String> log = new ArrayList<>();
        TestService s1 = new TestService("A", log);
        TestService s2 = new TestService("B", log);

        services.add(s1);
        services.add(s2);

        registry.enableAll();
        registry.disableAll();

        assertEquals(List.of("enable-A",  "enable-B", "disable-B", "disable-A"), log);
    }

    @Test
    void getService_returns_correctInstanceOrThrows()  throws Exception {
        JavaPlugin plugin = Mockito.mock(JavaPlugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test-logger"));
        ServiceRegistry registry = new ServiceRegistry(plugin);

        Field servicesField = ServiceRegistry.class.getDeclaredField("services");
        servicesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<IService> services = (List<IService>) servicesField.get(registry);

        TestService s1 = new TestService("A", new ArrayList<>());
        services.add(s1);

        TestService resolved = registry.getService(TestService.class);
        assertSame(s1, resolved);

        class OtherWise implements IService {
            @Override
            public void onEnable() {
            }
            @Override
            public void onDisable() {}
        }

        assertThrows(IllegalArgumentException.class, () -> registry.getService(OtherWise.class));
    }
}
