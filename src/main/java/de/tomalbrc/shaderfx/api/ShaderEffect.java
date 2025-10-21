package de.tomalbrc.shaderfx.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;

public class ShaderEffect {
    final ResourceLocation location;
    final int id;
    final String snippet;

    public ShaderEffect(ResourceLocation location, int id, String snippet) {
        this.location = location;
        this.id = id;
        this.snippet = snippet;
    }

    public ResourceLocation location() {
        return location;
    }

    public int id() {
        return this.id;
    }

    public String asChar() {
        return String.valueOf((char) (0xE100 + id()));
    }

    public int asFullscreenColor() {
        return FastColor.ARGB32.color(251, 0, 0, id());
    }

    public int asLocalEffectColor() {
        return FastColor.ARGB32.color(253, 0, 0, id());
    }

    public String snippet() {
        return this.snippet;
    }
}
