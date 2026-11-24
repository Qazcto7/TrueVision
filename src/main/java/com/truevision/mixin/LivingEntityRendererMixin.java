package com.truevision.mixin;

import com.truevision.VisionHelper;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks the Minecraft 26.3 Render State extraction layer in LivingEntityRenderer.
 *
 * Delegates all logic to {@link VisionHelper#applyRenderState} outside the mixin package,
 * keeping injected bytecode minimal and preventing synthetic inner classes inside the mixin package.
 *
 * Compatibility:
 * - Never touches draw calls (submit/render), VertexConsumer, or MultiBufferSource buffers.
 * - Fully compatible with Sodium, Iris, ImmediatelyFast, ModernFix, NoRender, and ViaFabricPlus.
 * - Uses priority = 1100 and require = 0 for conflict-free injection.
 */
@Mixin(value = LivingEntityRenderer.class, priority = 1100)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState> {

    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
        at = @At("TAIL"),
        require = 0
    )
    private void truevision$onExtractRenderState(T entity, S state, float partialTick, CallbackInfo ci) {
        VisionHelper.applyRenderState(entity, state, partialTick);
    }
}
