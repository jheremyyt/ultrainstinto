package dev.uitransform.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Maquina de estados de la transformacion (solo en tu cliente).
 *
 * 1) CAPARAZON: un aura azul-blanca crece alrededor de tu cuerpo hasta cubrirlo todo de blanco brillante.
 *    Mientras tanto puedes moverte, saltar, etc. con total libertad (no se bloquea nada).
 * 2) ESTALLIDO: el blanco se rompe en particulas emisivas que salen volando y aparece la skin elegida.
 * 3) Pulsa la tecla otra vez para volver a tu skin normal con un efecto mas corto.
 */
public final class UiTransformation {
	private UiTransformation() {
	}

	private static final long SHELL_TRANSFORM_MS = 2600L;
	private static final long SHELL_REVERT_MS = 1000L;
	private static final long AFTER_MS = 1000L;
	private static final long FLASH_MS = 500L;

	private static boolean transformed;     // ya tienes puesta la skin elegida
	private static boolean whiteSkin;       // estas completamente blanco
	private static boolean shellActive;     // el caparazon esta creciendo
	private static boolean target;          // estado al que se llega tras el estallido
	private static long shellStart;
	private static long shellDuration;
	private static long burstAt = -1L;

	private static PlayerSkin cacheOriginal;
	private static PlayerSkin.Patch cachePatch;
	private static PlayerSkin cacheResult;

	/** Datos para dibujar el aura (los usa la capa de render). */
	public record Shell(float coreAlpha, int coreRgb, float coreScale, float haloAlpha, int haloRgb, float haloScale) {
	}

	// ------------------------------------------------------------------ control

	public static void toggle(Minecraft mc) {
		if (mc.player == null || mc.level == null) {
			return;
		}

		long now = System.currentTimeMillis();

		if (shellActive || (burstAt >= 0 && now - burstAt < AFTER_MS)) {
			return; // todavia animando
		}

		boolean goingToTransform = !transformed;
		boolean skinReady = SkinLibrary.ensureReady();

		if (goingToTransform && !skinReady) {
			mc.player.displayClientMessage(Component.translatable("uitransform.msg.no_skin"), true);
			mc.setScreen(new dev.uitransform.client.screen.SkinConfigScreen(null));
			return;
		}

		target = goingToTransform;
		shellActive = true;
		shellStart = now;
		shellDuration = goingToTransform ? SHELL_TRANSFORM_MS : SHELL_REVERT_MS;
		burstAt = -1L;
		whiteSkin = false;
	}

	public static void tick(Minecraft mc) {
		LocalPlayer player = mc.player;
		ClientLevel level = mc.level;

		if (player == null || level == null) {
			reset();
			return;
		}

		long now = System.currentTimeMillis();

		if (transformed) {
			SkinLibrary.ensureReady(); // si cambias de skin en el menu, se actualiza al instante
		}

		if (shellActive) {
			float p = shellProgress(now);

			if (!whiteSkin && p >= 0.72f) {
				whiteSkin = true;
			}

			spawnAmbient(player, level, p);

			if (now >= shellStart + shellDuration) {
				shellActive = false;
				whiteSkin = false;
				transformed = target;
				burstAt = now;
				spawnBurst(player, level);
			}
		} else if (burstAt >= 0 && now - burstAt > AFTER_MS) {
			burstAt = -1L;
		}
	}

	public static void reset() {
		transformed = false;
		whiteSkin = false;
		shellActive = false;
		burstAt = -1L;
	}

	// -------------------------------------------------------------------- skin

	/** Devuelve la skin modificada para el jugador local, o null si no hay que cambiar nada. */
	public static PlayerSkin override(PlayerSkin original) {
		PlayerSkin.Patch patch = null;

		if (whiteSkin) {
			patch = SkinLibrary.whitePatch();
		} else if (transformed) {
			patch = SkinLibrary.customPatch();
		}

		if (patch == null || original == null) {
			return null;
		}

		if (original == cacheOriginal && patch == cachePatch) {
			return cacheResult;
		}

		PlayerSkin result = original.with(patch);
		cacheOriginal = original;
		cachePatch = patch;
		cacheResult = result;
		return result;
	}

	/** True mientras la mano debe brillar a maxima luz. */
	public static boolean isGlowing() {
		return whiteSkin;
	}

	// ------------------------------------------------------------------ visuales

	private static float shellProgress(long now) {
		return clamp((now - shellStart) / (float) shellDuration);
	}

	public static Shell shell(long now) {
		float pulse = (float) (Math.sin(now * 0.018) * 0.5 + 0.5);

		if (shellActive) {
			float p = shellProgress(now);
			float ease = smooth(0.05f, 0.70f, p);
			int coreRgb = lerpRgb(0x4FC3FF, 0xFFFFFF, smooth(0.0f, 0.8f, p));
			int haloRgb = lerpRgb(0x2E9BFF, 0xE6F7FF, smooth(0.0f, 0.9f, p));
			float coreScale = 1.03f + 0.012f * pulse;
			float haloAlpha = (0.14f + 0.34f * p) * (0.8f + 0.2f * pulse);
			float haloScale = 1.10f + 0.12f * p + 0.03f * pulse;
			return new Shell(ease, coreRgb, coreScale, haloAlpha, haloRgb, haloScale);
		}

		if (burstAt >= 0) {
			float a = clamp((now - burstAt) / (float) AFTER_MS);
			float fade = (1.0f - a) * (1.0f - a);
			// El blanco que te cubria sale despedido: una cascara que se agranda y desaparece.
			return new Shell(0.0f, 0xFFFFFF, 1.0f, 0.85f * fade, 0xFFFFFF, 1.05f + 0.9f * a);
		}

		return null;
	}

	/** Brillo de pantalla completa en el momento del estallido (se ve en 1a y 3a persona). */
	private static float flashAlpha(long now) {
		float flash = 0.0f;

		if (burstAt >= 0) {
			float a = (now - burstAt) / (float) FLASH_MS;

			if (a < 1.0f) {
				flash = 0.9f * (1.0f - a) * (1.0f - a);
			}
		}

		if (shellActive) {
			flash = Math.max(flash, 0.35f * smooth(0.9f, 1.0f, shellProgress(now)));
		}

		return flash;
	}

	/** Bordes blancos que se cierran alrededor de la pantalla mientras te cubre la luz (solo 1a persona). */
	private static float vignetteAlpha(long now) {
		return shellActive ? 0.5f * smooth(0.1f, 1.0f, shellProgress(now)) : 0.0f;
	}

	public static void renderOverlay(GuiGraphics graphics) {
		long now = System.currentTimeMillis();
		float flash = flashAlpha(now);
		float vignette = vignetteAlpha(now);

		if (flash <= 0.01f && vignette <= 0.01f) {
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		int w = mc.getWindow().getGuiScaledWidth();
		int h = mc.getWindow().getGuiScaledHeight();

		if (vignette > 0.01f && mc.options.getCameraType().isFirstPerson()) {
			int rings = 8;
			int thickness = Math.max(2, h / 36);

			for (int i = 0; i < rings; i++) {
				float falloff = 1.0f - i / (float) rings;
				int color = argb(vignette * falloff * falloff, 0xDDF4FF);
				int inset = i * thickness;
				graphics.fill(inset, inset, w - inset, inset + thickness, color);
				graphics.fill(inset, h - inset - thickness, w - inset, h - inset, color);
				graphics.fill(inset, inset + thickness, inset + thickness, h - inset - thickness, color);
				graphics.fill(w - inset - thickness, inset + thickness, w - inset, h - inset - thickness, color);
			}
		}

		if (flash > 0.01f) {
			graphics.fill(0, 0, w, h, argb(flash, 0xFFFFFF));
		}
	}

	// ----------------------------------------------------------------- particulas

	/** Chispas emisivas que suben alrededor de tu cuerpo mientras te cubre la luz. */
	private static void spawnAmbient(LocalPlayer player, ClientLevel level, float p) {
		ThreadLocalRandom rnd = ThreadLocalRandom.current();
		int count = Math.round((1.0f + 5.0f * p) * UiConfig.get().particles.multiplier);
		double height = player.getBbHeight();

		for (int i = 0; i < count; i++) {
			double angle = rnd.nextDouble() * Math.PI * 2.0;
			double radius = 0.35 + rnd.nextDouble() * 0.25;
			double x = player.getX() + Math.cos(angle) * radius;
			double z = player.getZ() + Math.sin(angle) * radius;
			double y = player.getY() + rnd.nextDouble() * height;
			level.addParticle(ParticleTypes.END_ROD, x, y, z,
					(rnd.nextDouble() - 0.5) * 0.02, 0.03 + rnd.nextDouble() * 0.06, (rnd.nextDouble() - 0.5) * 0.02);
		}
	}

	/** El blanco se destruye: cientos de particulas emisivas salen volando desde todo tu cuerpo. */
	private static void spawnBurst(LocalPlayer player, ClientLevel level) {
		ThreadLocalRandom rnd = ThreadLocalRandom.current();
		int count = Math.round(260 * UiConfig.get().particles.multiplier);
		double height = player.getBbHeight();

		for (int i = 0; i < count; i++) {
			double angle = rnd.nextDouble() * Math.PI * 2.0;
			double heightFraction = rnd.nextDouble();
			double radius = 0.2 + rnd.nextDouble() * 0.15;
			double x = player.getX() + Math.cos(angle) * radius;
			double z = player.getZ() + Math.sin(angle) * radius;
			double y = player.getY() + heightFraction * height;
			double speed = 0.12 + rnd.nextDouble() * 0.30;
			double vx = Math.cos(angle) * speed + (rnd.nextDouble() - 0.5) * 0.05;
			double vz = Math.sin(angle) * speed + (rnd.nextDouble() - 0.5) * 0.05;
			double vy = (rnd.nextDouble() - 0.35) * 0.25 + (heightFraction - 0.5) * 0.10;
			level.addParticle(ParticleTypes.END_ROD, x, y, z, vx, vy, vz);
		}
	}

	// ------------------------------------------------------------------ utilidades

	public static int argb(float alpha, int rgb) {
		int a = Math.round(clamp(alpha) * 255.0f);
		return (a << 24) | (rgb & 0xFFFFFF);
	}

	private static float clamp(float v) {
		return Math.max(0.0f, Math.min(1.0f, v));
	}

	private static float smooth(float edge0, float edge1, float x) {
		float t = clamp((x - edge0) / (edge1 - edge0));
		return t * t * (3.0f - 2.0f * t);
	}

	private static int lerpRgb(int from, int to, float t) {
		int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
		int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
		int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
		return (r << 16) | (g << 8) | b;
	}
}
