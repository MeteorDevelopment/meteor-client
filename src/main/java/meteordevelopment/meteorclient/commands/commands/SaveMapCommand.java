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
import meteordevelopment.meteorclient.mixin.MapRendererAccessor;
import net.minecraft.client.render.MapRenderer;
import net.minecraft.command.CommandSource;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class SaveMapCommand extends Command {
    private static final SimpleCommandExceptionType MAP_NOT_FOUND = new SimpleCommandExceptionType(Text.literal("You must be holding a filled map."));
    private static final SimpleCommandExceptionType OOPS = new SimpleCommandExceptionType(Text.literal("Something went wrong."));

    private final PointerBuffer filters;

    public SaveMapCommand() {
        super("save-map", "Saves a map to an image.", "sm");

        filters = BufferUtils.createPointerBuffer(1);

        ByteBuffer pngFilter = MemoryUtil.memASCII("*.png");

        filters.put(pngFilter);
        filters.rewind();
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            saveMap(128);

            return SINGLE_SUCCESS;
        }).then(argument("scale", IntegerArgumentType.integer(1)).executes(context -> {
            saveMap(IntegerArgumentType.getInteger(context, "scale"));

            return SINGLE_SUCCESS;
        }));
    }

    private void saveMap(int scale) throws CommandSyntaxException {
        ItemStack map = getMap();
        MapState state = getMapState();
        if (map == null || state == null) throw MAP_NOT_FOUND.create();

        File path = getPath();
        if (path == null) throw OOPS.create();

        MapRenderer mapRenderer = mc.gameRenderer.getMapRenderer();
        MapRenderer.MapTexture texture = ((MapRendererAccessor) mapRenderer).invokeGetMapTexture(map.get(DataComponentTypes.MAP_ID), state);
        if (texture.texture.getImage() == null) throw OOPS.create();

        try {
            if (scale == 128) texture.texture.getImage().writeTo(path);
            else {
                int[] data = texture.texture.getImage().makePixelArray();
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
    }

    private @Nullable MapState getMapState() {
        ItemStack map = getMap();
        if (map == null) return null;

        return FilledMapItem.getMapState(map.get(DataComponentTypes.MAP_ID), mc.world);
    }

    private @Nullable File getPath() {
        String path = TinyFileDialogs.tinyfd_saveFileDialog("Save image", null, filters, null);
        if (path == null) return null;
        if (!path.endsWith(".png")) path += ".png";

        return new File(path);
    }

    private @Nullable ItemStack getMap() {
        ItemStack itemStack = mc.player.getMainHandStack();
        if (itemStack.getItem() == Items.FILLED_MAP) return itemStack;

        itemStack = mc.player.getOffHandStack();
        if (itemStack.getItem() == Items.FILLED_MAP) return itemStack;

        return null;
    }
}
