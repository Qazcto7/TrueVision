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
 * Shared client-side helper for entity filtering, invisibility detection,
 * line-of-sight checks, and Minecraft 26.3 Render State updates.
 *
 * Keeping all logic outside the mixin package prevents synthetic inner classes ($1)
 * from being generated inside the mixin package and keeps injected bytecode minimal.
 */
public final class VisionHelper {
    private static final String BADGE_MARKER = "[INVIS]";
    private static final Component INVIS_BADGE = Component.literal(BADGE_MARKER + " ")
        .withStyle(ChatFormatting.LIGHT_PURPLE);

    private VisionHelper() {}

    /**
     * Determines whether an entity should be processed by TrueVision.
     * - Excludes non-living decorative entities (ItemFrame, Display, Marker, Interaction, etc.).
     * - Excludes ArmorStands so multiplayer server holograms, floating text, and custom furniture stay intact.
     * - When affectPlayersOnly is enabled, restricts handling to Avatar entities (Players / Mannequins).
     */
    public static boolean isTargetEntity(Entity entity) {
        if (!(entity instanceof LivingEntity) || entity instanceof ArmorStand) {
            return false;
        }
        return !AntiInvisConfig.isAffectPlayersOnly() || entity instanceof Avatar;
    }

    /**
     * Checks whether an entity is invisible via synced entity metadata flags (used across all servers,
     * including ViaFabricPlus / Paper / Spigot / Vanilla) or via a local Invisibility mob effect.
     */
    public static boolean isEntityInvisible(Entity entity) {
        return entity.isInvisible()
            || (entity instanceof LivingEntity living && living.hasEffect(MobEffects.INVISIBILITY));
    }

    /**
     * Checks whether the entity is visually in line of sight when seeThroughWalls is disabled.
     * Uses ClipContext.Block.VISUAL so glass, iron bars, and fences do not block vision,
     * while solid walls suppress glowing outlines.
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

        if (entity.level() != viewer.level()) {
            return false;
        }

        return viewer.hasLineOfSight(entity, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, entity.getEyeY())
            || viewer.hasLineOfSight(entity, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, entity.getY() + entity.getBbHeight() * 0.5);
    }

    /**
     * Invoked at the tail of LivingEntityRenderer.extractRenderState to update the extracted render state.
     * Does not touch vertex buffers or draw calls, preserving compatibility with Sodium, Iris, and ImmediatelyFast.
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

        // Allow sneaking invisible players' nametags to remain visible through blocks when seeThroughWalls is enabled
        if (AntiInvisConfig.isSeeThroughWalls()) {
            state.isDiscrete = false;
        }

        switch (mode) {
            case TRANSLUCENT -> {
                // Spectator-style translucent rendering; preserves any vanilla glowing (e.g. Spectral Arrows)
                state.isInvisible = true;
                state.isInvisibleToPlayer = false;
                if (AntiInvisConfig.isShowNameBadge()) {
                    attachNameBadge(entity, state, partialTick);
                }
            }
            case OPAQUE_GLOWING -> {
                // Fully opaque body, armor, held items, and cape
                state.isInvisible = false;
                state.isInvisibleToPlayer = false;

                // MinecraftMixin normally populates state.outlineColor via EntityRenderer.extractRenderState.
                // Fallback only if outlineColor is still 0 (e.g., state.isInvisible was set externally).
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
                // Fully opaque model with forced [INVIS] nametag indicator
                state.isInvisible = false;
                state.isInvisibleToPlayer = false;
                attachNameBadge(entity, state, partialTick);
            }
            case OPAQUE_PURE -> {
                // Fully opaque standard rendering with no extra indicators
                state.isInvisible = false;
                state.isInvisibleToPlayer = false;
            }
        }
    }

    /**
     * Attaches the [INVIS] nametag badge without leaking color into the entity's original name.
     * - Skips when F1 (Hide HUD) is active or when rendering the local player / active camera entity (F5, Freecam, Spectator).
     * - Falls back to getDisplayName() and getName() when team rules, sneaking, or server plugins clear/blank the nametag.
     * - Leaves unnamed wild mobs untagged to avoid visual clutter.
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

        if (state.nameTag != null && !state.nameTag.getString().isBlank()) {
            if (!state.nameTag.getString().contains(BADGE_MARKER)) {
                state.nameTag = Component.empty().append(INVIS_BADGE).append(state.nameTag);
            }
        } else if (entity instanceof Avatar || entity.hasCustomName()) {
            Component name = entity.getDisplayName();
            if (name == null || name.getString().isBlank()) {
                name = entity.getName();
            }
            if (name != null && !name.getString().isBlank()) {
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
