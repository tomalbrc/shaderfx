package de.tomalbrc.shaderfx.polymer;

import com.mojang.serialization.MapCodec;

public interface FontProvider {
    MapCodec<FontProvider> CODEC = BitmapProvider.CODEC.xmap(
            provider -> provider,
            provider -> (BitmapProvider) provider
    );

    MapCodec<? extends FontProvider> codec();

    interface Builder {
        FontProvider build();
    }

}