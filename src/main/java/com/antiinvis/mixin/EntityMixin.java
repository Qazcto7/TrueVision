package com.antiinvis.mixin;

import com.antiinvis.VisionHelper;
import com.antiinvis.config.AntiInvisConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Entity sınıfının isInvisibleTo kontrolüne müdahale eder.
 *
 * Anti-Invis devredeyken istemci oyuncusuna karşı hedef varlıkların görünmez sayılmasını
 * engelleyerek nişangah odaklanması, isim etiketi ve genel istemci mantığında varlığı görünür kılar.
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
        if (!AntiInvisConfig.isEnabled()) {
            return;
        }

        // Tek oyunculuda yerel sunucu (integrated server) iş parçacığını ve diğer oyuncuları etkilememek için
        // yalnızca yerel istemci oyuncusunun (LocalPlayer) bakış açısına müdahale et.
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || player != mc.player) {
            return;
        }

        if (VisionHelper.isTargetEntity((Entity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
