/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows the contents of containers without opening them.
 *
 * <p>When the player interacts with a container, the server answers with an open screen packet
 * followed by the contents of the container. Both packets are swallowed here, so the screen never
 * opens, and the contents are rendered instead. The container is closed right away, so the chest
 * is not locked for anybody else.
 */
public class ChestPreview extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<List<Block>> containers = sgGeneral.add(new BlockListSetting.Builder()
        .name("containers")
        .description("Blocks whose contents are previewed.")
        .defaultValue(Blocks.CHEST, Blocks.BARREL, Blocks.SHULKER_BOX)
        .build()
    );

    private final Setting<Integer> expire = sgGeneral.add(new IntSetting.Builder()
        .name("expire")
        .description("Seconds until a preview is removed.")
        .defaultValue(10)
        .min(1)
        .max(60)
        .build()
    );

    private final Setting<Mode> mode = sgRender.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("Where the contents are shown.")
        .defaultValue(Mode.World)
        .build()
    );

    private final Setting<Double> scale = sgRender.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Scale of the preview.")
        .defaultValue(1)
        .min(0.25)
        .max(4)
        .build()
    );

    private final Setting<Boolean> showCount = sgRender.add(new BoolSetting.Builder()
        .name("show-count")
        .description("Shows how many of each item there are.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showEmpty = sgRender.add(new BoolSetting.Builder()
        .name("show-empty")
        .description("Shows containers that are empty.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> maxItems = sgRender.add(new IntSetting.Builder()
        .name("max-items")
        .description("Maximum amount of items that are shown.")
        .defaultValue(54)
        .min(1)
        .max(54)
        .build()
    );

    private final Setting<Integer> maxDistance = sgRender.add(new IntSetting.Builder()
        .name("max-distance")
        .description("Maximum distance the preview is shown at.")
        .defaultValue(32)
        .min(1)
        .max(128)
        .build()
    );

    /** The container that is currently being peeked at, -1 when there is none. */
    private int containerId = -1;
    /** The block the player interacted with last. */
    private BlockPos interacted;
    /** When the player interacted with that block. */
    private long interactedAt;
    /** When the container that is being peeked at was opened. */
    private long openedAt;
    /** The block the preview belongs to. */
    private BlockPos pos;
    /** The items of the preview. */
    private List<ItemStack> items = List.of();
    /** When the preview was received. */
    private long timestamp;

    private final List<ItemStack> visible = new ArrayList<>();
    private final Vector3d renderPos = new Vector3d();

    public ChestPreview() {
        super(Categories.Misc, "chest-preview", "Shows the contents of containers without opening them.");
    }

    @EventHandler
    private void onInteractBlock(InteractBlockEvent event) {
        interacted = event.result.getBlockPos();
        interactedAt = System.currentTimeMillis();
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (event.packet instanceof ClientboundOpenScreenPacket packet) {
            if (containerId != -1) {
                // the container was never filled, do not wait for it forever
                if (System.currentTimeMillis() - openedAt < 2000) return;

                containerId = -1;
            }
            if (!peekable(packet.getType())) return;

            BlockPos block = interacted;
            if (block == null || mc.level == null) return;
            if (System.currentTimeMillis() - interactedAt > 1000) return;

            BlockState state = mc.level.getBlockState(block);
            if (!containers.get().contains(state.getBlock())) return;

            containerId = packet.getContainerId();
            openedAt = System.currentTimeMillis();
            pos = block;

            // the screen must not open
            event.cancel();
        }
        else if (event.packet instanceof ClientboundContainerSetContentPacket packet) {
            if (packet.containerId() != containerId) return;

            items = packet.items();
            timestamp = System.currentTimeMillis();
            containerId = -1;

            // release the container on the server, it was opened for the preview only
            if (mc.getConnection() != null) {
                mc.getConnection().send(new ServerboundContainerClosePacket(packet.containerId()));
            }

            event.cancel();
        }
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (pos == null || mc.level == null) return;
        if (System.currentTimeMillis() - timestamp > expire.get() * 1000L) return;

        visible.clear();
        for (ItemStack stack : items) {
            if (stack.isEmpty()) continue;
            if (visible.size() >= maxItems.get()) break;

            visible.add(stack);
        }

        if (visible.isEmpty() && !showEmpty.get()) {
            items = List.of();
            return;
        }

        if (mode.get() == Mode.World) renderWorld(event);
        else renderScreen(event);
    }

    /** Renders the contents above the container. */
    private void renderWorld(Render2DEvent event) {
        if (mc.player == null) return;
        if (mc.player.distanceToSqr(Vec3.atCenterOf(pos)) > (double) maxDistance.get() * maxDistance.get()) return;

        renderPos.set(Vec3.atCenterOf(pos).x, pos.getY() + 1, Vec3.atCenterOf(pos).z);
        if (!NametagUtils.to2D(renderPos, scale.get())) return;

        int columns = Math.max(1, Math.min(9, visible.size()));
        int rows = (visible.size() + columns - 1) / columns;
        double width = columns * 16.0;
        double height = rows * 16.0;

        NametagUtils.begin(renderPos, event.graphics);

        if (visible.isEmpty()) {
            event.graphics.text(mc.font, "Empty", (int) -mc.font.width("Empty") / 2, -8, 0xFFFFFFFF);
            NametagUtils.end(event.graphics);
            return;
        }

        for (int i = 0; i < visible.size(); i++) {
            int x = (int) (-width / 2 + (i % columns) * 16);
            int y = (int) (-height + (i / columns) * 16);

            event.graphics.item(visible.get(i), x, y);
            if (showCount.get()) event.graphics.itemDecorations(mc.font, visible.get(i), x, y, null);
        }

        NametagUtils.end(event.graphics);
    }

    /** Renders the contents in a panel at the top of the screen. */
    private void renderScreen(Render2DEvent event) {
        int columns = Math.min(9, Math.max(visible.size(), 1));
        int rows = (visible.size() + columns - 1) / columns;
        int width = columns * 16;
        int height = rows * 16;
        int x = (event.screenWidth - width) / 2;

        if (visible.isEmpty()) {
            event.graphics.text(mc.font, "Empty", x, 4, 0xFFFFFFFF);
            return;
        }

        for (int i = 0; i < visible.size(); i++) {
            int itemX = x + (i % columns) * 16;
            int itemY = 4 + (i / columns) * 16;

            event.graphics.item(visible.get(i), itemX, itemY);
            if (showCount.get()) event.graphics.itemDecorations(mc.font, visible.get(i), itemX, itemY, null);
        }
    }

    /** Only the generic container menus are peeked at: single and double chests. */
    private boolean peekable(MenuType<?> type) {
        return type == MenuType.GENERIC_9x3 || type == MenuType.GENERIC_9x6;
    }

    @Override
    public String getInfoString() {
        return items.isEmpty() ? null : Integer.toString(items.size());
    }

    public enum Mode {
        World,
        Screen
    }
}
