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
 * Minecraft sınıfındaki shouldEntityAppearGlowing metoduna müdahale eder.
 *
 * MİMARİ AMACI:
 * OPAQUE_GLOWING modunda görünmez bir varlığın zırhı ve modeli tam opak render edilirken,
 * EntityRenderer'ın bu varlığa parlayan dış hat (outline buffer) atamasını sağlar.
 *
 * seeThroughWalls ayarı kapalıysa, katı blokların arkasındaki varlıklar için parlamayı tetiklemez
 * (cam gibi şeffaf bloklar ClipContext.Block.VISUAL ile görüşü kesmez).
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
