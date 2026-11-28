/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.commands.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.mixin.MapTextureManagerAccessor;
import meteordevelopment.meteorclient.utils.misc.FileDialogs;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.resources.MapTextureManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jspecify.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class SaveMapCommand extends Command {
    private static final SimpleCommandExceptionType MAP_NOT_FOUND = new SimpleCommandExceptionType(Component.literal("You must be holding a filled map."));
    private static final SimpleCommandExceptionType OOPS = new SimpleCommandExceptionType(Component.literal("Something went wrong."));

    public SaveMapCommand() {
        super("save-map", "Saves a map to an image.", "sm");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(_ -> {
            saveMap(128);

            return SINGLE_SUCCESS;
        }).then(argument("scale", IntegerArgumentType.integer(1)).executes(context -> {
            saveMap(IntegerArgumentType.getInteger(context, "scale"));

            return SINGLE_SUCCESS;
        }));
    }

    @SuppressWarnings("deprecation") // Use of NativeImage#makePixelArray
    private void saveMap(int scale) throws CommandSyntaxException {
        ItemStack map = getMap();
        MapItemSavedData state = getMapState();
        if (map == null || state == null) throw MAP_NOT_FOUND.create();

        FileDialogs.saveFile("png", selectedFile -> {
            String str = selectedFile.getPath();
            if (!str.endsWith(".png")) str += ".png";
            File path = new File(str);

            MapTextureManagerAccessor textureManager = (MapTextureManagerAccessor) mc.getMapTextureManager();
            MapTextureManager.MapInstance texture = textureManager.meteor$invokeGetOrCreateMapInstance(map.get(DataComponents.MAP_ID), state);
            if (texture.texture.getPixels() == null) return;

            try {
                if (scale == 128) texture.texture.getPixels().writeToFile(path);
                else {
                    int[] data = texture.texture.getPixels().makePixelArray();
                    BufferedImage image = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
                    image.setRGB(0, 0, image.getWidth(), image.getHeight(), data, 0, 128);

                    BufferedImage scaledImage = new BufferedImage(scale, scale, 2);
                    scaledImage.createGraphics().drawImage(image, 0, 0, scale, scale, null);

                    ImageIO.write(scaledImage, "png", path);
                }
            } catch (IOException e) {
                error("Error writing map texture");
                MeteorClient.LOG.error(e.toString());
            }
        });
    }

    private @Nullable MapItemSavedData getMapState() {
        ItemStack map = getMap();
        if (map == null) return null;

        return MapItem.getSavedData(map.get(DataComponents.MAP_ID), mc.level);
    }

    private @Nullable ItemStack getMap() {
        ItemStack itemStack = mc.player.getMainHandItem();
        if (itemStack.getItem() == Items.FILLED_MAP) return itemStack;

        itemStack = mc.player.getOffhandItem();
        if (itemStack.getItem() == Items.FILLED_MAP) return itemStack;

        return null;
    }
}
