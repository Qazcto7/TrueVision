package com.antiinvis.mixin;

import com.antiinvis.AntiInvisClient;
import com.antiinvis.config.AntiInvisConfig;
import com.antiinvis.config.VisionMode;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Minecraft sınıfındaki shouldEntityAppearGlowing metoduna müdahale eder.
 *
 * MİMARİ AMACI:
 * OPAQUE_GLOWING modunda görünmez bir varlığın zırhı ve modeli tam opak render edilirken,
 * EntityRenderDispatcher'ın bu varlığı parlayan dış hat (outline buffer) katmanına da
 * göndermesini sağlar.
 *
 * seeThroughWalls ayarı kapalıysa, blokların arkasındaki varlıklar için parlamayı tetiklemez.
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
        if (entity == null || !AntiInvisClient.isEnabled() 
                || AntiInvisConfig.getVisionMode() != VisionMode.OPAQUE_GLOWING) {
            return;
        }

        boolean isInvis = entity.isInvisible() 
            || (entity instanceof LivingEntity living && living.hasEffect(MobEffects.INVISIBILITY));

        if (isInvis) {
            if (!AntiInvisConfig.isAffectPlayersOnly() || entity instanceof Player) {
                // Duvardan parlama kontrolü (seeThroughWalls)
                if (!AntiInvisConfig.isSeeThroughWalls()) {
                    Minecraft mc = (Minecraft) (Object) this;
                    if (mc.player != null && !mc.player.hasLineOfSight(entity)) {
                        return; // Blok arkasındaysa parlamasın
                    }
                }
                cir.setReturnValue(true);
            }
        }
    }
}
