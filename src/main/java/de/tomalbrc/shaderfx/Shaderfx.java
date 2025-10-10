package de.tomalbrc.shaderfx;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import de.tomalbrc.shaderfx.api.ShaderEffect;
import de.tomalbrc.shaderfx.api.ShaderEffects;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.resourcepack.extras.api.format.font.BitmapProvider;
import eu.pb4.polymer.resourcepack.extras.api.format.font.FontAsset;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Shaderfx implements ModInitializer {
    public static final String MODID = "shaderfx";

    @Override
    public void onInitialize() {
        PolymerResourcePackUtils.addModAssets(MODID);

        ShaderEffects.addImport(ResourceLocation.withDefaultNamespace("shaderfx_utils.glsl"));
        ShaderEffects.addImport(ResourceLocation.withDefaultNamespace("spikes.glsl"));
        ShaderEffects.addImport(ResourceLocation.withDefaultNamespace("fractal1.glsl"));
        ShaderEffects.addImport(ResourceLocation.withDefaultNamespace("fractal2.glsl"));
        ShaderEffects.addImport(ResourceLocation.withDefaultNamespace("aperture.glsl"));

        PolymerResourcePackUtils.RESOURCE_PACK_CREATION_EVENT.register(builder -> {
            String fragmentShader = loadCore("rendertype_text.fsh");
            StringBuilder importList = new StringBuilder();
            for (ResourceLocation location : ShaderEffects.IMPORTS) {
                importList.append(String.format("#moj_import <%s>\n", location));
            }
            fragmentShader = fragmentShader.replace("//%IMPORTS%", importList);

            List<String> chars = new ArrayList<>();
            StringBuilder stringBuilder = new StringBuilder();

            StringBuilder shaderCases = new StringBuilder();
            var img = new BufferedImage(ShaderEffects.EFFECTS.size(), 1, BufferedImage.TYPE_INT_ARGB);
            for (Map.Entry<ResourceLocation, ShaderEffect> entry : ShaderEffects.EFFECTS.entrySet()) {
                var effect = entry.getValue();

                stringBuilder.append(effect.asChar());

                img.setRGB(effect.id(), 0, effect.asColor());

                shaderCases.append(wrapCase(effect.snippet(), effect.id()));
            }
            fragmentShader = fragmentShader.replace("//%CASES%", shaderCases);

            chars.add(stringBuilder.toString());

            FontAsset.Builder fontAsset = FontAsset.builder();
            fontAsset.add(new BitmapProvider(ResourceLocation.fromNamespaceAndPath(MODID, String.format("font/%s.png", ShaderEffects.FONT.id().getPath())), chars, 0));

            var out = new ByteArrayOutputStream();
            try {
                ImageIO.write(img, "PNG", out);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            builder.addData(String.format("assets/%s/font/%s.json", ShaderEffects.FONT.id().getNamespace(), ShaderEffects.FONT.id().getPath()), fontAsset.build().toBytes());
            builder.addData(String.format("assets/%s/textures/font/%s.png", ShaderEffects.FONT.id().getNamespace(), ShaderEffects.FONT.id().getPath()), out.toByteArray());
            builder.addData("assets/minecraft/shaders/core/rendertype_text.fsh", fragmentShader.getBytes(StandardCharsets.UTF_8));
        });

        final SuggestionProvider<CommandSourceStack> SUGGESTER = (commandContext, suggestionsBuilder) -> SharedSuggestionProvider.suggest(ShaderEffects.EFFECTS.keySet().stream().map(ResourceLocation::toString), suggestionsBuilder);

        CommandRegistrationCallback.EVENT.register((dispatcher, commandBuildContext, commandSelection) -> {
            var cmd = Commands.literal("shaderfx").requires(x -> x.hasPermission(3)).then(Commands.literal("run").then(Commands.argument("id", ResourceLocationArgument.id()).suggests(SUGGESTER).then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("color", HexColorArgument.hexColor()).then(Commands.argument("fadeIn", IntegerArgumentType.integer(0)).then(Commands.argument("stay", IntegerArgumentType.integer(0)).then(Commands.argument("fadeOut", IntegerArgumentType.integer(0)).executes(x -> {
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

            dispatcher.register(cmd);
        });
    }

    public static String wrapCase(String snippet, int caseNum) {
        return String.format(
                """
                        case %d: {
                        %s
                        } break;
                        """, caseNum, snippet
        );
    }

    public static byte[] loadBytes(String snippet) {
        try (InputStream is = Shaderfx.class.getResourceAsStream("/shaders/" + snippet)) {
            return is.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String loadSnippet(String snippet) {
        var list = new String(loadBytes("snippets/" + snippet), StandardCharsets.UTF_8).split("\n");
        return String.join("\n", Arrays.copyOfRange(list, 1, list.length - 1));
    }

    public static String loadCore(String id) {
        return new String(loadBytes(id), StandardCharsets.UTF_8);
    }
}
