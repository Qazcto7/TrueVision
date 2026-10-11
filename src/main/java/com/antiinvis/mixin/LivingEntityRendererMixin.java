package com.antiinvis.mixin;

import com.antiinvis.AntiInvisClient;
import com.antiinvis.config.AntiInvisConfig;
import com.antiinvis.config.VisionMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Minecraft 26.3 Render State Mimarisinde LivingEntityRenderer veri çıkarma katmanına müdahale eder.
 *
 * MİMARİ AÇIKLAMA:
 * Minecraft 26.3'te render mantığı doğrudan Entity üzerinden değil, extractRenderState ile üretilen
 * LivingEntityRenderState / PlayerRenderState nesneleri üzerinden yürütülür.
 *
 * Bu Mixin, veri çıkarma aşamasının sonunda (TAIL) devreye girerek:
 * 1. OPAQUE modlarında: state.isInvisible = false ve state.isInvisibleToPlayer = false yapar.
 *    Böylece oyun motoru ve katmanlar (HumanoidArmorLayer, ItemInHandLayer, CapeLayer)
 *    zırhları ve silahları normal bir varlık gibi render kuyruğuna sokar.
 * 2. TRANSLUCENT modunda: Spectator modundaki gibi state.isInvisible = true ve
 *    state.isInvisibleToPlayer = false bırakarak varlığı yarı saydam (alpha ~0.5f) render ettirir.
 * 3. OPAQUE_GLOWING modunda: state.outlineColor = 0xFFFFFFFF (veya teamColor) yaparak
 *    parlayan dış silüet ekler. seeThroughWalls kontrolü ile duvar arkasındaki varlıkların
 *    parlaması isteğe göre engellenir.
 * 4. İsim Etiketi: Eğilen (crouching) veya etiketi gizlenmiş görünmez oyuncuların isimlerini
 *    getDisplayName() ve getName() ile alarak "[GÖRÜNMEZ]" etiketi ekler.
 *    İsimsiz mob'ların mağaralarda gereksiz etiketle dolmasını engeller.
 *
 * UYUMLULUK:
 * - Doğrudan çizim komutlarına (submit/render), VertexConsumer'a veya MultiBufferSource tamponlarına dokunulmaz.
 * - Sodium, Iris ve ImmediatelyFast buffer/mesh mekanizmaları hiçbir şekilde bozulmaz.
 * - priority = 1100 ve require = 0 sayesinde enjektör hatasız çalışır.
 */
@Mixin(value = LivingEntityRenderer.class, priority = 1100)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState> {

    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
        at = @At("TAIL"),
        require = 0
    )
    private void antiinvis$onExtractRenderState(T entity, S state, float partialTick, CallbackInfo ci) {
        if (!AntiInvisClient.isEnabled()) {
            return;
        }

        if (AntiInvisConfig.isAffectPlayersOnly() && !(entity instanceof Player)) {
            return;
        }

        // 1. Görünmezlik Kontrolü: Hem metadata bayrağı (isInvisible) hem de iksir etkisi (MobEffects.INVISIBILITY)
        boolean isInvisible = entity.isInvisible() || entity.hasEffect(MobEffects.INVISIBILITY);

        if (isInvisible) {
            VisionMode mode = AntiInvisConfig.getVisionMode();

            switch (mode) {
                case TRANSLUCENT -> {
                    // Spectator tarzı yarı saydam (translucent / alpha ~0.5f) render
                    state.isInvisible = true;
                    state.isInvisibleToPlayer = false;
                    state.outlineColor = 0;
                }
                case OPAQUE_GLOWING -> {
                    // Tam opak model, zırh ve tutulan eşyalar
                    state.isInvisible = false;
                    state.isInvisibleToPlayer = false;

                    // Duvardan parlama kontrolü (seeThroughWalls)
                    boolean shouldGlow = true;
                    if (!AntiInvisConfig.isSeeThroughWalls()) {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.player != null && !mc.player.hasLineOfSight(entity)) {
                            shouldGlow = false;
                        }
                    }

                    if (shouldGlow) {
                        int teamColor = entity.getTeamColor();
                        state.outlineColor = (teamColor != 0xFFFFFF && teamColor != 0) 
                            ? ARGB.opaque(teamColor) 
                            : 0xFFFFFFFF;
                    } else {
                        state.outlineColor = 0;
                    }

                    if (AntiInvisConfig.isShowNameBadge()) {
                        attachNameBadge(entity, state);
                    }
                }
                case OPAQUE_TAGGED -> {
                    // Tam opak model, zırh ve eşyalar + İsim Etiketinde Belirteç
                    state.isInvisible = false;
                    state.isInvisibleToPlayer = false;
                    state.outlineColor = 0;
                    attachNameBadge(entity, state);
                }
                case OPAQUE_PURE -> {
                    // Tam opak normal render (belirteçsiz)
                    state.isInvisible = false;
                    state.isInvisibleToPlayer = false;
                    state.outlineColor = 0;
                }
            }
        }
    }

    /**
     * İsim etiketini ekler.
     * - Oyuncular veya özel isimli varlıklar için fallback olarak displayName/name kullanır.
     * - İsimsiz rastgele mob'lar için etiket oluşturmaz.
     * - Çift eklemeleri (string duplicate) engeller ve nameTagAttachment null kontrolü yapar.
     */
    private void attachNameBadge(T entity, S state) {
        if (state.nameTag != null) {
            String currentName = state.nameTag.getString();
            if (!currentName.contains("[GÖRÜNMEZ]")) {
                state.nameTag = Component.literal("§d[GÖRÜNMEZ] ").append(state.nameTag);
            }
        } else if (entity instanceof Player || entity.hasCustomName()) {
            Component name = entity.getDisplayName();
            if (name == null) {
                name = entity.getName();
            }
            if (name != null) {
                state.nameTag = Component.literal("§d[GÖRÜNMEZ] ").append(name);
                if (state.nameTagAttachment == null) {
                    state.nameTagAttachment = new Vec3(0.0, entity.getBbHeight() + 0.5, 0.0);
                }
            }
        }
    }
}
