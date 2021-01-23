/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.player;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.entity.player.StartBreakingBlockEvent;
import minegame159.meteorclient.events.render.RenderEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.rendering.Renderer;
import minegame159.meteorclient.rendering.ShapeMode;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.misc.Pool;
import minegame159.meteorclient.utils.render.color.SettingColor;
import net.minecraft.block.Block;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

public class PacketMine extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General

    private final Setting<Boolean> oneByOne = sgGeneral.add(new BoolSetting.Builder()
            .name("one-by-one")
            .description("Mines blocks one by one.")
            .defaultValue(true)
            .build()
    );

    // Render

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
            .name("render")
            .description("Whether to render blocks being mined.")
            .defaultValue(true)
            .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side-color")
            .description("The color of the sides of the blocks being rendered.")
            .defaultValue(new SettingColor(204, 0, 0, 10))
            .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The color of the lines of the blocks being rendered.")
            .defaultValue(new SettingColor(204, 0, 0, 255))
            .build()
    );

    private final Pool<MyBlock> blockPool = new Pool<>(MyBlock::new);
    private final List<MyBlock> blocks = new ArrayList<>();

    public PacketMine() {
        super(Category.Player, "packet-mine", "Sends packets to mine blocks without the mining animation.");
    }

    @Override
    public void onDeactivate() {
        for (MyBlock block : blocks) blockPool.free(block);
        blocks.clear();
    }

    private boolean isMiningBlock(BlockPos pos) {
        for (MyBlock block : blocks) {
            if (block.blockPos.equals(pos)) return true;
        }

        return false;
    }

    @EventHandler
    private final Listener<StartBreakingBlockEvent> onStartBreakingBlock = new Listener<>(event -> {
        if (mc.world.getBlockState(event.blockPos).getHardness(mc.world, event.blockPos) < 0) return;

        if (!isMiningBlock(event.blockPos)) {
            MyBlock block = blockPool.get();
            block.blockPos = event.blockPos;
            block.direction = event.direction;
            block.originalBlock = mc.world.getBlockState(block.blockPos).getBlock();
            blocks.add(block);
        }

        event.cancel();
    });

    @EventHandler
    private final Listener<TickEvent.Post> onTick = new Listener<>(event -> {
        blocks.removeIf(MyBlock::shouldRemove);

        if (oneByOne.get()) {
            if (!blocks.isEmpty()) blocks.get(0).mine();
        }
        else blocks.forEach(MyBlock::mine);
    });

    @EventHandler
    private final Listener<RenderEvent> onRender = new Listener<>(event -> {
        if (render.get()) {
            for (MyBlock block : blocks) block.render();
        }
    });

    private class MyBlock {
        public BlockPos blockPos;
        public Direction direction;
        public Block originalBlock;

        public boolean shouldRemove() {
            return mc.world.getBlockState(blockPos).getBlock() != originalBlock || Utils.distance(mc.player.getX() - 0.5, mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ() - 0.5, blockPos.getX() + direction.getOffsetX(), blockPos.getY() + direction.getOffsetY(), blockPos.getZ() + direction.getOffsetZ()) > mc.interactionManager.getReachDistance();
        }

        public void mine() {
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction));
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction));
        }

        public void render() {
            Renderer.boxWithLines(Renderer.NORMAL, Renderer.LINES, blockPos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }
    }
}
