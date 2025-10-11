package de.tomalbrc.shaderfx.api;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static de.tomalbrc.shaderfx.Shaderfx.MODID;
import static de.tomalbrc.shaderfx.api.FileUtil.loadSnippet;

public class ShaderEffects {
    public static final FontDescription.Resource FONT = new FontDescription.Resource(ResourceLocation.fromNamespaceAndPath(MODID, "fx"));
    public static final FontDescription.Resource FONT_LOCAL = new FontDescription.Resource(ResourceLocation.fromNamespaceAndPath(MODID, "fx_local"));
    public static final Map<ResourceLocation, ShaderEffect> EFFECTS = new Object2ObjectArrayMap<>();

    public static final List<ResourceLocation> IMPORTS = new ArrayList<>();

    public static void addImport(ResourceLocation mojimport) {
        IMPORTS.add(mojimport);
    }

    public static MutableComponent effectComponent(ResourceLocation effectId, int color) {
        return Component.literal(EFFECTS.get(effectId).asChar()).withStyle(Style.EMPTY.withFont(FONT).withColor(color).withShadowColor(0));
    }

    public static MutableComponent effectComponentLocal(ResourceLocation effectId, int color) {
        return Component.literal(EFFECTS.get(effectId).asChar()).withStyle(Style.EMPTY.withFont(FONT_LOCAL).withColor(color).withShadowColor(0));
    }

    public static MutableComponent effectComponentLocal(ResourceLocation effectId) {
        return effectComponentLocal(effectId, 0xFF_FF_FF);
    }

    public static MutableComponent effectComponent(ResourceLocation effectId) {
        return effectComponent(effectId, 0xFF_FF_FF);
    }

    public static ShaderEffect effect(ResourceLocation resourceLocation) {
        return EFFECTS.get(resourceLocation);
    }

    public static ShaderEffect register(ResourceLocation id, String snippet, boolean addFont) {
        ShaderEffect effect = new ShaderEffect(EFFECTS.size(), snippet);
        EFFECTS.put(id, effect);
        return effect;
    }

    public static ShaderEffect register(ResourceLocation id, String snippet) {
        return register(id, snippet, true);
    }

    public static final ShaderEffect CIRCLE = ShaderEffects.register(ResourceLocation.fromNamespaceAndPath(MODID, "circle"), loadSnippet("circle.glsl"));
    public static final ShaderEffect FADE = ShaderEffects.register(ResourceLocation.fromNamespaceAndPath(MODID, "fade"), loadSnippet("color_fade.glsl"));
    public static final ShaderEffect DIRECTIONAL_GRID = ShaderEffects.register(ResourceLocation.fromNamespaceAndPath(MODID, "directional_grid"), loadSnippet("dir_grid_impl.glsl"));
    public static final ShaderEffect NOISE_GRID = ShaderEffects.register(ResourceLocation.fromNamespaceAndPath(MODID, "noise_grid"), loadSnippet("noise_grid_impl.glsl"));
    public static final ShaderEffect FRACTAL1 = ShaderEffects.register(ResourceLocation.fromNamespaceAndPath(MODID, "fractal1"), loadSnippet("fractal1.glsl"));
    public static final ShaderEffect FRACTAL2 = ShaderEffects.register(ResourceLocation.fromNamespaceAndPath(MODID, "fractal2"), loadSnippet("fractal2.glsl"));
    public static final ShaderEffect APERTURE = ShaderEffects.register(ResourceLocation.fromNamespaceAndPath(MODID, "aperture"), loadSnippet("rot_impl.glsl"), false);
    public static final ShaderEffect SPIKE = ShaderEffects.register(ResourceLocation.fromNamespaceAndPath(MODID, "spike"), loadSnippet("spike_impl.glsl"));
    public static final ShaderEffect VIGNETTE = ShaderEffects.register(ResourceLocation.fromNamespaceAndPath(MODID, "vignette"), loadSnippet("vignette_impl.glsl"));
}
