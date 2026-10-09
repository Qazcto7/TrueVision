package com.antiinvis;

import com.antiinvis.config.AntiInvisConfig;
import com.antiinvis.config.VisionMode;
import com.mojang.blaze3d.platform.InputConstants;
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
import net.minecraft.sounds.SoundEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * İstemci tarafı mod ana giriş noktası.
 * Kısayol tuşlarını ('V' ve 'B'), /antiinvis sohbet komutunu ve bildirimleri yönetir.
 */
public class AntiInvisClient implements ClientModInitializer {
    public static final String MOD_ID = "antiinvis";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyMapping toggleKey;
    private static KeyMapping modeKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[Anti-Invis] İstemci modu başlatılıyor...");

        AntiInvisConfig.load();

        // Minecraft 26.3 tablosundaki 'V' (25) ve 'B' (5) değerlerini bağla
        int keyV = InputConstants.getKey("key.keyboard.v").getValue();
        int keyB = InputConstants.getKey("key.keyboard.b").getValue();

        // 1. Aç/Kapa kısayol tuşu ('V' tuşu)
        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.antiinvis.toggle",
            keyV,
            KeyMapping.Category.GAMEPLAY
        ));

        // 2. Görünüm modu değiştirme kısayol tuşu ('B' tuşu)
        modeKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.antiinvis.cycle_mode",
            keyB,
            KeyMapping.Category.GAMEPLAY
        ));

        // İstemci tick dinleyicisi: tuş tıklamalarını yakala
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                return;
            }

            while (toggleKey.consumeClick()) {
                toggleAction(client);
            }

            while (modeKey.consumeClick()) {
                cycleModeAction(client);
            }
        });

        // Sohbet komutları (/antiinvis ve /ai)
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("antiinvis")
                .executes(context -> {
                    toggleAction(Minecraft.getInstance());
                    return 1;
                })
                .then(literal("toggle").executes(context -> {
                    toggleAction(Minecraft.getInstance());
                    return 1;
                }))
                .then(literal("mode").executes(context -> {
                    cycleModeAction(Minecraft.getInstance());
                    return 1;
                }))
                .then(literal("wallhack").executes(context -> {
                    boolean stw = AntiInvisConfig.toggleSeeThroughWalls();
                    String msg = "§6[Anti-Invis] §7Duvardan Görme (Wallhack): " + (stw ? "§aAÇIK" : "§cKAPALI");
                    context.getSource().sendFeedback(Component.literal(msg));
                    return 1;
                }))
                .then(literal("status").executes(context -> {
                    sendStatusReport(context.getSource());
                    return 1;
                }))
            );

            dispatcher.register(literal("ai")
                .executes(context -> {
                    toggleAction(Minecraft.getInstance());
                    return 1;
                })
                .then(literal("toggle").executes(context -> {
                    toggleAction(Minecraft.getInstance());
                    return 1;
                }))
                .then(literal("mode").executes(context -> {
                    cycleModeAction(Minecraft.getInstance());
                    return 1;
                }))
                .then(literal("wallhack").executes(context -> {
                    boolean stw = AntiInvisConfig.toggleSeeThroughWalls();
                    String msg = "§6[Anti-Invis] §7Duvardan Görme (Wallhack): " + (stw ? "§aAÇIK" : "§cKAPALI");
                    context.getSource().sendFeedback(Component.literal(msg));
                    return 1;
                }))
                .then(literal("status").executes(context -> {
                    sendStatusReport(context.getSource());
                    return 1;
                }))
            );
        });

        LOGGER.info("[Anti-Invis] Başarıyla yüklendi. 'V' ve 'B' tuşları aktif, /antiinvis komutu devrede.");
    }

    public static void toggleAction(Minecraft client) {
        boolean newState = !AntiInvisConfig.isEnabled();
        AntiInvisConfig.setEnabled(newState);

        if (client.getSoundManager() != null) {
            client.getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, newState ? 1.2f : 0.8f)
            );
        }

        String status = newState ? "§aAÇIK" : "§cKAPALI";
        String modeName = AntiInvisConfig.getVisionMode().getDisplayName();
        String message = "§6[Anti-Invis] " + status + (newState ? " §7(Mod: " + modeName + "§7)" : "");

        if (client.player != null) {
            client.player.sendOverlayMessage(Component.literal(message));
        }

        if (client.gui != null && client.gui.toastManager() != null) {
            SystemToast.addOrUpdate(
                client.gui.toastManager(),
                SystemToast.SystemToastId.NARRATOR_TOGGLE,
                Component.literal("§6Anti-Invis"),
                Component.literal(status + (newState ? " §7(" + modeName + "§7)" : ""))
            );
        }
    }

    public static void cycleModeAction(Minecraft client) {
        VisionMode newMode = AntiInvisConfig.cycleMode();

        if (client.getSoundManager() != null) {
            client.getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f)
            );
        }

        String message = "§6[Anti-Invis] §7Görünüm Modu: " + newMode.getDisplayName();

        if (client.player != null) {
            client.player.sendOverlayMessage(Component.literal(message));
        }

        if (client.gui != null && client.gui.toastManager() != null) {
            SystemToast.addOrUpdate(
                client.gui.toastManager(),
                SystemToast.SystemToastId.NARRATOR_TOGGLE,
                Component.literal("§6Görünüm Modu"),
                Component.literal(newMode.getDisplayName())
            );
        }
    }

    private static void sendStatusReport(FabricClientCommandSource source) {
        boolean enabled = AntiInvisConfig.isEnabled();
        VisionMode mode = AntiInvisConfig.getVisionMode();
        boolean wallhack = AntiInvisConfig.isSeeThroughWalls();
        boolean playersOnly = AntiInvisConfig.isAffectPlayersOnly();

        source.sendFeedback(Component.literal("§6===== [Anti-Invisibility Durum Raporu] ====="));
        source.sendFeedback(Component.literal("§7Durum: " + (enabled ? "§aAÇIK" : "§cKAPALI")));
        source.sendFeedback(Component.literal("§7Görünüm Modu: §e" + mode.getDisplayName()));
        source.sendFeedback(Component.literal("§7Duvardan Görme (Wallhack): " + (wallhack ? "§aAÇIK" : "§cKAPALI")));
        source.sendFeedback(Component.literal("§7Yalnızca Oyuncular: " + (playersOnly ? "§aAÇIK" : "§7KAPALI (Tüm Varlıklar)")));
        source.sendFeedback(Component.literal("§7Kısayol Tuşları: §f[V] §7Aç/Kapa | §f[B] §7Mod Değiştir"));
        source.sendFeedback(Component.literal("§6==========================================="));
    }

    public static boolean isEnabled() {
        return AntiInvisConfig.isEnabled();
    }

    public static void toggle() {
        AntiInvisConfig.setEnabled(!AntiInvisConfig.isEnabled());
    }
}
