package de.tomalbrc.shaderfx.polymer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.kyori.adventure.util.TriState;

public record FontProviderFilter(net.kyori.adventure.util.TriState uniform, TriState jp) {
    public static final FontProviderFilter DEFAULT = new FontProviderFilter(TriState.NOT_SET, TriState.NOT_SET);
    public static final FontProviderFilter UNIFORM = new FontProviderFilter(TriState.TRUE, TriState.NOT_SET);
    public static final FontProviderFilter JP = new FontProviderFilter(TriState.NOT_SET, TriState.TRUE);
    public static final FontProviderFilter UNIFORM_JP = new FontProviderFilter(TriState.TRUE, TriState.TRUE);

    public static final Codec<FontProviderFilter> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.xmap(x -> x ? TriState.TRUE : TriState.FALSE, x -> x.toBooleanOrElse(true)).optionalFieldOf("uniform", TriState.NOT_SET).forGetter(FontProviderFilter::uniform),
            Codec.BOOL.xmap(x -> x ? TriState.TRUE : TriState.FALSE, x -> x.toBooleanOrElse(true)).optionalFieldOf("jp", TriState.NOT_SET).forGetter(FontProviderFilter::jp)
    ).apply(instance, FontProviderFilter::new));
}