package com.paperspacecraft.intellij.plugin.tinylynx.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.XCollection;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@State(
       name = "com.paperspacecraft.intellij.plugin.tinylynx.tinylynx-settings",
       storages = @Storage(StoragePathMacros.NON_ROAMABLE_FILE)
)
public final class SettingsService implements PersistentStateComponent<SettingsService> {

    private static final boolean DEFAULT_ON_THE_FLY = true;
    private static final boolean DEFAULT_SHOW_ADVANCED = true;
    private static final boolean DEFAULT_EXTENDED_LOGGING = false;

    static final int DEFAULT_CACHE_LIFESPAN = 30; // minutes
    static final int DEFAULT_PARALLEL_REQUESTS = 5;

    private static final String GRAMMARLY_PERSISTENT_COOKIE = "firefox_freemium=true; funnelType=free; browser_info=FIREFOX:67:COMPUTER:SUPPORTED:FREEMIUM:MAC_OS_X:MAC_OS_X;";
    private static final String GRAMMARLY_CLIENT_ORIGIN = "moz-extension://6adb0179-68f0-aa4f-8666-ae91f500210b";
    private static final String GRAMMARLY_CLIENT_TYPE = "extension-firefox";
    private static final String GRAMMARLY_CLIENT_VERSION = "8.852.2307";
    private static final String GRAMMARLY_USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.100 Safari/537.36";

    /* ----------------
       General settings
       ---------------- */

    @Tag
    @Getter
    @Setter
    private boolean onTheFly = DEFAULT_ON_THE_FLY;

    @Tag
    @Getter
    @Setter
    private boolean showAdvancedMistakes = DEFAULT_SHOW_ADVANCED;

    @Tag
    @Getter
    @Setter
    private int cacheLifespan = DEFAULT_CACHE_LIFESPAN;

    @Tag
    @Getter
    @Setter
    private int parallelRequests = DEFAULT_PARALLEL_REQUESTS;

    @Tag
    @Getter
    @Setter
    private boolean extendedLogging = DEFAULT_EXTENDED_LOGGING;

    @XCollection(elementName = "exclusion", propertyElementName = "exclusions")
    private List<String> exclusions;
    @Getter
    private Set<String> exclusionSet = new HashSet<>();

    /* ------------------
       Grammarly settings
       ------------------ */

    @Tag
    @Getter
    @Setter
    private String grammarlyClientType = GRAMMARLY_CLIENT_TYPE;

    @Tag
    @Getter
    @Setter
    private String grammarlyClientVersion = GRAMMARLY_CLIENT_VERSION;

    @Tag
    @Getter
    @Setter
    private String grammarlyClientOrigin = GRAMMARLY_CLIENT_ORIGIN;

    @Tag
    @Getter
    @Setter
    private String grammarlyUserAgent = GRAMMARLY_USER_AGENT;

    @Tag
    @Getter
    @Setter
    private String grammarlyCookie = GRAMMARLY_PERSISTENT_COOKIE;

    /* ----------------
       State management
       ---------------- */

    @Override
    public SettingsService getState() {
        this.exclusions = new ArrayList<>(exclusionSet);
        return this;
    }

    @Override
    public void loadState(@NotNull SettingsService state) {
        XmlSerializerUtil.copyBean(state, this);
        this.exclusionSet = exclusions != null ? new HashSet<>(exclusions) : new HashSet<>();
    }

    public static SettingsService getInstance(Project project) {
        return project.getService(SettingsService.class);
    }
}
