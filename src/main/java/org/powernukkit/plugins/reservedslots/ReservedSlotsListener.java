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
import java.util.stream.Collectors;

/**
 * @author joserobjr
 * @since 2021-06-28
 */
class ReservedSlotsListener implements Listener {
    static final String PERM_NOT_AFFECTED = "reservedslots.notaffected";
    static final String PERM_JOIN_FULL = "reservedslots.joinfull";
    static final String DEFAULT_VALUE = "default";
    
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
    
    @EventHandler(ignoreCancelled = true)
    public void onQueryRegenerateEvent(QueryRegenerateEvent event) {
        if (event.getMaxPlayerCount() > event.getPlayerCount()) {
            return;
        }
        
        if (getBoolean("change-ping-packet", true)) {
            event.setMaxPlayerCount(event.getPlayerCount() + 1);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerKickEvent(PlayerKickEvent event) {
        if (event.getReasonEnum() == PlayerKickEvent.Reason.SERVER_FULL && isAffected(event) && canJoinFull(event)) {
            event.setCancelled(true);
        }
    }
    
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
    
    private void kickFull(PlayerPreLoginEvent event, int slots) {
        event.setCancelled(true);
        event.setKickMessage(getMessage(slots));
    }
    
    private String getMessage(int currentSlots) {
        return getMessagesCache().int2ObjectEntrySet().stream()
                .filter(e-> e.getIntKey() >= currentSlots)
                .min(Comparator.comparingInt(Int2ObjectMap.Entry::getIntKey))
                .map(Map.Entry::getValue)
                .orElseGet(()-> defaultMessages.getString("reservedslots.reserved"));
    }
    
    private Optional<String> getReservation(int currentSlots) {
        return getReservedSlotsCache().int2ObjectEntrySet().stream()
                .filter(e-> e.getIntKey() >= currentSlots)
                .min(Comparator.comparingInt(Int2ObjectMap.Entry::getIntKey))
                .map(Map.Entry::getValue);
    }
    
    private Int2ObjectMap<String> getMessagesCache() {
        ConfigSection section = config.getSection("custom-messages");
        int hash = section.hashCode();
        Int2ObjectMap<String> cache;
        if (hash == messagesHash) {
            return messagesCache;
        }
        
        cache = section.entrySet().stream()
                .filter(e-> e != null && e.getKey() != null && e.getValue() != null)
                .map(e-> {
                    try {
                        return new AbstractInt2ObjectMap.BasicEntry<>(Integer.parseInt(e.getKey()), e.getValue().toString());
                    } catch (NumberFormatException ignored) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .filter(e-> e.getIntKey() >= 0)
                .collect(Collectors.toMap(Int2ObjectMap.Entry::getIntKey, Int2ObjectMap.Entry::getValue, (a,b)->b, 
                        Int2ObjectOpenHashMap::new));
        
        messagesCache = cache;
        messagesHash = hash;
        return cache;
    }
    
    private Int2ObjectMap<String> getReservedSlotsCache() {
        ConfigSection section = config.getSection("reserved-slots");
        int hash = section.hashCode();
        if (reservedSlotsHash == hash) {
            return reservedSlotsCache;
        }
        Int2ObjectMap<String> map = section.entrySet().stream()
                .filter(e -> e != null && e.getKey() != null && e.getValue() != null)
                .map(e-> {
                    try {
                        Object value = e.getValue();
                        return new AbstractInt2ObjectMap.BasicEntry<>(
                                value instanceof Number? ((Number) value).intValue() 
                                        : Integer.parseInt(e.getValue().toString()), 
                                e.getKey());
                    } catch (NumberFormatException ignored) {
                        return null;
                    }
                })
                .filter(e -> e != null && e.getIntKey() > 0)
                .collect(Collectors.toMap(Int2ObjectMap.Entry::getIntKey, Int2ObjectMap.Entry::getValue, (a, b) -> b,
                        Int2ObjectOpenHashMap::new));
        
        reservedSlotsCache = map;
        reservedSlotsHash = hash;
        return map;
    }
    
    private boolean canJoinFull(PlayerEvent event) {
        return event.getPlayer().hasPermission(PERM_JOIN_FULL);
    }
    
    private boolean isAffected(PlayerEvent event) {
        return !event.getPlayer().hasPermission(PERM_NOT_AFFECTED);
    }
    
    private boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(getConfig(key, Boolean.toString(defaultValue)));
    }
    
    private String getConfig(String key, String defaultValue) {
        String value = config.getString(key, DEFAULT_VALUE);
        if (value.equalsIgnoreCase(DEFAULT_VALUE)) {
            return defaultValue;
        } else {
            return value;
        }
    }
}
