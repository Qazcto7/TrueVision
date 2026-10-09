package com.antiinvis;

import com.antiinvis.config.AntiInvisConfig;
import com.antiinvis.config.VisionMode;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * İstemci tarafı mod ana giriş noktası.
 * Kısayol tuşlarını tanımlar, yapılandırmayı yükler, sesli ve görsel bildirimleri yönetir.
 */
public class AntiInvisClient implements ClientModInitializer {
    public static final String MOD_ID = "antiinvis";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyMapping toggleKey;
    private static KeyMapping modeKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[Anti-Invis] İstemci modu başlatılıyor...");

        // Yapılandırma ayarlarını yükle
        AntiInvisConfig.load();

        // 1. Aç/Kapa kısayol tuşu ('V' tuşu)
        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.antiinvis.toggle",
            GLFW.GLFW_KEY_V,
            KeyMapping.Category.GAMEPLAY
        ));

        // 2. Görünüm modu değiştirme kısayol tuşu ('B' tuşu)
        modeKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.antiinvis.cycle_mode",
            GLFW.GLFW_KEY_B,
            KeyMapping.Category.GAMEPLAY
        ));

        // İstemci tick dinleyicisi: tuş tıklamalarını yakala, ses çal ve bildirim göster
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                return;
            }

            // 'V' tuşuna basıldığında aç / kapa yap
            while (toggleKey.consumeClick()) {
                boolean newState = !AntiInvisConfig.isEnabled();
                AntiInvisConfig.setEnabled(newState);

                // Hafif tıklama sesi
                if (client.getSoundManager() != null) {
                    client.getSoundManager().play(
                        SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, newState ? 1.2f : 0.8f)
                    );
                }

                // Bildirim metinleri
                String status = newState ? "§aAÇIK" : "§cKAPALI";
                String modeName = AntiInvisConfig.getVisionMode().getDisplayName();
                String message = "§6[Anti-Invis] " + status + (newState ? " §7(Mod: " + modeName + "§7)" : "");

                // 1. ActionBar Bildirimi
                client.player.sendOverlayMessage(Component.literal(message));

                // 2. Ekranda küçük Toast bildirimi
                if (client.gui != null && client.gui.toastManager() != null) {
                    SystemToast.addOrUpdate(
                        client.gui.toastManager(),
                        SystemToast.SystemToastId.NARRATOR_TOGGLE,
                        Component.literal("§6Anti-Invis"),
                        Component.literal(status + (newState ? " §7(" + modeName + "§7)" : ""))
                    );
                }
            }

            // 'B' tuşuna basıldığında modu döngüsel olarak değiştir
            while (modeKey.consumeClick()) {
                VisionMode newMode = AntiInvisConfig.cycleMode();

                // Hafif tıklama sesi
                if (client.getSoundManager() != null) {
                    client.getSoundManager().play(
                        SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f)
                    );
                }

                String message = "§6[Anti-Invis] §7Görünüm Modu: " + newMode.getDisplayName();

                // 1. ActionBar Bildirimi
                client.player.sendOverlayMessage(Component.literal(message));

                // 2. Ekranda küçük Toast bildirimi
                if (client.gui != null && client.gui.toastManager() != null) {
                    SystemToast.addOrUpdate(
                        client.gui.toastManager(),
                        SystemToast.SystemToastId.NARRATOR_TOGGLE,
                        Component.literal("§6Görünüm Modu"),
                        Component.literal(newMode.getDisplayName())
                    );
                }
            }
        });

        LOGGER.info("[Anti-Invis] Başarıyla yüklendi. Sodium, Iris, ImmediatelyFast ve ModernFix ile tam uyumlu.");
    }

    public static boolean isEnabled() {
        return AntiInvisConfig.isEnabled();
    }

    public static void toggle() {
        AntiInvisConfig.setEnabled(!AntiInvisConfig.isEnabled());
    }
}
