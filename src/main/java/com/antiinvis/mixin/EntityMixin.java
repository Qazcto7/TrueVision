package com.antiinvis.mixin;

import com.antiinvis.AntiInvisClient;
import com.antiinvis.config.AntiInvisConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Entity sınıfının isInvisibleTo kontrolüne müdahale eder.
 *
 * Anti-Invis devredeyken istemci oyuncusuna karşı varlıkların görünmez sayılmasını
 * engelleyerek nişangah odaklanması, hedef alma ve genel istemci mantığında varlığı görünür kılar.
 *
 * Mod Uyumluluğu:
 * - Kesinlikle @Overwrite kullanılmaz.
 * - priority = 1100 ve require = 0 sayesinde ViaFabricPlus ve optimizasyon modlarıyla çakışmaz.
 */
@Mixin(value = Entity.class, priority = 1100)
public abstract class EntityMixin {

    @Inject(
        method = "isInvisibleTo",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void antiinvis$onIsInvisibleTo(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (AntiInvisClient.isEnabled()) {
            if (!AntiInvisConfig.isAffectPlayersOnly() || ((Object) this instanceof Player)) {
                cir.setReturnValue(false);
            }
        }
    }
}
