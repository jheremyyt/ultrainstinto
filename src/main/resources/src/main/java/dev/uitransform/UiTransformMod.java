package dev.uitransform;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UiTransformMod implements ModInitializer {
	public static final String MOD_ID = "uitransform";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Ultra Instinct Transformation cargado (solo cliente).");
	}
}
