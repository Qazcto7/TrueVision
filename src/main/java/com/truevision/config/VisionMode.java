package com.truevision.config;

/**
 * Visual rendering modes for revealed invisible entities.
 */
public enum VisionMode {
    /**
     * Renders the entity's model, armor, and held items fully opaque
     * with a glowing outline for immediate visibility.
     */
    OPAQUE_GLOWING("glowing", "§eOpaque + Outline", "Solid model, armor, held items, and glowing outline"),

    /**
     * Spectator-style semi-transparent ghost rendering.
     */
    TRANSLUCENT("translucent", "§bTranslucent (Ghost)", "Spectator-style semi-transparent ghost rendering"),

    /**
     * Renders the entity fully opaque and prefixes its nametag with an [INVIS] badge.
     */
    OPAQUE_TAGGED("tagged", "§6Opaque + Badge", "Solid model with an [INVIS] nametag indicator"),

    /**
     * Renders the entity completely normally without any extra visual markers.
     */
    OPAQUE_PURE("pure", "§aOpaque (Pure)", "Normal solid rendering with no extra indicators");

    private final String commandKey;
    private final String displayName;
    private final String description;

    VisionMode(String commandKey, String displayName, String description) {
        this.commandKey = commandKey;
        this.displayName = displayName;
        this.description = description;
    }

    public String getCommandKey() {
        return commandKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Cycles to the next vision mode.
     */
    public VisionMode next() {
        VisionMode[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
