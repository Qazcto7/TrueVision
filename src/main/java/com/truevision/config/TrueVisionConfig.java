package com.truevision.config;

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
 * Manages client-side configuration and persists it atomically to .minecraft/config/truevision.json.
 */
public class TrueVisionConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("truevision");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("truevision.json");

    private static volatile TrueVisionConfig INSTANCE = new TrueVisionConfig();

    // Configuration fields (volatile for cross-thread visibility)
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

    public static synchronized VisionMode setVisionMode(VisionMode mode) {
        VisionMode target = (mode != null) ? mode : VisionMode.OPAQUE_GLOWING;
        INSTANCE.visionMode = target;
        save();
        return target;
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
            TrueVisionConfig config = GSON.fromJson(reader, TrueVisionConfig.class);
            if (config == null) {
                LOGGER.warn("[TrueVision] Config file is empty or invalid; keeping defaults.");
                return;
            }
            if (config.visionMode == null) {
                LOGGER.warn("[TrueVision] Invalid visionMode in config; falling back to OPAQUE_GLOWING.");
                config.visionMode = VisionMode.OPAQUE_GLOWING;
            }
            INSTANCE = config;
        } catch (Exception e) {
            LOGGER.error("[TrueVision] Failed to read config file; keeping defaults.", e);
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
            LOGGER.error("[TrueVision] Failed to save config file to disk.", e);
            try {
                Files.deleteIfExists(tempPath);
            } catch (Exception ignored) {}
        }
    }
}
