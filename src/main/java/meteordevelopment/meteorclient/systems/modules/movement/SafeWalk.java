/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.movement;

import meteordevelopment.meteorclient.events.entity.player.ClipAtLedgeEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

import java.util.stream.StreamSupport;

public class SafeWalk extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> onlyOnGround = sgGeneral.add(new BoolSetting.Builder()
        .name("only-on-ground")
        .description("仅在地面时生效")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> sneak = sgGeneral.add(new BoolSetting.Builder()
        .name("sneak")
        .description("边缘自动潜行 (Eagle模式，防回弹推荐)")
        .defaultValue(true)
        .build()
    );
    
    private final Setting<Double> lookAhead = sgGeneral.add(new DoubleSetting.Builder()
        .name("look-ahead")
        .description("提前检测距离 (值越大越早潜行)")
        .defaultValue(0.5)
        .min(0.1)
        .max(2.0)
        .visible(sneak::get)
        .build()
    );

    private final Setting<Integer> minFallDistance = sgGeneral.add(new IntSetting.Builder()
        .name("min-fall-distance")
        .description("仅当掉落高度大于此值时生效")
        .defaultValue(0)
        .min(0)
        .max(10)
        .build()
    );

    private boolean sneakingByModule;

    public SafeWalk() {
        super(Categories.Movement, "safe-walk", "防止掉落 (Eagle 优化版)");
    }

    @Override
    public void onDeactivate() {
        if (sneakingByModule) {
            setSneak(false);
        }
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        if (mc.world == null || mc.player == null) return;
        if (onlyOnGround.get() && !mc.player.isOnGround()) {
            if (sneakingByModule) setSneak(false);
            return;
        }

        if (sneak.get()) {
            if (isOverEdge()) {
                if (!mc.options.sneakKey.isPressed()) {
                    setSneak(true);
                }
            } else {
                if (sneakingByModule) {
                    setSneak(false);
                }
            }
        }
    }

    @EventHandler
    private void onClipAtLedge(ClipAtLedgeEvent event) {
        if (mc.world == null || mc.player == null) return;
        if (onlyOnGround.get() && !mc.player.isOnGround()) return;

        if (minFallDistance.get() > 0 && isSafeFall()) return;

        if (sneak.get()) {
            if (mc.player.isSneaking()) return;
            event.setClip(true);
        } else {
            event.setClip(true);
        }
    }
    
    private void setSneak(boolean pressed) {
        mc.options.sneakKey.setPressed(pressed);
        sneakingByModule = pressed;
    }

    private boolean isOverEdge() {
        Vec3d velocity = mc.player.getVelocity();
        if (velocity.x == 0 && velocity.z == 0) return false;

        // 修复：手动构建 Vec3d 替代 getPos()，解决编译报错
        Vec3d playerPos = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        
        // 预测玩家下一帧的位置
        // lookAhead 决定了提前量
        // Vec3d nextPos = playerPos.add(velocity.x * lookAhead.get(), 0, velocity.z * lookAhead.get()); // 如果需要用到 nextPos 可以这样写
        
        // 构建检测框
        Box box = mc.player.getBoundingBox().offset(velocity.x * lookAhead.get(), 0, velocity.z * lookAhead.get());
        // 向下延伸检测是否有方块
        Box checkArea = box.offset(0, -1.0, 0);

        boolean hasGround = StreamSupport.stream(mc.world.getBlockCollisions(mc.player, checkArea).spliterator(), false)
            .map(VoxelShape::getBoundingBox)
            .anyMatch(blockBox -> blockBox.maxY > mc.player.getY() - 1.0);

        return !hasGround && !isSafeFall();
    }
    
    private boolean isSafeFall() {
        if (minFallDistance.get() == 0) return false;

        Box box = mc.player.getBoundingBox();
        Box checkArea = box.offset(0, -minFallDistance.get(), 0);

        return StreamSupport.stream(mc.world.getBlockCollisions(mc.player, checkArea).spliterator(), false)
            .map(VoxelShape::getBoundingBox)
            .anyMatch(blockBox -> blockBox.maxY > mc.player.getY() - minFallDistance.get());
    }
}