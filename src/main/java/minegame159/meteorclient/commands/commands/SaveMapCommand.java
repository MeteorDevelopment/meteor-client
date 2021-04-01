/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import minegame159.meteorclient.commands.Command;
import net.minecraft.block.MaterialColor;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.command.CommandSource;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.text.LiteralText;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class SaveMapCommand extends Command {
    private static final SimpleCommandExceptionType MAP_NOT_FOUND = new SimpleCommandExceptionType(new LiteralText("You must be holding a filled map."));
    private static final SimpleCommandExceptionType OOPS = new SimpleCommandExceptionType(new LiteralText("Something went wrong."));

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
            ItemStack map = getMap();
            if (map == null) throw MAP_NOT_FOUND.create();

            MapState state = FilledMapItem.getMapState(map, mc.world);
            if (state == null) throw MAP_NOT_FOUND.create();

            String path = TinyFileDialogs.tinyfd_saveFileDialog("Save image", null, filters, null);
            if (path == null) throw OOPS.create();
            if (!path.endsWith(".png")) path += ".png";

            NativeImage image = new NativeImage(128, 128, true);

            for (int x = 0; x < 128; x++) {
                for (int y = 0; y < 128; y++) {
                    int c = state.colors[x + y * 128] & 255;

                    if (c / 4 == 0) image.setPixelColor(x, y, 0);
                    else image.setPixelColor(x, y, MaterialColor.COLORS[c / 4].getRenderColor(c & 3));
                }
            }

            try {
                image.writeFile(new File(path));
                image.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return SINGLE_SUCCESS;
        });
    }

    private ItemStack getMap() {
        ItemStack itemStack = mc.player.getMainHandStack();
        if (itemStack.getItem() == Items.FILLED_MAP) return itemStack;

        itemStack = mc.player.getOffHandStack();
        if (itemStack.getItem() == Items.FILLED_MAP) return itemStack;

        return null;
    }
}
