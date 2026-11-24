package com.antiinvis.mixin;

import com.antiinvis.VisionHelper;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Minecraft 26.3 Render State Mimarisinde LivingEntityRenderer veri çıkarma katmanına müdahale eder.
 *
 * MİMARİ AÇIKLAMA:
 * Minecraft 26.3'te render mantığı doğrudan Entity üzerinden değil, extractRenderState ile üretilen
 * LivingEntityRenderState / AvatarRenderState nesneleri üzerinden yürütülür.
 *
 * Tüm iş mantığı mixin paketi dışındaki {@link VisionHelper#applyRenderState} metoduna delege edilir;
 * böylece hedef sınıfa enjekte edilen bytecode minimumda kalır ve sentetik iç sınıf ($1) üretilmez.
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
        VisionHelper.applyRenderState(entity, state, partialTick);
    }
}
