package de.tomalbrc.shaderfx;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import de.tomalbrc.shaderfx.api.FileUtil;
import de.tomalbrc.shaderfx.api.ShaderEffect;
import de.tomalbrc.shaderfx.api.ShaderEffects;
import de.tomalbrc.shaderfx.api.ShaderUtil;
import eu.pb4.polymer.resourcepack.api.PackResource;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.resourcepack.extras.api.format.font.BitmapProvider;
import eu.pb4.polymer.resourcepack.extras.api.format.font.FontAsset;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.HexColorArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Shaderfx implements ModInitializer {
    public static final String MODID = "shaderfx";
    public static boolean ADD_LOCAL = false;

    public static boolean convertAnimoji = true;
    private static boolean addedAssets = false;

    public static void enableAnimojiConversion() {
        convertAnimoji = true;
    }

    public static void enableAssets() {
        if (addedAssets)
            return;

        addedAssets = true;

        PolymerResourcePackUtils.addModAssets(MODID);

        PolymerResourcePackUtils.RESOURCE_PACK_CREATION_EVENT.register(builder -> {
            builder.addResourceConverter((path, resource) -> {
                if (convertAnimoji && path.contains("/textures/font/") && path.endsWith("_animoji.png")) {
                    try {
                        BufferedImage image = ImageIO.read(new ByteArrayInputStream(resource.readAllBytes()));
                        image = ShaderUtil.addAnimatedEmojiMarker(image, 5);
                        var out = new ByteArrayOutputStream();
                        ImageIO.write(image, "PNG", out);
                        return PackResource.of(out.toByteArray());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else if (path.matches("assets/shaderfx/textures/font/transition-.*\\.png")) {
                    try {
                        BufferedImage image = ShaderUtil.tintEdges(ImageIO.read(new ByteArrayInputStream(resource.readAllBytes())), ShaderEffects.IMAGE_TRANSITION.asFullscreenColor());
                        var out = new ByteArrayOutputStream();
                        ImageIO.write(image, "PNG", out);
                        return PackResource.of(out.toByteArray());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                return resource;
            });

            String fragmentShader = FileUtil.loadCore("rendertype_text.fsh");
            StringBuilder importList = new StringBuilder();
            for (ResourceLocation location : ShaderEffects.IMPORTS) {
                importList.append(String.format("#moj_import <%s>\n", location));
            }
            fragmentShader = fragmentShader.replace("//%IMPORTS%", importList);

            List<String> chars = new ArrayList<>();
            StringBuilder stringBuilder = new StringBuilder();

            StringBuilder shaderCases = new StringBuilder();
            var img = createFullscreenFontImage(stringBuilder, shaderCases);
            var imgLocal = createLocalEffectFontImage();
            fragmentShader = fragmentShader.replace("//%CASES%", shaderCases);

            chars.add(stringBuilder.toString());

            FontAsset.Builder fontAsset = FontAsset.builder();
            String formatted = String.format("font/%s.png", ShaderEffects.FONT.id().getPath());
            fontAsset.add(new BitmapProvider(ResourceLocation.fromNamespaceAndPath(MODID, formatted), chars, 0));

            FontAsset.Builder fontAssetLocal = FontAsset.builder();
            String formatted2 = String.format("font/%s_local.png", ShaderEffects.FONT.id().getPath());
            fontAssetLocal.add(new BitmapProvider(ResourceLocation.fromNamespaceAndPath(MODID, formatted2), chars, 0));

            var out = new ByteArrayOutputStream();
            var outLocal = new ByteArrayOutputStream();
            try {
                ImageIO.write(img, "PNG", out);
                ImageIO.write(imgLocal, "PNG", outLocal);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            builder.addData(String.format("assets/%s/font/%s.json", ShaderEffects.FONT.id().getNamespace(), ShaderEffects.FONT.id().getPath()), fontAsset.build().toBytes());
            if (ADD_LOCAL) builder.addData(String.format("assets/%s/font/%s_local.json", ShaderEffects.FONT.id().getNamespace(), ShaderEffects.FONT.id().getPath()), fontAssetLocal.build().toBytes());
            builder.addData(String.format("assets/%s/textures/font/%s.png", ShaderEffects.FONT.id().getNamespace(), ShaderEffects.FONT.id().getPath()), out.toByteArray());
            if (ADD_LOCAL) builder.addData(String.format("assets/%s/textures/font/%s_local.png", ShaderEffects.FONT.id().getNamespace(), ShaderEffects.FONT.id().getPath()), outLocal.toByteArray());
            builder.addData("assets/minecraft/shaders/core/rendertype_text.fsh", fragmentShader.getBytes(StandardCharsets.UTF_8));
        });
    }

    private static @NotNull BufferedImage createFullscreenFontImage(StringBuilder stringBuilder, StringBuilder shaderCases) {
        var img = new BufferedImage(ShaderEffects.EFFECTS.size(), 1, BufferedImage.TYPE_INT_ARGB);
        for (Map.Entry<ResourceLocation, ShaderEffect> entry : ShaderEffects.EFFECTS.entrySet()) {
            var effect = entry.getValue();

            stringBuilder.append(effect.asChar());

            img.setRGB(effect.id(), 0, effect.asFullscreenColor());

            shaderCases.append(FileUtil.wrapCase(effect.snippet(), effect.id()));
        }
        return img;
    }

    private static @NotNull BufferedImage createLocalEffectFontImage() {
        var img = new BufferedImage(ShaderEffects.EFFECTS.size(), 1, BufferedImage.TYPE_INT_ARGB);
        for (Map.Entry<ResourceLocation, ShaderEffect> entry : ShaderEffects.EFFECTS.entrySet()) {
            var effect = entry.getValue();
            img.setRGB(effect.id(), 0, effect.asLocalEffectColor());
        }
        return img;
    }

    public static MinecraftServerAudiences ADVENTURE;

    public static MinecraftServerAudiences adventure() {
        if (ADVENTURE == null) {
            throw new IllegalStateException("Tried to access Adventure without a running server!");
        }
        return ADVENTURE;
    }

    @Override
    public void onInitialize() {
        ShaderEffects.addImport(ResourceLocation.withDefaultNamespace("shaderfx_utils.glsl"));
        ShaderEffects.addImport(ResourceLocation.withDefaultNamespace("spikes.glsl"));
        ShaderEffects.addImport(ResourceLocation.withDefaultNamespace("fractal1.glsl"));
        ShaderEffects.addImport(ResourceLocation.withDefaultNamespace("fractal2.glsl"));

        ServerLifecycleEvents.SERVER_STARTING.register(server -> ADVENTURE = MinecraftServerAudiences.of(server));

        final SuggestionProvider<CommandSourceStack> SUGGESTER = (commandContext, suggestionsBuilder) -> SharedSuggestionProvider.suggest(ShaderEffects.EFFECTS.keySet().stream().map(ResourceLocation::toString), suggestionsBuilder);
        CommandRegistrationCallback.EVENT.register((dispatcher, commandBuildContext, commandSelection) -> {
            dispatcher.register(commandFullscreen(SUGGESTER));
            dispatcher.register(commandLocal(SUGGESTER));
            dispatcher.register(commandCustom(SUGGESTER));
        });
    }

    private static LiteralArgumentBuilder<CommandSourceStack> commandFullscreen(SuggestionProvider<CommandSourceStack> suggestionProvider) {
        return Commands.literal("shaderfx").requires(x -> x.hasPermission(3)).then(Commands.literal("run").then(Commands.argument("id", ResourceLocationArgument.id()).suggests(suggestionProvider).then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("color", HexColorArgument.hexColor()).then(Commands.argument("fadeIn", IntegerArgumentType.integer(0)).then(Commands.argument("stay", IntegerArgumentType.integer(0)).then(Commands.argument("fadeOut", IntegerArgumentType.integer(0)).executes(x -> {
            int color = HexColorArgument.getHexColor(x, "color");
            ResourceLocation id = ResourceLocationArgument.getId(x, "id");
            ServerPlayer player = EntityArgument.getPlayer(x, "player");

            int fadeIn = IntegerArgumentType.getInteger(x, "fadeIn");
            int stay = IntegerArgumentType.getInteger(x, "stay");
            int fadeOut = IntegerArgumentType.getInteger(x, "fadeOut");

            var titlePacket = new ClientboundSetTitleTextPacket(ShaderEffects.effectComponent(id, color));
            var timesPacket = new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut);
            player.connection.send(new ClientboundBundlePacket(ImmutableList.of(timesPacket, titlePacket)));

            return Command.SINGLE_SUCCESS;
        })))).executes(x -> {
            int color = HexColorArgument.getHexColor(x, "color");
            ResourceLocation id = ResourceLocationArgument.getId(x, "id");
            ServerPlayer player = EntityArgument.getPlayer(x, "player");

            var titlePacket = new ClientboundSetTitleTextPacket(ShaderEffects.effectComponent(id, color));
            player.connection.send(titlePacket);

            return Command.SINGLE_SUCCESS;
        })).executes(x -> {
            ResourceLocation id = ResourceLocationArgument.getId(x, "id");
            ServerPlayer player = EntityArgument.getPlayer(x, "player");

            var titlePacket = new ClientboundSetTitleTextPacket(ShaderEffects.effectComponent(id));
            player.connection.send(titlePacket);

            return Command.SINGLE_SUCCESS;
        }))));
    }


    private static LiteralArgumentBuilder<CommandSourceStack> commandLocal(SuggestionProvider<CommandSourceStack> suggestionProvider) {
        return Commands.literal("shaderfx:local").requires(x -> x.hasPermission(3)).then(Commands.literal("run").then(Commands.argument("id", ResourceLocationArgument.id()).suggests(suggestionProvider).then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("color", HexColorArgument.hexColor()).then(Commands.argument("fadeIn", IntegerArgumentType.integer(0)).then(Commands.argument("stay", IntegerArgumentType.integer(0)).then(Commands.argument("fadeOut", IntegerArgumentType.integer(0)).executes(x -> {
            int color = HexColorArgument.getHexColor(x, "color");
            ResourceLocation id = ResourceLocationArgument.getId(x, "id");
            ServerPlayer player = EntityArgument.getPlayer(x, "player");

            int fadeIn = IntegerArgumentType.getInteger(x, "fadeIn");
            int stay = IntegerArgumentType.getInteger(x, "stay");
            int fadeOut = IntegerArgumentType.getInteger(x, "fadeOut");

            var titlePacket = new ClientboundSetTitleTextPacket(ShaderEffects.effectComponentLocal(id, color));
            var timesPacket = new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut);
            player.connection.send(new ClientboundBundlePacket(ImmutableList.of(timesPacket, titlePacket)));

            return Command.SINGLE_SUCCESS;
        })))).executes(x -> {
            int color = HexColorArgument.getHexColor(x, "color");
            ResourceLocation id = ResourceLocationArgument.getId(x, "id");
            ServerPlayer player = EntityArgument.getPlayer(x, "player");

            var titlePacket = new ClientboundSetTitleTextPacket(ShaderEffects.effectComponentLocal(id, color));
            player.connection.send(titlePacket);

            return Command.SINGLE_SUCCESS;
        })).executes(x -> {
            ResourceLocation id = ResourceLocationArgument.getId(x, "id");
            ServerPlayer player = EntityArgument.getPlayer(x, "player");

            var titlePacket = new ClientboundSetTitleTextPacket(ShaderEffects.effectComponentLocal(id));
            player.connection.send(titlePacket);

            return Command.SINGLE_SUCCESS;
        }))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> commandCustom(SuggestionProvider<CommandSourceStack> suggestionProvider) {
        return Commands.literal("shaderfx:custom").requires(x -> x.hasPermission(3)).then(Commands.literal("run").then(Commands.argument("id", ResourceLocationArgument.id()).suggests(suggestionProvider).then(Commands.argument("text", StringArgumentType.string()).then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("color", HexColorArgument.hexColor()).then(Commands.argument("fadeIn", IntegerArgumentType.integer(0)).then(Commands.argument("stay", IntegerArgumentType.integer(0)).then(Commands.argument("fadeOut", IntegerArgumentType.integer(0)).executes(x -> {
            int color = HexColorArgument.getHexColor(x, "color");
            ResourceLocation id = ResourceLocationArgument.getId(x, "id");
            ServerPlayer player = EntityArgument.getPlayer(x, "player");

            int fadeIn = IntegerArgumentType.getInteger(x, "fadeIn");
            int stay = IntegerArgumentType.getInteger(x, "stay");
            int fadeOut = IntegerArgumentType.getInteger(x, "fadeOut");

            var titlePacket = new ClientboundSetTitleTextPacket(Shaderfx.adventure().asNative(MiniMessage.miniMessage().deserialize(StringArgumentType.getString(x, "text"))));
            var timesPacket = new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut);
            player.connection.send(new ClientboundBundlePacket(ImmutableList.of(timesPacket, titlePacket)));

            return Command.SINGLE_SUCCESS;
        })))).executes(x -> {
            int color = HexColorArgument.getHexColor(x, "color");
            ResourceLocation id = ResourceLocationArgument.getId(x, "id");
            ServerPlayer player = EntityArgument.getPlayer(x, "player");

            var titlePacket = new ClientboundSetTitleTextPacket(ShaderEffects.effectComponentLocal(id, color));
            player.connection.send(titlePacket);

            return Command.SINGLE_SUCCESS;
        })).executes(x -> {
            ResourceLocation id = ResourceLocationArgument.getId(x, "id");
            ServerPlayer player = EntityArgument.getPlayer(x, "player");

            var titlePacket = new ClientboundSetTitleTextPacket(ShaderEffects.effectComponentLocal(id));
            player.connection.send(titlePacket);

            return Command.SINGLE_SUCCESS;
        })))));
    }
}
