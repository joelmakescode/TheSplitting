package org.thesplitting.src.core;

import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thesplitting.src.core.contracts.IService;

import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceRegistryTest {

    static class FirstService implements IService {
        @Override
        public void onEnable() {}

        @Override
        public void onDisable() {}
    }

    static class SecondService implements IService {
        @Override
        public void onEnable() {}

        @Override
        public void onDisable() {}
    }

    record DependentService(FirstService dependency) implements IService {
        @Override
        public void onEnable() {}

        @Override
        public void onDisable() {}
    }

    @Mock
    private JavaPlugin plugin;

    private ServiceRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ServiceRegistry(plugin);
    }

    @Nested
    class Lifecycle {

        @Mock
        private Logger logger;

        @Mock
        private IService first;

        @Mock
        private IService second;

        @Mock
        private IService third;

        @BeforeEach
        void setUp() {
            when(plugin.getLogger()).thenReturn(logger);
            registry.registerAll(List.of(r -> first, r -> second, r -> third));
        }

        @Test
        void enableAll_callsOnEnableInRegistrationOrder() {
            registry.enableAll();

            InOrder inOrder = inOrder(first, second, third);
            inOrder.verify(first).onEnable();
            inOrder.verify(second).onEnable();
            inOrder.verify(third).onEnable();
        }

        @Test
        void disableAll_callsOnDisableInReverseOrder() {
            registry.disableAll();

            InOrder inOrder = inOrder(first, second, third);
            inOrder.verify(third).onDisable();
            inOrder.verify(second).onDisable();
            inOrder.verify(first).onDisable();
        }

        @Test
        void enableAll_failingService_logsErrorAndStartsRemainingServices() {
            doThrow(new RuntimeException("test failure")).when(second).onEnable();

            registry.enableAll();

            verify(third).onEnable();
            verify(logger).severe(startsWith("Failed to start service"));
        }

        @Test
        void disableAll_failingService_logsErrorAndStopsRemainingServices() {
            doThrow(new RuntimeException("test failure")).when(second).onDisable();

            registry.disableAll();

            verify(first).onDisable();
            verify(logger).severe(startsWith("Failed to stop service"));
        }
    }

    @Nested
    class Lookup {

        @Test
        void getService_registeredType_returnsSameInstance() {
            FirstService first = new FirstService();
            registry.registerAll(List.of(r -> first, r -> new SecondService()));

            assertSame(first, registry.getService(FirstService.class));
        }

        @Test
        void getService_unregisteredType_throwsIllegalArgumentException() {
            registry.registerAll(List.of(r -> new FirstService()));

            assertThrows(IllegalArgumentException.class, () -> registry.getService(SecondService.class));
        }

        @Test
        void registerAll_factoryUsesEarlierService_receivesRegisteredInstance() {
            registry.registerAll(List.of(
                    r -> new FirstService(),
                    r -> new DependentService(r.getService(FirstService.class))
            ));

            assertSame(registry.getService(FirstService.class), registry.getService(DependentService.class).dependency());
        }

        @Test
        void registerAll_realServiceConfig_resolvesEveryDependency() {
            assertDoesNotThrow(() -> registry.registerAll());
        }
    }
}
