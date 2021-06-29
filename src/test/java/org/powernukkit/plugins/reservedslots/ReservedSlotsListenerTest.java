package org.powernukkit.plugins.reservedslots;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.event.player.PlayerKickEvent;
import cn.nukkit.event.player.PlayerKickEvent.Reason;
import cn.nukkit.event.player.PlayerPreLoginEvent;
import cn.nukkit.event.server.QueryRegenerateEvent;
import cn.nukkit.plugin.PluginManager;
import cn.nukkit.utils.Config;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author joserobjr
 * @since 2021-06-28
 */
@ExtendWith(MockitoExtension.class)
class ReservedSlotsListenerTest {
    @Mock
    PluginManager pluginManager;
    
    @Mock
    Server server;
    
    @Mock
    Player player;

    Config config;
    ResourceBundle messages;
    ReservedSlotsListener listener;

    @BeforeEach
    void setUp() {
        lenient().when(server.getConfig(anyString(), any(Boolean.class))).thenReturn(false);
        lenient().when(server.getPluginManager()).thenReturn(pluginManager);
        lenient().when(pluginManager.getPlugins()).thenReturn(Collections.emptyMap());
        lenient().when(server.getOnlinePlayers()).thenReturn(Collections.emptyMap());
        lenient().when(player.getServer()).thenReturn(server);
        config = new Config();
        messages = ResourceBundle.getBundle(
                "org.powernukkit.plugins.reservedslots.reserved_slots_messages",
                Locale.ENGLISH
        );
        listener = new ReservedSlotsListener(config, messages);
    }

    @Test
    void onQueryRegenerateEventNoIncrease() {
        QueryRegenerateEvent event = new QueryRegenerateEvent(server);
        event.setMaxPlayerCount(10);
        listener.onQueryRegenerateEvent(event);
        assertEquals(10, event.getMaxPlayerCount());
    }

    @Test
    void onQueryRegenerateEventWouldIncreaseButIsDisabled() {
        QueryRegenerateEvent event = new QueryRegenerateEvent(server);
        event.setPlayerCount(10);
        event.setMaxPlayerCount(10);
        config.set("change-ping-packet", "false");
        listener.onQueryRegenerateEvent(event);
        assertEquals(10, event.getMaxPlayerCount());
    }

    @Test
    void onQueryRegenerateEventShouldIncrease() {
        QueryRegenerateEvent event = new QueryRegenerateEvent(server);
        event.setPlayerCount(10);
        event.setMaxPlayerCount(10);
        listener.onQueryRegenerateEvent(event);
        assertEquals(11, event.getMaxPlayerCount());

        config.set("change-ping-packet", "default");
        event.setMaxPlayerCount(10);
        listener.onQueryRegenerateEvent(event);
        assertEquals(11, event.getMaxPlayerCount());
    }

    @Test
    void onPlayerKickEventNotByServerFull() {
        PlayerKickEvent event = new PlayerKickEvent(player, Reason.KICKED_BY_ADMIN, "");
        listener.onPlayerKickEvent(event);
        assertFalse(event.isCancelled());
    }

    @Test
    void onPlayerKickEventNotAffected() {
        when(player.hasPermission(ReservedSlotsListener.PERM_NOT_AFFECTED)).thenReturn(true);
        PlayerKickEvent event = new PlayerKickEvent(player, Reason.SERVER_FULL, "");
        listener.onPlayerKickEvent(event);
        assertFalse(event.isCancelled());
    }

    @Test
    void onPlayerKickEventCantJoinFull() {
        when(player.hasPermission(ReservedSlotsListener.PERM_NOT_AFFECTED)).thenReturn(false);
        when(player.hasPermission(ReservedSlotsListener.PERM_JOIN_FULL)).thenReturn(false);
        PlayerKickEvent event = new PlayerKickEvent(player, Reason.SERVER_FULL, "");
        listener.onPlayerKickEvent(event);
        assertFalse(event.isCancelled());
    }

    @Test
    void onPlayerKickEventAllow() {
        when(player.hasPermission(ReservedSlotsListener.PERM_NOT_AFFECTED)).thenReturn(false);
        when(player.hasPermission(ReservedSlotsListener.PERM_JOIN_FULL)).thenReturn(true);
        PlayerKickEvent event = new PlayerKickEvent(player, Reason.SERVER_FULL, "");
        listener.onPlayerKickEvent(event);
        assertTrue(event.isCancelled());
    }

    @Test
    void onAsyncPreLoginNotAffected() {
        when(player.hasPermission(ReservedSlotsListener.PERM_NOT_AFFECTED)).thenReturn(true);
        PlayerPreLoginEvent event = new PlayerPreLoginEvent(player, "");
        listener.onAsyncPreLogin(event);
        assertFalse(event.isCancelled());
    }

    @Test
    void onAsyncPreLoginCanJoinFull() {
        when(player.hasPermission(ReservedSlotsListener.PERM_NOT_AFFECTED)).thenReturn(false);
        when(player.hasPermission(ReservedSlotsListener.PERM_JOIN_FULL)).thenReturn(true);
        PlayerPreLoginEvent event = new PlayerPreLoginEvent(player, "");
        listener.onAsyncPreLogin(event);
        assertFalse(event.isCancelled());
    }

    @Test
    void onAsyncPreLoginFullServer() {
        Map<UUID, Player> onlinePlayers = new HashMap<>();
        onlinePlayers.put(UUID.randomUUID(), mock(Player.class));
        onlinePlayers.put(UUID.randomUUID(), mock(Player.class));
        
        when(server.getMaxPlayers()).thenReturn(1);
        when(server.getOnlinePlayers()).thenReturn(onlinePlayers);
        
        PlayerPreLoginEvent event = new PlayerPreLoginEvent(player, "");
        listener.onAsyncPreLogin(event);
        assertTrue(event.isCancelled());
        assertEquals(messages.getString("reservedslots.reserved"), event.getKickMessage());
        
        String expected = "Message 5";
        config.set("custom-messages.5", expected);
        listener.onAsyncPreLogin(event);
        assertEquals(expected, event.getKickMessage());

        expected = "Message 3";
        config.set("custom-messages.3", expected);
        listener.onAsyncPreLogin(event);
        assertEquals(expected, event.getKickMessage());

        config.set("custom-messages.8", "Message 8 (not expected)");
        listener.onAsyncPreLogin(event);
        assertEquals(expected, event.getKickMessage());

        expected = "Message 0";
        config.set("custom-messages.0", expected);
        listener.onAsyncPreLogin(event);
        assertEquals(expected, event.getKickMessage());
    }

    @Test
    void onAsyncPreLoginReserved() {
        Map<UUID, Player> onlinePlayers = new HashMap<>();
        onlinePlayers.put(UUID.randomUUID(), mock(Player.class));
        onlinePlayers.put(UUID.randomUUID(), mock(Player.class));

        config.set("reserved-slots.custom-permission", "3");
        config.set("reserved-slots.custom-permission7", "7");
        config.set("reserved-slots.custom-permission1", "1");

        when(player.hasPermission(ReservedSlotsListener.PERM_NOT_AFFECTED)).thenReturn(false);
        when(player.hasPermission(ReservedSlotsListener.PERM_JOIN_FULL)).thenReturn(false);
        when(player.hasPermission("custom-permission")).thenReturn(false);
        when(server.getMaxPlayers()).thenReturn(5);
        when(server.getOnlinePlayers()).thenReturn(onlinePlayers);
        
        PlayerPreLoginEvent event = new PlayerPreLoginEvent(player, "");
        listener.onAsyncPreLogin(event);
        assertTrue(event.isCancelled());
        assertEquals(messages.getString("reservedslots.reserved"), event.getKickMessage());

        config.set("custom-messages.5", "Message 5 (not expected)");
        config.set("custom-messages.3", "Message 3 (expected)");
        config.set("custom-messages.8", "Message 8 (not expected)");
        config.set("custom-messages.0", "Message 0 (not expected)");

        listener.onAsyncPreLogin(event);
        assertEquals("Message 3 (expected)", event.getKickMessage());
    }

    @Test
    void onAsyncPreLoginReservedButAllowed() {
        Map<UUID, Player> onlinePlayers = new HashMap<>();
        onlinePlayers.put(UUID.randomUUID(), mock(Player.class));
        onlinePlayers.put(UUID.randomUUID(), mock(Player.class));

        config.set("reserved-slots.custom-permission", "3");

        when(player.hasPermission(ReservedSlotsListener.PERM_NOT_AFFECTED)).thenReturn(false);
        when(player.hasPermission(ReservedSlotsListener.PERM_JOIN_FULL)).thenReturn(false);
        when(player.hasPermission("custom-permission")).thenReturn(true);
        when(server.getMaxPlayers()).thenReturn(5);
        when(server.getOnlinePlayers()).thenReturn(onlinePlayers);

        PlayerPreLoginEvent event = new PlayerPreLoginEvent(player, "");
        listener.onAsyncPreLogin(event);
        assertFalse(event.isCancelled());
    }
    
    @Test
    @SuppressWarnings("unchecked")
    void getMessagesCache() throws ReflectiveOperationException {
        Method getMessagesCache = ReservedSlotsListener.class.getDeclaredMethod("getMessagesCache");
        getMessagesCache.setAccessible(true);

        config.set("custom-messages.5", "hi");
        Int2ObjectMap<String> messagesMap1 = (Int2ObjectMap<String>) getMessagesCache.invoke(listener);
        assertSame(getMessagesCache.invoke(listener), messagesMap1);
        assertEquals("hi", messagesMap1.get(5));
        assertEquals(1, messagesMap1.size());
        
        config.set("custom-messages.8", "bye");
        Int2ObjectMap<String> messagesMap2 = (Int2ObjectMap<String>) getMessagesCache.invoke(listener);
        assertNotSame(messagesMap1, messagesMap2);
        assertNotEquals(messagesMap1, messagesMap2);
        assertEquals("bye", messagesMap2.get(8));
        assertEquals("hi", messagesMap2.get(5));
        assertEquals(2, messagesMap2.size());

        assertSame(getMessagesCache.invoke(listener), messagesMap2);

        config.set("custom-messages.bad", "ignored");
        messagesMap1 = (Int2ObjectMap<String>) getMessagesCache.invoke(listener);
        assertNotSame(messagesMap2, messagesMap1);
        assertEquals(messagesMap2, messagesMap1);

        config.set("custom-messages.-3", "negative ignored");
        messagesMap2 = (Int2ObjectMap<String>) getMessagesCache.invoke(listener);
        assertNotSame(messagesMap1, messagesMap2);
        assertEquals(messagesMap1, messagesMap2);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getReservedSlotsCache() throws ReflectiveOperationException {
        Method getReservedSlotsCache = ReservedSlotsListener.class.getDeclaredMethod("getReservedSlotsCache");
        getReservedSlotsCache.setAccessible(true);

        config.set("reserved-slots.perm", "5");
        Int2ObjectMap<String> permMap1 = (Int2ObjectMap<String>) getReservedSlotsCache.invoke(listener);
        assertSame(getReservedSlotsCache.invoke(listener), permMap1);
        assertEquals("perm", permMap1.get(5));
        assertEquals(1, permMap1.size());

        config.set("reserved-slots.lol", "8");
        Int2ObjectMap<String> permMap2 = (Int2ObjectMap<String>) getReservedSlotsCache.invoke(listener);
        assertNotSame(permMap1, permMap2);
        assertNotEquals(permMap1, permMap2);
        assertEquals("lol", permMap2.get(8));
        assertEquals("perm", permMap2.get(5));
        assertEquals(2, permMap2.size());

        assertSame(getReservedSlotsCache.invoke(listener), permMap2);

        config.set("reserved-slots.ignored", "bad");
        permMap1 = (Int2ObjectMap<String>) getReservedSlotsCache.invoke(listener);
        assertNotSame(permMap2, permMap1);
        assertEquals(permMap2, permMap1);

        config.set("reserved-slots.negative-ignored", "-3");
        permMap2 = (Int2ObjectMap<String>) getReservedSlotsCache.invoke(listener);
        assertNotSame(permMap1, permMap2);
        assertEquals(permMap1, permMap2);
    }
}
