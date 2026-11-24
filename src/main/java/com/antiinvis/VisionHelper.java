package com.antiinvis;

import com.antiinvis.config.AntiInvisConfig;
import com.antiinvis.config.VisionMode;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;

/**
 * Mixin'ler arasında ortak varlık filtreleme, görünmezlik, görüş hattı ve Render State güncelleme sınıfı.
 * Mantığın mixin paketi (com.antiinvis.mixin) dışında tutulması, enum switch-map ($1) gibi
 * sentetik sınıfların Mixin paketinde üretilmesini önler ve hedef sınıfa enjekte edilen bytecode'u en aza indirir.
 */
public final class VisionHelper {
    private static final String BADGE_MARKER = "[GÖRÜNMEZ]";
    private static final Component INVIS_BADGE = Component.literal(BADGE_MARKER + " ")
        .withStyle(ChatFormatting.LIGHT_PURPLE);

    private VisionHelper() {}

    /**
     * Varlığın Anti-Invisibility kapsamında işlenip işlenmeyeceğini belirler.
     * - Dekoratif / canlı olmayan varlıkları (ItemFrame, Display, Marker vb.) hariç tutar.
     * - Çok oyunculu sunuculardaki hologram ve mobilya amaçlı görünmez ArmorStand'ları hariç tutar.
     * - affectPlayersOnly açıksa yalnızca Avatar (Player / Mannequin) varlıklarını kabul eder.
     */
    public static boolean isTargetEntity(Entity entity) {
        if (!(entity instanceof LivingEntity) || entity instanceof ArmorStand) {
            return false;
        }
        return !AntiInvisConfig.isAffectPlayersOnly() || entity instanceof Avatar;
    }

    /**
     * Varlığın metadata bayrağı veya iksir etkisi sebebiyle görünmez olup olmadığını kontrol eder.
     */
    public static boolean isEntityInvisible(Entity entity) {
        return entity.isInvisible()
            || (entity instanceof LivingEntity living && living.hasEffect(MobEffects.INVISIBILITY));
    }

    /**
     * Duvardan görme (seeThroughWalls) kapalıyken varlığın görsel olarak görüş hattında olup olmadığını kontrol eder.
     * ClipContext.Block.VISUAL sayesinde cam, demir parmaklık ve çit gibi şeffaf bloklar görüşü kesmez;
     * yalnızca katı duvarlar parlamayı engeller.
     */
    public static boolean canSeeEntity(Minecraft mc, Entity entity) {
        if (AntiInvisConfig.isSeeThroughWalls()) {
            return true;
        }
        if (mc == null) {
            return true;
        }

        LivingEntity viewer = (mc.getCameraEntity() instanceof LivingEntity cameraLiving)
            ? cameraLiving
            : mc.player;

        if (viewer == null || entity == viewer) {
            return true;
        }

        return viewer.hasLineOfSight(entity, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, entity.getEyeY())
            || viewer.hasLineOfSight(entity, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, entity.getY() + entity.getBbHeight() * 0.5);
    }

    /**
     * LivingEntityRenderer.extractRenderState sonunda (TAIL) çağrılarak render state'i günceller.
     */
    public static void applyRenderState(LivingEntity entity, LivingEntityRenderState state, float partialTick) {
        if (!AntiInvisConfig.isEnabled() || entity == null || state == null) {
            return;
        }

        if (!isTargetEntity(entity)) {
            return;
        }

        boolean entityWasInvisible = isEntityInvisible(entity);
        if (!state.isInvisible && !entityWasInvisible) {
            return;
        }

        VisionMode mode = AntiInvisConfig.getVisionMode();

        // Duvardan görme devredeyse eğilen (sneaking) oyuncuların isim etiketinin
        // duvar arkasından gizlenmesini engellemek için isDiscrete bayrağını kaldır
        if (AntiInvisConfig.isSeeThroughWalls()) {
            state.isDiscrete = false;
        }

        switch (mode) {
            case TRANSLUCENT -> {
                // Spectator tarzı yarı saydam (translucent) render; vanilla parlama (Spectral Arrow) korunur
                state.isInvisible = true;
                state.isInvisibleToPlayer = false;
                if (AntiInvisConfig.isShowNameBadge()) {
                    attachNameBadge(entity, state, partialTick);
                }
            }
            case OPAQUE_GLOWING -> {
                // Tam opak model, zırh ve tutulan eşyalar
                state.isInvisible = false;
                state.isInvisibleToPlayer = false;

                // MinecraftMixin normal durumda EntityRenderer.extractRenderState sırasında state.outlineColor değerini ayarlar.
                // Yalnızca outlineColor 0 ise (örn. harici mod state.isInvisible'ı değiştirdiyse veya hook atlandıysa) fallback uygula.
                if (state.outlineColor == 0
                        && (AntiInvisConfig.isSeeThroughWalls()
                            || (!entityWasInvisible && canSeeEntity(Minecraft.getInstance(), entity)))) {
                    state.outlineColor = ARGB.opaque(entity.getTeamColor());
                }

                if (AntiInvisConfig.isShowNameBadge()) {
                    attachNameBadge(entity, state, partialTick);
                }
            }
            case OPAQUE_TAGGED -> {
                // Tam opak model, zırh ve eşyalar + İsim Etiketinde Belirteç
                state.isInvisible = false;
                state.isInvisibleToPlayer = false;
                attachNameBadge(entity, state, partialTick);
            }
            case OPAQUE_PURE -> {
                // Tam opak normal render (belirteçsiz)
                state.isInvisible = false;
                state.isInvisibleToPlayer = false;
            }
        }
    }

    /**
     * İsim etiketini ekler.
     * - F1 (HUD gizli) modunda ve yerel oyuncu / aktif kamera varlığı (F5, Freecam, Spectator) için etiket eklemez.
     * - Oyuncular (Avatar) veya özel isimli mob'lar için fallback olarak displayName/name kullanır.
     * - İsimsiz rastgele mob'lar için etiket oluşturmaz.
     * - Kardeş bileşen (sibling) yapısı ile renk sızıntısını ve çift eklemeyi engeller.
     */
    private static void attachNameBadge(LivingEntity entity, LivingEntityRenderState state, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            if (entity == mc.player || entity == mc.getCameraEntity()) {
                return;
            }
            if (mc.gui != null && mc.gui.hud != null && mc.gui.hud.isHidden()) {
                return;
            }
        }

        if (state.nameTag != null) {
            if (!state.nameTag.getString().contains(BADGE_MARKER)) {
                state.nameTag = Component.empty().append(INVIS_BADGE).append(state.nameTag);
            }
        } else if (entity instanceof Avatar || entity.hasCustomName()) {
            Component name = entity.getDisplayName();
            if (name == null) {
                name = entity.getName();
            }
            if (name != null) {
                state.nameTag = Component.empty().append(INVIS_BADGE).append(name);
            }
        }

        if (state.nameTag != null) {
            ensureNameTagAttachment(entity, state, partialTick);
        }
    }

    private static void ensureNameTagAttachment(LivingEntity entity, LivingEntityRenderState state, float partialTick) {
        if (state.nameTagAttachment == null) {
            Vec3 official = null;
            try {
                if (entity.getAttachments() != null) {
                    official = entity.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, entity.getYRot(partialTick));
                }
            } catch (Exception ignored) {}
            state.nameTagAttachment = (official != null) ? official : new Vec3(0.0, entity.getBbHeight() + 0.5, 0.0);
        }
    }
}
