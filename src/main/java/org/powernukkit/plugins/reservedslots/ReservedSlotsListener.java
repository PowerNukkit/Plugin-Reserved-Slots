/*
 * https://PowerNukkit.org - The Nukkit you know but Powerful!
 * Copyright (C) 2021  José Roberto de Araújo Júnior
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.powernukkit.plugins.reservedslots;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerEvent;
import cn.nukkit.event.player.PlayerKickEvent;
import cn.nukkit.event.player.PlayerPreLoginEvent;
import cn.nukkit.event.server.QueryRegenerateEvent;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.ConfigSection;
import it.unimi.dsi.fastutil.ints.AbstractInt2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author joserobjr
 * @since 2021-06-28
 */
class ReservedSlotsListener implements Listener {
    static final String PERM_NOT_AFFECTED = "reservedslots.notaffected";
    static final String PERM_JOIN_FULL = "reservedslots.joinfull";
    static final String DEFAULT_VALUE = "default";
    
    private final Object slotsLock = new Object();
    private final Object messagesLock = new Object();
    private final Config config;
    private final ResourceBundle defaultMessages;
    
    private Int2ObjectMap<String> reservedSlotsCache = Int2ObjectMaps.emptyMap();
    private Int2ObjectMap<String> messagesCache = Int2ObjectMaps.emptyMap();
    private int reservedSlotsHash; 
    private int messagesHash; 

    ReservedSlotsListener(Config config, ResourceBundle messages) {
        this.config = config;
        this.defaultMessages = messages;
    }

    /**
     * Increase the maximum player limit if the change-ping-packet configuration is enabled.
     */
    @EventHandler(ignoreCancelled = true)
    public void onQueryRegenerateEvent(QueryRegenerateEvent event) {
        if (event.getMaxPlayerCount() > event.getPlayerCount()) {
            return;
        }
        
        if (getBoolean("change-ping-packet", true)) {
            event.setMaxPlayerCount(event.getPlayerCount() + 1);
        }
    }

    /**
     * Allows players who have the permission to join when the server is full to join on that condition 
     * by cancelling the kick. 
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerKickEvent(PlayerKickEvent event) {
        if (event.getReasonEnum() == PlayerKickEvent.Reason.SERVER_FULL && isAffected(event) && canJoinFull(event)) {
            event.setCancelled(true);
        }
    }

    /**
     * Apply the restriction to the last slots available on the server.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPreLogin(PlayerPreLoginEvent event) {
        if (!isAffected(event) || canJoinFull(event)) {
            return;
        }
        
        Player player = event.getPlayer();
        int slots = Math.max(0, player.getServer().getMaxPlayers() - player.getServer().getOnlinePlayers().size());
        if (slots == 0) {
            kickFull(event, slots);
            return;
        }
        
        Optional<String> reservation = getReservation(slots);
        if (reservation.isPresent() && !player.hasPermission(reservation.get())) {
            kickFull(event, slots);
        }
    }

    /**
     * Cancels the event and set the kick message.
     * @param event Event to be cancelled
     * @param slots The amount of slots remaining
     */
    private void kickFull(PlayerPreLoginEvent event, int slots) {
        event.setCancelled(true);
        event.setKickMessage(getMessage(slots));
    }

    /**
     * Gets the kick message to be show to players who can't join when the server have that amount of remaining slots.
     * @param currentSlots Slots remaining.
     * @return The kick message.
     */
    private String getMessage(int currentSlots) {
        return getMessagesCache().int2ObjectEntrySet().stream()
                .filter(e-> e.getIntKey() >= currentSlots)
                .min(Comparator.comparingInt(Int2ObjectMap.Entry::getIntKey))
                .map(Map.Entry::getValue)
                .orElseGet(()-> defaultMessages.getString("reservedslots.reserved"));
    }

    /**
     * Gets the permission required to join when the server have that amount of remaining slots.
     * @param currentSlots Slots remaining.
     * @return Permission required to join.
     */
    private Optional<String> getReservation(int currentSlots) {
        return getReservedSlotsCache().int2ObjectEntrySet().stream()
                .filter(e-> e.getIntKey() >= currentSlots)
                .min(Comparator.comparingInt(Int2ObjectMap.Entry::getIntKey))
                .map(Map.Entry::getValue);
    }

    /**
     * Return the cached map which was parsed from the custom-messages config section.
     *
     * A new cache is automatically computed when the configuration is changed.
     */
    private Int2ObjectMap<String> getMessagesCache() {
        ConfigSection section = config.getSection("custom-messages");
        int hash = section.hashCode();
        Int2ObjectMap<String> cache;
        if (hash == messagesHash) {
            return messagesCache;
        }
        
        synchronized (messagesLock) {
            cache = safeEntryStream(section)
                    .map(toInt2StringEntry(Map.Entry::getKey, Map.Entry::getValue))
                    .filter(e -> e != null && e.getIntKey() >= 0)
                    .collect(toInt2StringMap());

            messagesCache = Int2ObjectMaps.unmodifiable(cache);
            messagesHash = hash;
            return cache;
        }
    }

    /**
     * Return the cached map which was parsed from the reserved-slots config section.
     * 
     * A new cache is automatically computed when the configuration is changed.
     */
    private Int2ObjectMap<String> getReservedSlotsCache() {
        ConfigSection section = config.getSection("reserved-slots");
        int hash = section.hashCode();
        if (reservedSlotsHash == hash) {
            return reservedSlotsCache;
        }
        
        synchronized (slotsLock) {
            Int2ObjectMap<String> map = safeEntryStream(section)
                    .map(toInt2StringEntry(Map.Entry::getValue, Map.Entry::getKey))
                    .filter(e -> e != null && e.getIntKey() > 0)
                    .collect(toInt2StringMap());

            reservedSlotsCache = Int2ObjectMaps.synchronize(map);
            reservedSlotsHash = hash;
            return map;
        }
    }

    /**
     * Checks if the player in the event have permission to join even when the server is full.
     */
    private boolean canJoinFull(PlayerEvent event) {
        return event.getPlayer().hasPermission(PERM_JOIN_FULL);
    }

    /**
     * Checks if the player in the event should be affected by this plugin.
     */
    private boolean isAffected(PlayerEvent event) {
        return !event.getPlayer().hasPermission(PERM_NOT_AFFECTED);
    }

    /**
     * Workaround for https://github.com/PowerNukkit/PowerNukkit/issues/1162
     */
    static Stream<Map.Entry<String, String>> safeEntryStream(@SuppressWarnings("rawtypes") Map configSection) {
        @SuppressWarnings("unchecked")
        Map<Object, Object> objectMap = configSection;

        Stream<Map.Entry<String, String>> stream =  objectMap.entrySet().stream()
                .filter(e-> e != null && e.getKey() != null && e.getValue() != null)
                .map(e-> new AbstractMap.SimpleEntry<>(
                        Objects.toString(e.getKey()).trim(),
                        Objects.toString(e.getValue()).trim()
                ));

        return stream.filter(e-> !e.getValue().isEmpty());
    }

    /**
     * Collects {@code Int2ObjectMap.Entry<String>} into a new {@link Int2ObjectOpenHashMap} of {@link String} values. 
     */
    private Collector<Int2ObjectMap.Entry<String>, ?, Int2ObjectOpenHashMap<String>> toInt2StringMap() {
        return Collectors.toMap(
                Int2ObjectMap.Entry::getIntKey,
                Int2ObjectMap.Entry::getValue,
                (a, b) -> b,
                Int2ObjectOpenHashMap::new
        );
    }

    /**
     * Converts a {@code Map.Entry<String, String>} into a {@code Int2ObjectMap.Entry<String>} by mapping
     * the returned value of the `keyFunction` and `valueFunction` functions.
     * 
     * If The `keyFunction` don't return a valid number parsable by {@link Integer#parseInt(String)},
     * then `null` is returned.
     */
    private Function<Map.Entry<String, String>, Int2ObjectMap.Entry<String>> toInt2StringEntry(
            Function<Map.Entry<String, String>, String> keyFunction, 
            Function<Map.Entry<String, String>, String> valueFunction
    ) {
        return entry -> createIntToStringEntry(keyFunction.apply(entry), valueFunction.apply(entry));
    }

    /**
     * Creates an {@code AbstractInt2ObjectMap.BasicEntry<String>} if `key` is a valid number. Otherwise returns `null`.
     */
    private Int2ObjectMap.Entry<String> createIntToStringEntry(String key, String value) {
        try {
            return new AbstractInt2ObjectMap.BasicEntry<>(Integer.parseInt(key), value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * Same as {@link #getConfig(String, String)} but using booleans values.
     */
    private boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(getConfig(key, Boolean.toString(defaultValue)));
    }

    /**
     * Reads a config key checking if it is missing from the configuration or has the special default value.
     * 
     * If the key is missing or is the special default value, then the `defaultValue` as parameter is returned,
     * otherwise, the current value is returned.
     */
    private String getConfig(String key, String defaultValue) {
        String value = config.getString(key, DEFAULT_VALUE);
        if (value.equalsIgnoreCase(DEFAULT_VALUE)) {
            return defaultValue;
        } else {
            return value;
        }
    }
}
