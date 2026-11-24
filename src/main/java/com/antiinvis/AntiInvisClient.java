package com.antiinvis;

import com.antiinvis.config.AntiInvisConfig;
import com.antiinvis.config.VisionMode;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-side entrypoint for TrueVision (Anti-Invisibility).
 * Manages keybindings ('V' and 'B'), client-only /antiinvis and /ai commands, and HUD notifications.
 * Sends zero network packets so it works seamlessly on all multiplayer servers and singleplayer worlds.
 */
public class AntiInvisClient implements ClientModInitializer {
    public static final String MOD_ID = "antiinvis";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final SystemToast.SystemToastId TOAST_ID = new SystemToast.SystemToastId(2500L);

    private static KeyMapping toggleKey;
    private static KeyMapping modeKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[TrueVision] Initializing client mod...");

        AntiInvisConfig.load();

        KeyMapping.Category category;
        try {
            category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "title"));
        } catch (Exception e) {
            LOGGER.warn("[TrueVision] Could not register custom key category; falling back to GAMEPLAY.", e);
            category = KeyMapping.Category.GAMEPLAY;
        }

        // 1. Toggle keybind (default: 'V')
        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.antiinvis.toggle",
            InputConstants.KEY_V,
            category
        ));

        // 2. Cycle vision mode keybind (default: 'B')
        modeKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.antiinvis.cycle_mode",
            InputConstants.KEY_B,
            category
        ));

        // Client tick listener for debounced key handling
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client == null || client.player == null || client.level == null || toggleKey == null || modeKey == null) {
                return;
            }

            // Drain key clicks while any GUI screen or loading overlay is active
            if (client.gui != null && (client.gui.screen() != null || client.gui.overlay() != null)) {
                while (toggleKey.consumeClick()) {}
                while (modeKey.consumeClick()) {}
                return;
            }

            if (toggleKey.consumeClick()) {
                while (toggleKey.consumeClick()) {}
                toggleAction(client);
            }

            if (modeKey.consumeClick()) {
                while (modeKey.consumeClick()) {}
                cycleModeAction(client);
            }
        });

        // Client-side chat commands (/antiinvis and /ai) — handled locally, never sent to servers
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(buildRootCommand("ai"));
            dispatcher.register(buildRootCommand("antiinvis"));
        });

        LOGGER.info("[TrueVision] Loaded successfully. Keybinds [V]/[B] and client commands /antiinvis & /ai are active.");
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildRootCommand(String name) {
        LiteralArgumentBuilder<FabricClientCommandSource> modeNode = literal("mode")
            .executes(context -> {
                cycleModeAction(Minecraft.getInstance());
                return 1;
            });

        for (VisionMode mode : VisionMode.values()) {
            modeNode.then(literal(mode.getCommandKey()).executes(context -> {
                setModeAction(Minecraft.getInstance(), mode);
                return 1;
            }));
        }

        return literal(name)
            .executes(context -> {
                toggleAction(Minecraft.getInstance());
                return 1;
            })
            .then(literal("toggle").executes(context -> {
                toggleAction(Minecraft.getInstance());
                return 1;
            }))
            .then(modeNode)
            .then(literal("walls").executes(context -> {
                toggleThroughWallsAction(context.getSource());
                return 1;
            }))
            .then(literal("throughwalls").executes(context -> {
                toggleThroughWallsAction(context.getSource());
                return 1;
            }))
            .then(literal("players").executes(context -> {
                togglePlayersOnlyAction(context.getSource());
                return 1;
            }))
            .then(literal("badge").executes(context -> {
                toggleBadgeAction(context.getSource());
                return 1;
            }))
            .then(literal("status").executes(context -> {
                sendStatusReport(context.getSource());
                return 1;
            }))
            .then(literal("help").executes(context -> {
                sendHelpReport(context.getSource());
                return 1;
            }));
    }

    public static void toggleAction(Minecraft client) {
        if (client == null) {
            client = Minecraft.getInstance();
        }

        boolean newState = AntiInvisConfig.toggleEnabled();
        if (client == null) {
            return;
        }

        playClickSound(client, newState ? 1.2f : 0.8f);

        String status = newState ? "§aON" : "§cOFF";
        String modeName = AntiInvisConfig.getVisionMode().getDisplayName();
        String message = "§6[TrueVision] " + status + (newState ? " §7(Mode: " + modeName + "§7)" : "");

        if (client.player != null) {
            client.player.sendOverlayMessage(Component.literal(message));
        }

        if (client.gui != null && client.gui.toastManager() != null) {
            SystemToast.addOrUpdate(
                client.gui.toastManager(),
                TOAST_ID,
                Component.literal("§6TrueVision"),
                Component.literal(status + (newState ? " §7(" + modeName + "§7)" : ""))
            );
        }
    }

    public static void cycleModeAction(Minecraft client) {
        notifyModeChange(client, AntiInvisConfig.cycleMode());
    }

    public static void setModeAction(Minecraft client, VisionMode mode) {
        notifyModeChange(client, AntiInvisConfig.setVisionMode(mode));
    }

    private static void notifyModeChange(Minecraft client, VisionMode newMode) {
        if (client == null) {
            client = Minecraft.getInstance();
        }
        if (client == null) {
            return;
        }

        playClickSound(client, 1.0f);

        String message = "§6[TrueVision] §7Vision Mode: " + newMode.getDisplayName();

        if (client.player != null) {
            client.player.sendOverlayMessage(Component.literal(message));
        }

        if (client.gui != null && client.gui.toastManager() != null) {
            SystemToast.addOrUpdate(
                client.gui.toastManager(),
                TOAST_ID,
                Component.literal("§6Vision Mode"),
                Component.literal(newMode.getDisplayName())
            );
        }
    }

    private static void toggleThroughWallsAction(FabricClientCommandSource source) {
        boolean stw = AntiInvisConfig.toggleSeeThroughWalls();
        String msg = "§6[TrueVision] §7See Through Walls: " + (stw ? "§aON" : "§cOFF §7(Line of Sight Only)");
        source.sendFeedback(Component.literal(msg));
        playClickSound(Minecraft.getInstance(), stw ? 1.2f : 0.8f);
    }

    private static void togglePlayersOnlyAction(FabricClientCommandSource source) {
        boolean po = AntiInvisConfig.toggleAffectPlayersOnly();
        String msg = "§6[TrueVision] §7Target Filter: " + (po ? "§aPlayers Only" : "§eAll Living Entities (Players & Mobs)");
        source.sendFeedback(Component.literal(msg));
        playClickSound(Minecraft.getInstance(), po ? 1.2f : 0.8f);
    }

    private static void toggleBadgeAction(FabricClientCommandSource source) {
        boolean badge = AntiInvisConfig.toggleShowNameBadge();
        String msg = "§6[TrueVision] §7[INVIS] Nametag Badge: " + (badge ? "§aON" : "§cOFF");
        source.sendFeedback(Component.literal(msg));
        playClickSound(Minecraft.getInstance(), badge ? 1.2f : 0.8f);
    }

    private static void playClickSound(Minecraft client, float pitch) {
        if (client != null && client.getSoundManager() != null) {
            client.getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch)
            );
        }
    }

    private static String getKeyDisplayName(KeyMapping mapping, String fallback) {
        if (mapping == null) {
            return fallback;
        }
        String label = mapping.getTranslatedKeyMessage().getString();
        return (label == null || label.isBlank()) ? fallback : label;
    }

    private static void sendStatusReport(FabricClientCommandSource source) {
        boolean enabled = AntiInvisConfig.isEnabled();
        VisionMode mode = AntiInvisConfig.getVisionMode();
        boolean throughWalls = AntiInvisConfig.isSeeThroughWalls();
        boolean playersOnly = AntiInvisConfig.isAffectPlayersOnly();
        boolean badge = AntiInvisConfig.isShowNameBadge();
        String toggleLabel = getKeyDisplayName(toggleKey, "V");
        String modeLabel = getKeyDisplayName(modeKey, "B");

        source.sendFeedback(Component.literal("§6===== [TrueVision Status] ====="));
        source.sendFeedback(Component.literal("§7Status: " + (enabled ? "§aON" : "§cOFF")));
        source.sendFeedback(Component.literal("§7Vision Mode: " + mode.getDisplayName() + " §8(" + mode.getDescription() + ")"));
        source.sendFeedback(Component.literal("§7See Through Walls: " + (throughWalls ? "§aON" : "§cOFF (Line of Sight Only)")));
        source.sendFeedback(Component.literal("§7Target Filter: " + (playersOnly ? "§aPlayers Only" : "§eAll Living Entities (Players & Mobs)")));
        source.sendFeedback(Component.literal("§7[INVIS] Nametag Badge: " + (badge ? "§aON" : "§cOFF")));
        source.sendFeedback(Component.literal("§7Keybinds: §f[" + toggleLabel + "] §7Toggle | §f[" + modeLabel + "] §7Cycle Mode"));
        source.sendFeedback(Component.literal("§7Commands: §f/ai §7| §f/ai mode §7| §f/ai walls §7| §f/ai players §7| §f/ai badge §7| §f/ai status §7| §f/ai help"));
        source.sendFeedback(Component.literal("§6==============================="));
    }

    private static void sendHelpReport(FabricClientCommandSource source) {
        String toggleLabel = getKeyDisplayName(toggleKey, "V");
        String modeLabel = getKeyDisplayName(modeKey, "B");

        source.sendFeedback(Component.literal("§6===== [TrueVision Command Help] ====="));
        source.sendFeedback(Component.literal("§f/ai §8(or /antiinvis, /ai toggle) §7- Toggle TrueVision ON / OFF"));
        source.sendFeedback(Component.literal("§f/ai mode §8[glowing|translucent|tagged|pure] §7- Cycle or set vision mode"));
        source.sendFeedback(Component.literal("§f/ai walls §8(or /ai throughwalls) §7- Toggle outlines & sneaking tags through walls"));
        source.sendFeedback(Component.literal("§f/ai players §7- Toggle Players Only vs Players & Mobs"));
        source.sendFeedback(Component.literal("§f/ai badge §7- Toggle the [INVIS] nametag badge"));
        source.sendFeedback(Component.literal("§f/ai status §7- Show current configuration and status"));
        source.sendFeedback(Component.literal("§f/ai help §7- Show this help guide"));
        source.sendFeedback(Component.literal("§7Available Vision Modes:"));
        for (VisionMode mode : VisionMode.values()) {
            source.sendFeedback(Component.literal("  §f" + mode.getCommandKey() + " §8(" + mode.getDisplayName() + "§8) §7- " + mode.getDescription()));
        }
        source.sendFeedback(Component.literal("§7Keybinds:"));
        source.sendFeedback(Component.literal("  §f[" + toggleLabel + "] §7- Toggle TrueVision ON / OFF"));
        source.sendFeedback(Component.literal("  §f[" + modeLabel + "] §7- Cycle through vision modes"));
        source.sendFeedback(Component.literal("§6====================================="));
    }
}
