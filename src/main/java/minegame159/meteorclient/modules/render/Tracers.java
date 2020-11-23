/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.render;

import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.RenderEvent;
import minegame159.meteorclient.friends.Friend;
import minegame159.meteorclient.friends.FriendManager;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.rendering.ShapeBuilder;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.Color;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class Tracers extends ToggleModule {
    public enum Target {
        Head,
        Body,
        Feet
    }

    public enum Mode {
        Simple,
        Stem
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAppearance = settings.createGroup("Appearance");
    private final SettingGroup sgColors = settings.createGroup("Colors");

    // General

    private final Setting<List<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
            .name("entites")
            .description("Select specific entities.")
            .defaultValue(new ArrayList<>(0))
            .build()
    );

    private final Setting<Boolean> storage = sgGeneral.add(new BoolSetting.Builder()
            .name("storage")
            .description("Display storage blocks.")
            .defaultValue(false)
            .build()
    );

    // Appearance

    private final Setting<Target> target = sgAppearance.add(new EnumSetting.Builder<Target>()
            .name("target")
            .description("Which body part to target.")
            .defaultValue(Target.Body)
            .build()
    );

    private final Setting<Mode> mode = sgAppearance.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("Rendering mode.")
            .defaultValue(Mode.Simple)
            .build()
    );

    // Colors

    private final Setting<Color> playersColor = sgColors.add(new ColorSetting.Builder()
            .name("players-colors")
            .description("Players color.")
            .defaultValue(new Color(205, 205, 205, 127))
            .build()
    );

    private final Setting<Color> animalsColor = sgColors.add(new ColorSetting.Builder()
            .name("animals-color")
            .description("Animals color.")
            .defaultValue(new Color(145, 255, 145, 127))
            .build()
    );

    private final Setting<Color> waterAnimalsColor = sgColors.add(new ColorSetting.Builder()
            .name("water-animals-color")
            .description("Water animals color.")
            .defaultValue(new Color(145, 145, 255, 127))
            .build()
    );

    private final Setting<Color> monstersColor = sgColors.add(new ColorSetting.Builder()
            .name("monsters-color")
            .description("Monsters color.")
            .defaultValue(new Color(255, 145, 145, 127))
            .build()
    );

    private final Setting<Color> ambientColor = sgColors.add(new ColorSetting.Builder()
            .name("ambient-color")
            .description("Ambient color.")
            .defaultValue(new Color(75, 75, 75, 127))
            .build()
    );

    private final Setting<Color> miscColor = sgColors.add(new ColorSetting.Builder()
            .name("misc-color")
            .description("Misc color.")
            .defaultValue(new Color(145, 145, 145, 127))
            .build()
    );

    private final Setting<Color> storageColor = sgColors.add(new ColorSetting.Builder()
            .name("storage-color")
            .description("Storage color.")
            .defaultValue(new Color(255, 160, 0, 127))
            .build()
    );

    private Vec3d vec1;
    private int count;

    public Tracers() {
        super(Category.Render, "tracers", "Displays lines to entities.");
    }

    private void render(Entity entity, Color color, RenderEvent event) {
        double x = entity.prevX + (entity.getX() - entity.prevX) * event.tickDelta;
        double y = entity.prevY + (entity.getY() - entity.prevY) * event.tickDelta;
        double z = entity.prevZ + (entity.getZ() - entity.prevZ) * event.tickDelta;

        double height = entity.getBoundingBox().maxY - entity.getBoundingBox().minY;

        if (target.get() == Target.Head) y += height;
        else if (target.get() == Target.Body) y += height / 2;

        ShapeBuilder.line(vec1.x - (mc.gameRenderer.getCamera().getPos().x - event.offsetX), vec1.y - (mc.gameRenderer.getCamera().getPos().y - event.offsetY), vec1.z - (mc.gameRenderer.getCamera().getPos().z - event.offsetZ), x, y, z, color);

        if (mode.get() == Mode.Stem) ShapeBuilder.line(x, entity.getY(), z, x, entity.getY() + height, z, color);

        count++;
    }

    private void render(BlockEntity blockEntity, RenderEvent event) {
        BlockPos pos = blockEntity.getPos();
        ShapeBuilder.line(vec1.x - (mc.gameRenderer.getCamera().getPos().x - event.offsetX), vec1.y - (mc.gameRenderer.getCamera().getPos().y - event.offsetY), vec1.z - (mc.gameRenderer.getCamera().getPos().z - event.offsetZ), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5f, storageColor.get());

        count++;
    }

    @EventHandler
    private final Listener<RenderEvent> onRender = new Listener<>(event -> {
        count = 0;

        vec1 = new Vec3d(0, 0, 1)
                .rotateX(-(float) Math.toRadians(mc.gameRenderer.getCamera().getPitch()))
                .rotateY(-(float) Math.toRadians(mc.gameRenderer.getCamera().getYaw()))
                .add(mc.gameRenderer.getCamera().getPos());

        for (Entity entity : mc.world.getEntities()) {
            if ((!ModuleManager.INSTANCE.isActive(Freecam.class) && entity == mc.player) || !entities.get().contains(entity.getType())) continue;

            if (entity instanceof PlayerEntity) {
                Color color = playersColor.get();
                Friend friend = FriendManager.INSTANCE.get(((PlayerEntity) entity).getGameProfile().getName());
                if (friend != null) color = friend.color;

                if (friend == null || friend.showInTracers) render(entity, color, event);
            } else {
                switch (entity.getType().getSpawnGroup()) {
                    case CREATURE:       render(entity, animalsColor.get(), event); break;
                    case WATER_CREATURE: render(entity, waterAnimalsColor.get(), event); break;
                    case MONSTER:        render(entity, monstersColor.get(), event); break;
                    case AMBIENT:        render(entity, ambientColor.get(), event); break;
                    case MISC:           render(entity, miscColor.get(), event); break;
                }
            }
        }

        if (storage.get()) {
            for (BlockEntity blockEntity : mc.world.blockEntities) {
                if (blockEntity.isRemoved()) continue;

                if (blockEntity instanceof ChestBlockEntity || blockEntity instanceof BarrelBlockEntity || blockEntity instanceof ShulkerBoxBlockEntity) {
                    render(blockEntity, event);
                }
            }
        }
    });

    @Override
    public String getInfoString() {
        return Integer.toString(count);
    }
}
