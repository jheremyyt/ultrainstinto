package dev.uitransform.client;

import com.mojang.blaze3d.platform.NativeImage;
import dev.uitransform.UiTransformMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.ClientAsset;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Maneja la carpeta de skins, las importa y las convierte en texturas del juego.
 * Todo lo que toca texturas debe llamarse desde el hilo principal del juego.
 */
public final class SkinLibrary {
	private SkinLibrary() {
	}

	/** Textura blanca solida (el "caparazon" de luz). Tambien la usa la capa de brillo. */
	public static final Identifier WHITE_TEXTURE = Identifier.fromNamespaceAndPath(Ui
