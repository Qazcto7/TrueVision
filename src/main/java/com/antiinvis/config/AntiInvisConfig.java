package com.antiinvis.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Mod ayarlarını yöneten ve .minecraft/config/antiinvis.json dosyasına kaydeden sınıf.
 */
public class AntiInvisConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("AntiInvis");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("antiinvis.json");

    private static volatile AntiInvisConfig INSTANCE = new AntiInvisConfig();

    // Ayar değişkenleri
    private boolean enabled = true;
    private VisionMode visionMode = VisionMode.OPAQUE_GLOWING;
    private boolean seeThroughWalls = true;
    private boolean affectPlayersOnly = false;
    private boolean showNameBadge = true;

    public static AntiInvisConfig getInstance() {
        return INSTANCE;
    }

    public static boolean isEnabled() {
        return INSTANCE.enabled;
    }

    public static void setEnabled(boolean enabled) {
        INSTANCE.enabled = enabled;
        save();
    }

    public static VisionMode getVisionMode() {
        return INSTANCE.visionMode;
    }

    public static void setVisionMode(VisionMode mode) {
        if (mode != null) {
            INSTANCE.visionMode = mode;
            save();
        }
    }

    public static VisionMode cycleMode() {
        if (INSTANCE.visionMode == null) {
            INSTANCE.visionMode = VisionMode.OPAQUE_GLOWING;
        }
        INSTANCE.visionMode = INSTANCE.visionMode.next();
        save();
        return INSTANCE.visionMode;
    }

    public static boolean isSeeThroughWalls() {
        return INSTANCE.seeThroughWalls;
    }

    public static void setSeeThroughWalls(boolean seeThroughWalls) {
        INSTANCE.seeThroughWalls = seeThroughWalls;
        save();
    }

    public static boolean toggleSeeThroughWalls() {
        INSTANCE.seeThroughWalls = !INSTANCE.seeThroughWalls;
        save();
        return INSTANCE.seeThroughWalls;
    }

    public static boolean isAffectPlayersOnly() {
        return INSTANCE.affectPlayersOnly;
    }

    public static void setAffectPlayersOnly(boolean affectPlayersOnly) {
        INSTANCE.affectPlayersOnly = affectPlayersOnly;
        save();
    }

    public static boolean toggleAffectPlayersOnly() {
        INSTANCE.affectPlayersOnly = !INSTANCE.affectPlayersOnly;
        save();
        return INSTANCE.affectPlayersOnly;
    }

    public static boolean isShowNameBadge() {
        return INSTANCE.showNameBadge;
    }

    public static void setShowNameBadge(boolean showNameBadge) {
        INSTANCE.showNameBadge = showNameBadge;
        save();
    }

    public static boolean toggleShowNameBadge() {
        INSTANCE.showNameBadge = !INSTANCE.showNameBadge;
        save();
        return INSTANCE.showNameBadge;
    }

    public static synchronized void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            AntiInvisConfig config = GSON.fromJson(reader, AntiInvisConfig.class);
            if (config != null) {
                if (config.visionMode == null) {
                    config.visionMode = VisionMode.OPAQUE_GLOWING;
                }
                INSTANCE = config;
            }
        } catch (Exception e) {
            LOGGER.error("[Anti-Invis] Yapılandırma dosyası okunamadı, varsayılan ayarlar korunuyor.", e);
        }
    }

    public static synchronized void save() {
        try {
            if (CONFIG_PATH.getParent() != null) {
                Files.createDirectories(CONFIG_PATH.getParent());
            }
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(INSTANCE, writer);
            }
        } catch (Exception e) {
            LOGGER.error("[Anti-Invis] Yapılandırma dosyası diske kaydedilemedi.", e);
        }
    }
}
