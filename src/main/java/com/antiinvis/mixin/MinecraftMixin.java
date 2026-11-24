package com.antiinvis.mixin;

import com.antiinvis.VisionHelper;
import com.antiinvis.config.AntiInvisConfig;
import com.antiinvis.config.VisionMode;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hooks Minecraft.shouldEntityAppearGlowing for OPAQUE_GLOWING mode.
 *
 * Enables Minecraft's native outline buffer for revealed invisible entities while
 * respecting the seeThroughWalls visual line-of-sight option (transparent blocks such as
 * glass do not block vision).
 */
@Mixin(value = Minecraft.class, priority = 1100)
public abstract class MinecraftMixin {

    @Inject(
        method = "shouldEntityAppearGlowing",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void antiinvis$onShouldEntityAppearGlowing(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity == null || !AntiInvisConfig.isEnabled()
                || AntiInvisConfig.getVisionMode() != VisionMode.OPAQUE_GLOWING) {
            return;
        }

        if (VisionHelper.isTargetEntity(entity)
                && VisionHelper.isEntityInvisible(entity)
                && VisionHelper.canSeeEntity((Minecraft) (Object) this, entity)) {
            cir.setReturnValue(true);
        }
    }
}
