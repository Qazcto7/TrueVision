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
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * İstemci tarafı mod ana giriş noktası.
 * Kısayol tuşlarını ('V' ve 'B'), /antiinvis ve /ai komutlarını ve bildirimleri yönetir.
 */
public class AntiInvisClient implements ClientModInitializer {
    public static final String MOD_ID = "antiinvis";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final SystemToast.SystemToastId TOAST_ID = new SystemToast.SystemToastId(2500L);

    private static final KeyMapping.Category CATEGORY;
    static {
        KeyMapping.Category cat;
        try {
            cat = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("antiinvis", "title"));
        } catch (Exception e) {
            cat = KeyMapping.Category.GAMEPLAY;
        }
        CATEGORY = cat;
    }

    private static KeyMapping toggleKey;
    private static KeyMapping modeKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[Anti-Invis] İstemci modu başlatılıyor...");

        AntiInvisConfig.load();

        // Minecraft 26.3 tablosundaki 'V' (25) ve 'B' (5) değerlerini bağla (hata korumalı)
        int keyV = 25;
        int keyB = 5;
        try {
            keyV = InputConstants.getKey("key.keyboard.v").getValue();
            keyB = InputConstants.getKey("key.keyboard.b").getValue();
        } catch (Exception ignored) {}

        // 1. Aç/Kapa kısayol tuşu ('V' tuşu)
        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.antiinvis.toggle",
            keyV,
            CATEGORY
        ));

        // 2. Görünüm modu değiştirme kısayol tuşu ('B' tuşu)
        modeKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.antiinvis.cycle_mode",
            keyB,
            CATEGORY
        ));

        // İstemci tick dinleyicisi: tuş tıklamalarını yakala
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client == null || client.player == null) {
                return;
            }

            // Sohbet, envanter veya herhangi bir GUI ekranı açıkken basılan tuşları tüket ve modu tetikleme
            if (client.gui != null && (client.gui.screen() != null || client.gui.overlay() != null)) {
                while (toggleKey.consumeClick()) {}
                while (modeKey.consumeClick()) {}
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
                    toggleWallhackAction(context.getSource());
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
                    toggleWallhackAction(context.getSource());
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
                }))
            );
        });

        LOGGER.info("[Anti-Invis] Başarıyla yüklendi. 'V' ve 'B' tuşları aktif, /antiinvis ve /ai komutları devrede.");
    }

    public static void toggleAction(Minecraft client) {
        if (client == null) {
            client = Minecraft.getInstance();
        }

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
                TOAST_ID,
                Component.literal("§6Anti-Invis"),
                Component.literal(status + (newState ? " §7(" + modeName + "§7)" : ""))
            );
        }
    }

    public static void cycleModeAction(Minecraft client) {
        if (client == null) {
            client = Minecraft.getInstance();
        }

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
                TOAST_ID,
                Component.literal("§6Görünüm Modu"),
                Component.literal(newMode.getDisplayName())
            );
        }
    }

    private static void toggleWallhackAction(FabricClientCommandSource source) {
        boolean stw = AntiInvisConfig.toggleSeeThroughWalls();
        String msg = "§6[Anti-Invis] §7Duvardan Görme (Wallhack): " + (stw ? "§aAÇIK" : "§cKAPALI");
        source.sendFeedback(Component.literal(msg));

        Minecraft client = Minecraft.getInstance();
        if (client.getSoundManager() != null) {
            client.getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, stw ? 1.2f : 0.8f)
            );
        }
    }

    private static void togglePlayersOnlyAction(FabricClientCommandSource source) {
        boolean po = AntiInvisConfig.toggleAffectPlayersOnly();
        String msg = "§6[Anti-Invis] §7Yalnızca Oyuncuları Göster: " + (po ? "§aAÇIK (Sadece Oyuncular)" : "§eKAPALI (Tüm Varlıklar)");
        source.sendFeedback(Component.literal(msg));

        Minecraft client = Minecraft.getInstance();
        if (client.getSoundManager() != null) {
            client.getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, po ? 1.2f : 0.8f)
            );
        }
    }

    private static void toggleBadgeAction(FabricClientCommandSource source) {
        boolean badge = AntiInvisConfig.toggleShowNameBadge();
        String msg = "§6[Anti-Invis] §7[GÖRÜNMEZ] İsim Etiketi: " + (badge ? "§aAÇIK" : "§cKAPALI");
        source.sendFeedback(Component.literal(msg));

        Minecraft client = Minecraft.getInstance();
        if (client.getSoundManager() != null) {
            client.getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, badge ? 1.2f : 0.8f)
            );
        }
    }

    private static void sendStatusReport(FabricClientCommandSource source) {
        boolean enabled = AntiInvisConfig.isEnabled();
        VisionMode mode = AntiInvisConfig.getVisionMode();
        boolean wallhack = AntiInvisConfig.isSeeThroughWalls();
        boolean playersOnly = AntiInvisConfig.isAffectPlayersOnly();
        boolean badge = AntiInvisConfig.isShowNameBadge();

        source.sendFeedback(Component.literal("§6===== [Anti-Invisibility Durum Raporu] ====="));
        source.sendFeedback(Component.literal("§7Durum: " + (enabled ? "§aAÇIK" : "§cKAPALI")));
        source.sendFeedback(Component.literal("§7Görünüm Modu: §e" + mode.getDisplayName()));
        source.sendFeedback(Component.literal("§7Duvardan Görme (Wallhack): " + (wallhack ? "§aAÇIK" : "§cKAPALI")));
        source.sendFeedback(Component.literal("§7Yalnızca Oyuncular: " + (playersOnly ? "§aAÇIK (Sadece Oyuncular)" : "§7KAPALI (Tüm Varlıklar)")));
        source.sendFeedback(Component.literal("§7[GÖRÜNMEZ] İsim Etiketi: " + (badge ? "§aAÇIK" : "§cKAPALI")));
        source.sendFeedback(Component.literal("§7Kısayol Tuşları: §f[V] §7Aç/Kapa | §f[B] §7Mod Değiştir"));
        source.sendFeedback(Component.literal("§7Komutlar: §f/ai §7| §f/ai mode §7| §f/ai wallhack §7| §f/ai players §7| §f/ai badge §7| §f/ai status §7| §f/ai help"));
        source.sendFeedback(Component.literal("§6==========================================="));
    }

    private static void sendHelpReport(FabricClientCommandSource source) {
        source.sendFeedback(Component.literal("§6===== [Anti-Invisibility Komut Yardımı] ====="));
        source.sendFeedback(Component.literal("§f/ai §8(veya /ai toggle) §7- Modu açar / kapatır"));
        source.sendFeedback(Component.literal("§f/ai mode §7- Görünüm modunu sırayla değiştirir"));
        source.sendFeedback(Component.literal("§f/ai wallhack §7- Duvardan parlamayı (ESP) açar / kapatır"));
        source.sendFeedback(Component.literal("§f/ai players §7- Yalnızca oyuncuları hedeflemeyi açar / kapatır"));
        source.sendFeedback(Component.literal("§f/ai badge §7- [GÖRÜNMEZ] isim etiketini açar / kapatır"));
        source.sendFeedback(Component.literal("§f/ai status §7- Mevcut durum ve yapılandırmayı listeler"));
        source.sendFeedback(Component.literal("§f/ai help §7- Bu yardım kılavuzunu görüntüler"));
        source.sendFeedback(Component.literal("§7Kısayol Tuşları:"));
        source.sendFeedback(Component.literal("  §f[V] §7- Modu Aç / Kapat"));
        source.sendFeedback(Component.literal("  §f[B] §7- Görünüm modları arasında geçiş yap"));
        source.sendFeedback(Component.literal("§6==========================================="));
    }

    public static boolean isEnabled() {
        return AntiInvisConfig.isEnabled();
    }

    public static void toggle() {
        AntiInvisConfig.setEnabled(!AntiInvisConfig.isEnabled());
    }
}
