package dev.uitransform.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.uitransform.UiTransformMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Configuracion guardada en config/uitransform/config.json */
public final class UiConfig {
	public enum Arms {
		AUTO, WIDE, SLIM;

		public Arms next() {
			return values()[(ordinal() + 1) % values().length];
		}
	}

	public enum Particles {
		LOW(0.4f), NORMAL(1.0f), HIGH(1.8f);

		public final float multiplier;

		Particles(float multiplier) {
			this.multiplier = multiplier;
		}

		public Particles next() {
			return values()[(ordinal() + 1) % values().length];
		}
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static UiConfig instance;

	/** Nombre del archivo PNG (dentro de la carpeta de skins) que se usa al transformarte. */
	public String selectedSkin = "";
	public Arms arms = Arms.AUTO;
	public Particles particles = Particles.NORMAL;

	public static Path directory() {
		return FabricLoader.getInstance().getConfigDir().resolve(UiTransformMod.MOD_ID);
	}

	public static Path skinsDirectory() {
		return directory().resolve("skins");
	}

	public static UiConfig get() {
		if (instance == null) {
			load();
		}

		return instance;
	}

	public static void load() {
		Path file = directory().resolve("config.json");
		UiConfig loaded = null;

		if (Files.isRegularFile(file)) {
			try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
				loaded = GSON.fromJson(reader, UiConfig.class);
			} catch (Exception e) {
				UiTransformMod.LOGGER.warn("No se pudo leer la configuracion, se usan valores por defecto", e);
			}
		}

		if (loaded == null) {
			loaded = new UiConfig();
		}

		if (loaded.selectedSkin == null) loaded.selectedSkin = "";
		if (loaded.arms == null) loaded.arms = Arms.AUTO;
		if (loaded.particles == null) loaded.particles = Particles.NORMAL;

		instance = loaded;
	}

	public static void save() {
		try {
			Files.createDirectories(directory());

			try (Writer writer = Files.newBufferedWriter(directory().resolve("config.json"), StandardCharsets.UTF_8)) {
				GSON.toJson(get(), writer);
			}
		} catch (IOException e) {
			UiTransformMod.LOGGER.warn("No se pudo guardar la configuracion", e);
		}
	}
}
