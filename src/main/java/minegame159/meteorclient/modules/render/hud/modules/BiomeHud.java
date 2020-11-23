/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.render.hud.modules;

import minegame159.meteorclient.modules.render.hud.HUD;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

public class BiomeHud extends DoubleTextHudModule {
    private final BlockPos.Mutable blockPos = new BlockPos.Mutable();

    public BiomeHud(HUD hud) {
        super(hud, "biome", "Displays the biome you are in.", "Biome: ");
    }

    @Override
    protected String getRight() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return "";

        blockPos.set(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        return Arrays.stream(mc.world.getBiome(blockPos).getCategory().getName().split("_")).map(StringUtils::capitalize).collect(Collectors.joining(" "));
    }
}
