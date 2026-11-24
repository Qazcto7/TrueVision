package com.antiinvis.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Mod ayarlarını yöneten ve .minecraft/config/antiinvis.json dosyasına atomik olarak kaydeden sınıf.
 */
public class AntiInvisConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("antiinvis");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("antiinvis.json");

    private static volatile AntiInvisConfig INSTANCE = new AntiInvisConfig();

    // Ayar değişkenleri (thread-safe okuma için volatile)
    private volatile boolean enabled = true;
    private volatile VisionMode visionMode = VisionMode.OPAQUE_GLOWING;
    private volatile boolean seeThroughWalls = true;
    private volatile boolean affectPlayersOnly = false;
    private volatile boolean showNameBadge = true;

    public static boolean isEnabled() {
        return INSTANCE.enabled;
    }

    public static synchronized boolean toggleEnabled() {
        boolean next = !INSTANCE.enabled;
        INSTANCE.enabled = next;
        save();
        return next;
    }

    public static VisionMode getVisionMode() {
        VisionMode mode = INSTANCE.visionMode;
        return mode != null ? mode : VisionMode.OPAQUE_GLOWING;
    }

    public static synchronized VisionMode cycleMode() {
        VisionMode next = getVisionMode().next();
        INSTANCE.visionMode = next;
        save();
        return next;
    }

    public static boolean isSeeThroughWalls() {
        return INSTANCE.seeThroughWalls;
    }

    public static synchronized boolean toggleSeeThroughWalls() {
        boolean next = !INSTANCE.seeThroughWalls;
        INSTANCE.seeThroughWalls = next;
        save();
        return next;
    }

    public static boolean isAffectPlayersOnly() {
        return INSTANCE.affectPlayersOnly;
    }

    public static synchronized boolean toggleAffectPlayersOnly() {
        boolean next = !INSTANCE.affectPlayersOnly;
        INSTANCE.affectPlayersOnly = next;
        save();
        return next;
    }

    public static boolean isShowNameBadge() {
        return INSTANCE.showNameBadge;
    }

    public static synchronized boolean toggleShowNameBadge() {
        boolean next = !INSTANCE.showNameBadge;
        INSTANCE.showNameBadge = next;
        save();
        return next;
    }

    public static synchronized void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
            AntiInvisConfig config = GSON.fromJson(reader, AntiInvisConfig.class);
            if (config == null) {
                LOGGER.warn("[Anti-Invis] Yapılandırma dosyası boş veya geçersiz, varsayılan ayarlar korunuyor.");
                return;
            }
            if (config.visionMode == null) {
                LOGGER.warn("[Anti-Invis] Geçersiz visionMode değeri tespit edildi, varsayılan (OPAQUE_GLOWING) kullanılıyor.");
                config.visionMode = VisionMode.OPAQUE_GLOWING;
            }
            INSTANCE = config;
        } catch (Exception e) {
            LOGGER.error("[Anti-Invis] Yapılandırma dosyası okunamadı, varsayılan ayarlar korunuyor.", e);
        }
    }

    public static synchronized void save() {
        Path tempPath = CONFIG_PATH.resolveSibling(CONFIG_PATH.getFileName() + ".tmp");
        try {
            if (CONFIG_PATH.getParent() != null) {
                Files.createDirectories(CONFIG_PATH.getParent());
            }
            try (Writer writer = Files.newBufferedWriter(tempPath, StandardCharsets.UTF_8)) {
                GSON.toJson(INSTANCE, writer);
            }
            try {
                Files.move(tempPath, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempPath, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            LOGGER.error("[Anti-Invis] Yapılandırma dosyası diske kaydedilemedi.", e);
            try {
                Files.deleteIfExists(tempPath);
            } catch (Exception ignored) {}
        }
    }
}
