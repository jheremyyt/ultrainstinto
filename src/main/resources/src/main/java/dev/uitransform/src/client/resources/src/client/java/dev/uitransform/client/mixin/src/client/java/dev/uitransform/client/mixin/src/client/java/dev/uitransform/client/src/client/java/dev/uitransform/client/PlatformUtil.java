package dev.uitransform.client;

import dev.uitransform.UiTransformMod;
import net.minecraft.util.Util;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Cosas que dependen del sistema: abrir el gestor de archivos (Android / Windows / Mac / Linux)
 * y elegir un PNG con el selector nativo en PC.
 * Todo esto es bloqueante: llamalo desde un hilo aparte, nunca desde el hilo del juego.
 */
public final class PlatformUtil {
	private PlatformUtil() {
	}

	/** path != null: archivo elegido. unavailable: no hay selector nativo disponible. Ambos falsos: cancelado. */
	public record PickResult(Path path, boolean unavailable) {
		static final PickResult CANCELLED = new PickResult(null, false);
		static final PickResult UNAVAILABLE = new PickResult(null, true);
	}

	private static final String OS = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);

	public static boolean isAndroid() {
		// Pojav, Zalith, FCL, etc. corren dentro de un proceso Android: estas variables siempre existen ahi.
		return System.getenv("ANDROID_ROOT") != null
				|| System.getenv("ANDROID_DATA") != null
				|| System.getenv("POJAV_RENDERER") != null
				|| Files.exists(Path.of("/system/build.prop"));
	}

	public static boolean isWindows() {
		return OS.contains("win");
	}

	public static boolean isMac() {
		return OS.contains("mac");
	}

	/** Abre el gestor de archivos del sistema en esa carpeta. Devuelve false si no
