/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.entity.player.ClipAtLedgeEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.ClipContext;

public class SafeWalk extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Integer> fallDistance = sgGeneral.add(new IntSetting.Builder()
        .name("minimum-fall-distance")
        .description("The minimum number of blocks you are expected to fall before the module activates.")
        .defaultValue(1)
        .min(1)
        .build()
    );

    private final Setting<Boolean> sneak = sgGeneral.add(new BoolSetting.Builder()
        .name("sneak")
        .description("Sneak when approaching edge of block.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> safeSneak = sgGeneral.add(new BoolSetting.Builder()
        .name("safe-sneak")
        .description("Prevent you from falling if sneak doesn't trigger correctly.")
        .defaultValue(true)
        .visible(sneak::get)
        .build()
    );

    private final Setting<Boolean> sneakSprint = sgGeneral.add(new BoolSetting.Builder()
        .name("sneak-on-sprint")
        .description("Sneak even when sprinting at the block edge.")
        .defaultValue(true)
        .visible(sneak::get)
        .build()
    );

    private final Setting<Double> edgeDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("edge-distance")
        .description("Distance offset before reaching an edge.")
        .defaultValue(0.30)
        .sliderRange(0.00, 0.30)
        .decimalPlaces(2)
        .visible(sneak::get)
        .build()
    );

    private final Setting<Boolean> renderEdgeDistance = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("Render edge distance helper.")
        .defaultValue(false)
        .visible(sneak::get)
        .build()
    );

    private final Setting<Boolean> renderPlayerBox = sgRender.add(new BoolSetting.Builder()
        .name("render-player-box")
        .description("Render player box helper.")
        .defaultValue(false)
        .visible(() -> sneak.get() && renderEdgeDistance.get())
        .build()
    );

    public SafeWalk() {
        super(Categories.Movement, "safe-walk", "Prevents you from walking off blocks.");
    }

    @EventHandler
    private void onClipAtLedge(ClipAtLedgeEvent event) {
        if (fallDistance.get() > 1) {
            // meteordevelopment.meteorclient.utils.entity.DamageUtils.fallDamage
            int surface = mc.world.getWorldChunk(mc.player.getBlockPos()).getHeightmap(Heightmap.Types.MOTION_BLOCKING).get(mc.player.getBlockX() & 15, mc.player.getBlockZ() & 15);
            if (mc.player.getBlockY() >= surface) {
                if (mc.player.getBlockY() - surface < fallDistance.get()) return;
            } else {
                BlockHitResult raycastResult = mc.world.raycast(new RaycastContext(mc.player.getEntityPos(), new Vec3d(mc.player.getX(), mc.world.getBottomY(), mc.player.getZ()), ClipContext.Block.COLLIDER, ClipContext.Fluid.WATER, mc.player));
                if (raycastResult.getType() != BlockHitResult.Type.MISS) {
                    if ((int) (mc.player.getY() - raycastResult.getBlockPos().up().getY()) < fallDistance.get()) return;
                }
            }
        }

        if (sneak.get()) {
            boolean closeToEdge = false;
            boolean isSprinting = !sneakSprint.get() && mc.options.sprintKey.isPressed();

            AABB playerBox = mc.player.getBoundingBox();
            AABB adjustedBox = getAdjustedPlayerBox(playerBox);

            if (mc.world.isSpaceEmpty(mc.player, adjustedBox) && mc.player.isOnGround()) closeToEdge = true;

            if (!isSprinting) {
                if (closeToEdge) {
                    mc.player.input.playerInput = new PlayerInput(
                        mc.player.input.playerInput.forward(),
                        mc.player.input.playerInput.backward(),
                        mc.player.input.playerInput.left(),
                        mc.player.input.playerInput.right(),
                        mc.player.input.playerInput.jump(),
                        true,
                        mc.player.input.playerInput.sprint()
                    );
                } else if (safeSneak.get()) {
                    event.setClip(true);
                }
            }
        } else {
            if (!mc.player.isSneaking()) event.setClip(true);
        }
    }

    private AABB getAdjustedPlayerBox(AABB playerBox) {
        return playerBox.stretch(0, -mc.player.getStepHeight(), 0)
            .expand(-edgeDistance.get(), 0, -edgeDistance.get());
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (sneak.get() && renderEdgeDistance.get()) {
            AABB playerBox = mc.player.getBoundingBox();
            AABB adjustedBox = getAdjustedPlayerBox(playerBox);

            event.renderer.box(adjustedBox, Color.BLUE, Color.RED, ShapeMode.Lines, 0);

            if (renderPlayerBox.get()) {
                event.renderer.box(playerBox, Color.BLUE, Color.GREEN, ShapeMode.Lines, 0);
            }
        }
    }
}
