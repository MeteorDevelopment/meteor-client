/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.player;

import java.util.List;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.block.Block;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class GhostHand extends Module {
	
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    
    public final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
        .name("blocks")
        .description("What blocks should be emptied.")
        .build()
    );
    
    public GhostHand() {
        super(Categories.Player, "ghost-hand", "Interact blocks through walls.");
    }

    public boolean inBlockList(Block block)
    {
        return isActive() && blocks.get().contains(block);
    }
}
