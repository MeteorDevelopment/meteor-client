/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.commands.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import meteordevelopment.meteorclient.mixin.MapRendererAccessor;
import meteordevelopment.meteorclient.systems.commands.Command;
import net.minecraft.client.render.MapRenderer;
import net.minecraft.command.CommandSource;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.text.Text;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

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

            MapState state = getMapState();
            if (state == null) throw MAP_NOT_FOUND.create();
            ItemStack map = getMap();

            String path = getPath();
            if (path == null) throw OOPS.create();

            saveMap(map, state, path, 128);

            return SINGLE_SUCCESS;
        }).then(argument("scale", IntegerArgumentType.integer()).executes(context -> {
            int scale = IntegerArgumentType.getInteger(context, "scale");

            MapState state = getMapState();
            if (state == null) throw MAP_NOT_FOUND.create();
            ItemStack map = getMap();

            String path = getPath();
            if (path == null) throw OOPS.create();

            saveMap(map, state, path, scale);

            return SINGLE_SUCCESS;
        }));
    }

    private void saveMap(ItemStack map, MapState state, String path, int scale) {
        //this is horrible code but it somehow works

        MapRenderer mapRenderer = mc.gameRenderer.getMapRenderer();
        MapRenderer.MapTexture texture = ((MapRendererAccessor) mapRenderer).invokeGetMapTexture(FilledMapItem.getMapId(map), state);

        int[] data = texture.texture.getImage().makePixelArray();
        BufferedImage image = new BufferedImage(128, 128, 2);
        image.setRGB(0, 0, image.getWidth(), image.getHeight(), data, 0, 128);

        BufferedImage scaledImage = new BufferedImage(scale, scale, 2);
        if (scale != 128) {
            Graphics2D g = scaledImage.createGraphics();
            g.setComposite(AlphaComposite.Src);
            g.drawImage(image, 0, 0, scale, scale, null);
            g.dispose();
        }

        try {
            ImageIO.write((scale == 128 ? image : scaledImage), "png", new File(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private MapState getMapState() {
        ItemStack map = getMap();
        if (map == null) return null;

        MapState state = FilledMapItem.getMapState(FilledMapItem.getMapId(map), mc.world);
        if (state == null) return null;

        return state;
    }

    private String getPath() {
        String path = TinyFileDialogs.tinyfd_saveFileDialog("Save image", null, filters, null);
        if (path == null) return null;
        if (!path.endsWith(".png")) path += ".png";

        return path;
    }

    private ItemStack getMap() {
        ItemStack itemStack = mc.player.getMainHandStack();
        if (itemStack.getItem() == Items.FILLED_MAP) return itemStack;

        itemStack = mc.player.getOffHandStack();
        if (itemStack.getItem() == Items.FILLED_MAP) return itemStack;

        return null;
    }
}
