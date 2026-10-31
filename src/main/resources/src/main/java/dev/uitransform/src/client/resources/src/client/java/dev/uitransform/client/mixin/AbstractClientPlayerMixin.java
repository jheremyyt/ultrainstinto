package dev.uitransform.client.mixin;

import dev.uitransform.client.UiTransformation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Cambia la skin del jugador local mientras dura la transformacion.
 * getSkin() lo usan tanto la vista en tercera persona como la mano en primera persona.
 */
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
	@Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
	private void uitransform$overrideSkin(CallbackInfoReturnable<PlayerSkin> cir) {
		if ((Object) this != Minecraft.getInstance().player) {
			return;
		}

		PlayerSkin patched = UiTransformation.override(cir.getReturnValue());

		if (patched != null) {
			cir.setReturnValue(patched);
		}
	}
}
