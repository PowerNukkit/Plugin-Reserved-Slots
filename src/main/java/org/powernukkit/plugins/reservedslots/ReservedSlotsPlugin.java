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

import cn.nukkit.api.UsedByReflection;
import cn.nukkit.lang.BaseLang;
import cn.nukkit.plugin.PluginBase;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * @author joserobjr
 * @since 2021-06-28
 */
@UsedByReflection
public class ReservedSlotsPlugin extends PluginBase {
    @Override
    public void onEnable() {
        saveDefaultConfig();
        ResourceBundle defaultMessages = ResourceBundle.getBundle(
                "org.powernukkit.plugins.reservedslots.reserved_slots_messages", getLocale());
        getServer().getPluginManager().registerEvents(new ReservedSlotsListener(getConfig(), defaultMessages), this);
    }
    
    private Locale getLocale() {
        BaseLang language = getServer().getLanguage();
        String iso6391 = language.translateString("language.locale");
        if (!"language.locale".equals(iso6391)) {
            String[] parts = iso6391.split("_", 2);
            if (parts.length == 2) {
                return new Locale(parts[0], parts[1]);
            } else {
                return new Locale(parts[0]);
            }
        }
        
        switch (language.getLang().toLowerCase(Locale.ENGLISH)) {
            case "bra": return new Locale("pt", "BR");
            case "chs": return Locale.SIMPLIFIED_CHINESE;
            case "cht": return Locale.TRADITIONAL_CHINESE;
            case "cze": return Locale.forLanguageTag("cs");
            case "deu": return Locale.GERMAN;
            case "fin": return Locale.forLanguageTag("fi");
            case "idn": return Locale.forLanguageTag("id");
            case "jpn": return Locale.JAPANESE;
            case "kor": return Locale.KOREAN;
            case "ltu": return Locale.forLanguageTag("lt");
            case "pol": return Locale.forLanguageTag("pl");
            case "rus": return Locale.forLanguageTag("ru");
            case "spa": return Locale.forLanguageTag("es");
            case "tur": return Locale.forLanguageTag("tr");
            case "ukr": return Locale.forLanguageTag("uk");
            case "vie": return Locale.forLanguageTag("vi");
            default: case "eng": return Locale.ENGLISH;
        }
    }
}
