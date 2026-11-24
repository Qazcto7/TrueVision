package com.truevision.mixin;

import com.truevision.VisionHelper;
import com.truevision.config.TrueVisionConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hooks Entity.isInvisibleTo purely for the local client player's perspective.
 *
 * Strictly isolates the integrated server thread and LAN guests in singleplayer by checking
 * {@code player != mc.player}, ensuring zero impact on server-side AI or game logic.
 *
 * Modpack compatibility:
 * - Zero @Overwrite annotations.
 * - priority = 1100 and require = 0 for conflict-free interoperability with ViaFabricPlus and optimization mods.
 */
@Mixin(value = Entity.class, priority = 1100)
public abstract class EntityMixin {

    @Inject(
        method = "isInvisibleTo",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void truevision$onIsInvisibleTo(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (!TrueVisionConfig.isEnabled()) {
            return;
        }

        // Only affect the local client player's view (never integrated server or LAN ServerPlayers)
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || player != mc.player) {
            return;
        }

        if (VisionHelper.isTargetEntity((Entity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
