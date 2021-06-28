package org.powernukkit.plugins.reservedslots;

import cn.nukkit.Server;
import cn.nukkit.api.UsedByReflection;
import cn.nukkit.lang.BaseLang;
import cn.nukkit.plugin.PluginDescription;
import cn.nukkit.plugin.PluginLoader;
import cn.nukkit.plugin.PluginManager;
import cn.nukkit.utils.Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author joserobjr
 * @since 2021-06-28
 */
@ExtendWith(MockitoExtension.class)
class ReservedSlotsPluginTest {
    private static final Set<Map.Entry<String, Locale>> builtinLanguages = Arrays.stream(new Object[][]{
            {"bra", new Locale("pt", "BR")},
            {"chs", Locale.SIMPLIFIED_CHINESE},
            {"cht", Locale.TRADITIONAL_CHINESE},
            {"cze", Locale.forLanguageTag("cs")},
            {"deu", Locale.GERMAN},
            {"fin", Locale.forLanguageTag("fi")},
            {"idn", Locale.forLanguageTag("id")},
            {"jpn", Locale.JAPANESE},
            {"kor", Locale.KOREAN},
            {"ltu", Locale.forLanguageTag("lt")},
            {"pol", Locale.forLanguageTag("pl")},
            {"rus", Locale.forLanguageTag("ru")},
            {"spa", Locale.forLanguageTag("es")},
            {"tur", Locale.forLanguageTag("tr")},
            {"ukr", Locale.forLanguageTag("uk")},
            {"vie", Locale.forLanguageTag("vi")},
            {"eng", Locale.ENGLISH},
    }).collect(Collectors.toMap(e->(String)e[0], e->(Locale)e[1], (a,b)->b, LinkedHashMap::new)).entrySet();
    
    @Mock
    PluginManager pluginManager;
    
    @Mock
    PluginLoader pluginLoader;
    
    @Mock
    Server server;
    
    @TempDir
    File serverDir;
    
    ReservedSlotsPlugin plugin;
    
    @BeforeEach
    void setUp() throws IOException {
        when(server.getPluginManager()).thenReturn(pluginManager);
        
        String descriptionYml;
        try (InputStream is = Objects.requireNonNull(ReservedSlotsPlugin.class.getResourceAsStream("/plugin.yml"))) {
            descriptionYml = Utils.readFile(is);
        }
        
        try (MockedStatic<Server> mockedStatic = mockStatic(Server.class)) {
            //noinspection ResultOfMethodCallIgnored
            mockedStatic.when(Server::getInstance).thenReturn(server);
            when(pluginManager.getPermissionSubscriptions(anyString())).thenAnswer(call-> new HashSet<>());

            plugin = new ReservedSlotsPlugin();
            PluginDescription description = new PluginDescription(descriptionYml);
            File pluginsDir = new File(serverDir, "plugins");
            plugin.init(pluginLoader,
                    server,
                    description,
                    new File(pluginsDir, description.getName()),
                    new File(pluginsDir, description.getName()+".jar"));
        }
    }

    @Test
    void onEnable() {
        when(server.getLanguage()).thenReturn(new BaseLang("eng"));
        plugin.onEnable();
        verify(pluginManager).registerEvents(any(ReservedSlotsListener.class), eq(plugin));
    }
    
    
    @ParameterizedTest
    @MethodSource("getBuiltinLanguages")
    void getLocale(Map.Entry<String, Locale> keyValue) {
        String lang = keyValue.getKey();
        Locale locale = keyValue.getValue();
        try(MockedStatic<ResourceBundle> mockedStatic = mockStatic(ResourceBundle.class)) {
            when(server.getLanguage()).thenReturn(new BaseLang(lang));
            
            mockedStatic.when(()-> ResourceBundle.getBundle(anyString(), any(Locale.class)))
                    .thenAnswer(call-> {
                        assertEquals("org.powernukkit.plugins.reservedslots.reserved_slots_messages",
                                call.getArgument(0));
                        assertEquals(locale, call.getArgument(1));
                        return call.callRealMethod();
                    });
            
            plugin.onEnable();
        }
    }

    @UsedByReflection
    private static Set<Map.Entry<String, Locale>> getBuiltinLanguages() {
        return builtinLanguages;
    }
}
