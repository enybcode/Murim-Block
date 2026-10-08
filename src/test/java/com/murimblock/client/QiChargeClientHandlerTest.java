package com.murimblock.client;

import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QiChargeClientHandlerTest {
    @Test
    void logoutClearsPendingChargeAndFovEvenWithoutAPlayer() throws ReflectiveOperationException {
        setState(QiChargeClientHandler.class, "lastSentCharging", true);
        setState(QiChargeClientHandler.class, "visualCharging", true);
        setState(QiChargeFovHandler.class, "transition", 1.0F);
        QiChargeClientHandler.onLogout(new ClientPlayerNetworkEvent.LoggingOut(null, null, null));
        assertFalse(QiChargeClientHandler.isVisualCharging());
        assertEquals(false, readState(QiChargeClientHandler.class, "lastSentCharging"));
        assertEquals(0.0F, readState(QiChargeFovHandler.class, "transition"));
    }

    private static void setState(Class<?> type, String name, Object value) throws ReflectiveOperationException {
        var field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }

    private static Object readState(Class<?> type, String name) throws ReflectiveOperationException {
        var field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(null);
    }
}
