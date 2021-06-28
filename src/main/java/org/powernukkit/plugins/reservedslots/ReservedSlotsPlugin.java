package org.powernukkit.plugins.reservedslots;

import cn.nukkit.api.UsedByReflection;
import cn.nukkit.lang.BaseLang;
import cn.nukkit.plugin.PluginBase;

import java.util.Locale;
import java.util.ResourceBundle;

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
            return Locale.forLanguageTag(iso6391);
        }
        
        switch (language.getLang()) {
            case "bra": return Locale.forLanguageTag("pt_BR");
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
            case "vi": return Locale.forLanguageTag("vi");
            default: case "eng": return Locale.ENGLISH;
        }
    }
}
