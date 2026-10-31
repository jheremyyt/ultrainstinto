package dev.uitransform.client.mixin;

import dev.uitransform.client.UiTransformation;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Hace que tu mano en primera persona brille (luz maxima) mientras estas cubierto de blanco.
 * Es solo cosmetico: si Minecraft cambia este metodo, simplemente no se aplica (require = 0).
 */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
	@ModifyVariable(method = "renderHand", at = @At("HEAD"), argsOnly = true, require = 0)
	private int uitransform$fullBrightHand(int light) {
		return UiTransformation.isGlowing() ? LightTexture.FULL_BRIGHT : light;
	}
}
