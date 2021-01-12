/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.combat;

//Created by squidoodly 03/08/2020

import com.google.common.collect.Streams;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.render.RenderEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.friends.FriendManager;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.rendering.Renderer;
import minegame159.meteorclient.rendering.ShapeMode;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.player.*;
import minegame159.meteorclient.utils.render.color.SettingColor;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.Optional;

public class AnchorAura extends Module {

    public enum Mode {
        Safe,
        Suicide
    }

    public enum PlaceMode {
        Above,
        AboveAndBelow,
        All
    }

    public enum RotationMode {
        Place,
        Break,
        Both,
        None
    }

    private final SettingGroup sgPlace = settings.createGroup("Place");
    private final SettingGroup sgBreak = settings.createGroup("Break");
    private final SettingGroup sgMisc = settings.createGroup("Misc");
    private final SettingGroup sgRender = settings.createGroup("Render");

    // Place

    private final Setting<Boolean> place = sgPlace.add(new BoolSetting.Builder()
            .name("place")
            .description("Allows Anchor Aura to place anchors.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Mode> placeMode = sgPlace.add(new EnumSetting.Builder<Mode>()
            .name("place-mode")
            .description("The way anchors are placed.")
            .defaultValue(Mode.Safe)
            .build()
    );

    private final Setting<PlaceMode> placePositions = sgPlace.add(new EnumSetting.Builder<PlaceMode>()
            .name("placement-positions")
            .description("The places anchors are placed.")
            .defaultValue(PlaceMode.AboveAndBelow)
            .build()
    );

    private final Setting<Double> placeRange = sgPlace.add(new DoubleSetting.Builder()
            .name("place-range")
            .description("The radius in which the anchors are placed in.")
            .defaultValue(3)
            .min(0)
            .sliderMax(5)
            .build()
    );

    private final Setting<Integer> placeDelay = sgPlace.add(new IntSetting.Builder()
            .name("place-delay")
            .description("The amount of delay in ticks for placement.")
            .defaultValue(2)
            .min(0)
            .max(20)
            .build()
    );

    // Break

    private final Setting<Mode> breakMode = sgBreak.add(new EnumSetting.Builder<Mode>()
            .name("break-mode")
            .description("The way anchors are broken.")
            .defaultValue(Mode.Safe)
            .build()
    );

    private final Setting<Integer> breakDelay = sgBreak.add(new IntSetting.Builder()
            .name("break-delay")
            .description("The amount of delay in ticks for breaking.")
            .defaultValue(10)
            .min(0)
            .max(10)
            .build()
    );

    private final Setting<Double> breakRange = sgBreak.add(new DoubleSetting.Builder()
            .name("break-range")
            .description("The radius in which the anchors are broken in.")
            .defaultValue(3)
            .min(0)
            .sliderMax(5)
            .build()
    );

    // Misc

    private final Setting<RotationMode> rotationMode = sgMisc.add(new EnumSetting.Builder<RotationMode>()
            .name("rotation-mode")
            .description("The mode to rotate you server-side.")
            .defaultValue(RotationMode.Both)
            .build()
    );

    private final Setting<Double> targetRange = sgMisc.add(new DoubleSetting.Builder()
            .name("target-range")
            .description("The radius in which players get targeted.")
            .defaultValue(4)
            .min(0)
            .sliderMax(5)
            .build()
    );

    private final Setting<Double> maxDamage = sgMisc.add(new DoubleSetting.Builder()
            .name("max-self-damage")
            .description("The maximum self-damage allowed.")
            .defaultValue(8)
            .build()
    );

    private final Setting<Double> minHealth = sgMisc.add(new DoubleSetting.Builder()
            .name("min-health")
            .description("The minimum health you have to be for Anchor Aura to work.")
            .defaultValue(15)
            .build()
    );

    // Render

    private final Setting<Boolean> renderPlace = sgRender.add(new BoolSetting.Builder()
            .name("render-place")
            .description("Renders the block where it is placing an anchor.")
            .defaultValue(true)
            .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("place-side-color")
            .description("The side color for positions to be placed.")
            .defaultValue(new SettingColor(255, 0, 0, 75))
            .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("place-line-color")
            .description("The line color for positions to be placed.")
            .defaultValue(new SettingColor(255, 0, 0, 255))
            .build()
    );

    private final Setting<Boolean> renderBreak = sgRender.add(new BoolSetting.Builder()
            .name("render-break")
            .description("Renders the block where it is breaking an anchor.")
            .defaultValue(true)
            .build()
    );

    private final Setting<SettingColor> breakSideColor = sgRender.add(new ColorSetting.Builder()
            .name("break-side-color")
            .description("The side color for anchors to be broken.")
            .defaultValue(new SettingColor(255, 0, 0, 75))
            .build()
    );

    private final Setting<SettingColor> breakLineColor = sgRender.add(new ColorSetting.Builder()
            .name("break-line-color")
            .description("The line color for anchors to be broken.")
            .defaultValue(new SettingColor(255, 0, 0, 255))
            .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .build()
    );

    public AnchorAura() {super(Category.Combat, "anchor-aura", "Automatically places and breaks Respawn Anchors to harm entities.");}

    private int placeDelayLeft;
    private int breakDelayLeft;
    private PlayerEntity target;

    @Override
    public void onActivate() {
        placeDelayLeft = 0;
        breakDelayLeft = 0;
    }

    @EventHandler
    private final Listener<TickEvent.Post> onTick = new Listener<>(event -> {
        if (mc.world.getDimension().isRespawnAnchorWorking()) {
            Chat.error(this, "You are in the Nether... (highlight)disabling(default)!");
            this.toggle();
            return;
        }

        if (getTotalHealth(mc.player) <= minHealth.get() && placeMode.get() != Mode.Suicide && breakMode.get() != Mode.Suicide) return;

        if (target == null || mc.player.distanceTo(target) > targetRange.get() || !target.isAlive()) target = findTarget();
        if (target == null) return;

        int anchorSlot = InvUtils.findItemInHotbar(Items.RESPAWN_ANCHOR, itemStack -> true);
        int glowSlot = InvUtils.findItemInHotbar(Items.GLOWSTONE, itemStack -> true);



        if (breakDelayLeft >= breakDelay.get()) {

            BlockPos breakPos = findAnchor(target);

            if (breakPos != null) {
                if (rotationMode.get() == RotationMode.Both || rotationMode.get() == RotationMode.Break) RotationUtils.packetRotate(breakPos);

                mc.player.setSneaking(false);
                mc.options.keySneak.setPressed(false);
                breakAnchor(breakPos, glowSlot, anchorSlot);

                breakDelayLeft = 0;
            }
        }

        if (placeDelayLeft >= placeDelay.get() && place.get()) {

            BlockPos placePos = findPlacePos(target);

            if (placePos != null) {
                if (rotationMode.get() == RotationMode.Both || rotationMode.get() == RotationMode.Place) RotationUtils.packetRotate(placePos);

                mc.player.setSneaking(false);
                mc.options.keySneak.setPressed(false);
                PlayerUtils.placeBlock(placePos, anchorSlot, Hand.MAIN_HAND);

                placeDelayLeft = 0;
            }
        }

        placeDelayLeft++;
        breakDelayLeft++;
    });

    @EventHandler
    private final Listener<RenderEvent> onRender = new Listener<>(event -> {
        if (target != null) {
            if (renderPlace.get() && findPlacePos(target) != null) Renderer.boxWithLines(Renderer.NORMAL, Renderer.LINES, findPlacePos(target).getX(), findPlacePos(target).getY(), findPlacePos(target).getZ(), 1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (renderBreak.get() && findAnchor(target) != null) Renderer.boxWithLines(Renderer.NORMAL, Renderer.LINES, findAnchor(target).getX(), findAnchor(target).getY(), findAnchor(target).getZ(), 1, breakSideColor.get(), breakLineColor.get(), shapeMode.get(), 0);
        }
    });

    private BlockPos findPlacePos(PlayerEntity target) {
        BlockPos targetPlacePos = target.getBlockPos();
        BlockPos finalPlacePos = null;

        switch (placePositions.get()){
            case All:
                if (isValidPlace(targetPlacePos.down())) finalPlacePos = targetPlacePos.down();
                else if (isValidPlace(targetPlacePos.up(2))) finalPlacePos = targetPlacePos.up(2);
                else if (isValidPlace(targetPlacePos.add(1, 0, 0))) finalPlacePos = targetPlacePos.add(1, 0, 0);
                else if (isValidPlace(targetPlacePos.add(-1, 0, 0))) finalPlacePos = targetPlacePos.add(-1, 0, 0);
                else if (isValidPlace(targetPlacePos.add(0, 0, 1))) finalPlacePos = targetPlacePos.add(0, 0, 1);
                else if (isValidPlace(targetPlacePos.add(0, 0, -1))) finalPlacePos = targetPlacePos.add(0, 0, -1);
                else if (isValidPlace(targetPlacePos.add(1, 1, 0))) finalPlacePos = targetPlacePos.add(1, 1, 0);
                else if (isValidPlace(targetPlacePos.add(-1, -1, 0))) finalPlacePos = targetPlacePos.add(-1, -1, 0);
                else if (isValidPlace(targetPlacePos.add(0, 1, 1))) finalPlacePos = targetPlacePos.add(0, 1, 1);
                else if (isValidPlace(targetPlacePos.add(0, 0, -1))) finalPlacePos = targetPlacePos.add(0, 0, -1);
                break;
            case Above:
                if (isValidPlace(targetPlacePos.up(2))) finalPlacePos = targetPlacePos.up(2);
                break;
            case AboveAndBelow:
                if (isValidPlace(targetPlacePos.down())) finalPlacePos = targetPlacePos.down();
                else if (isValidPlace(targetPlacePos.up(2))) finalPlacePos = targetPlacePos.up(2);
                break;
        }

        return finalPlacePos;
    }

    private BlockPos findAnchor(PlayerEntity target) {
        BlockPos targetPos = target.getBlockPos();
        BlockPos finalPlacePos = null;

        if (isValidBreak(targetPos.down())) finalPlacePos = targetPos.down();
        else if (isValidBreak(targetPos.up(2))) finalPlacePos = targetPos.up(2);
        else if (isValidBreak(targetPos.add(1, 0, 0))) finalPlacePos = targetPos.add(1, 0, 0);
        else if (isValidBreak(targetPos.add(-1, 0, 0))) finalPlacePos = targetPos.add(-1, 0, 0);
        else if (isValidBreak(targetPos.add(0, 0, 1))) finalPlacePos = targetPos.add(0, 0, 1);
        else if (isValidBreak(targetPos.add(0, 0, -1))) finalPlacePos = targetPos.add(0, 0, -1);
        else if (isValidBreak(targetPos.add(1, 1, 0))) finalPlacePos = targetPos.add(1, 1, 0);
        else if (isValidBreak(targetPos.add(-1, -1, 0))) finalPlacePos = targetPos.add(-1, -1, 0);
        else if (isValidBreak(targetPos.add(0, 1, 1))) finalPlacePos = targetPos.add(0, 1, 1);
        else if (isValidBreak(targetPos.add(0, 0, -1))) finalPlacePos = targetPos.add(0, 0, -1);

        return finalPlacePos;
    }

    private boolean getDamagePlace(BlockPos pos){
        assert mc.player != null;
        return placeMode.get() == Mode.Suicide || DamageCalcUtils.bedDamage(mc.player, new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5)) <= maxDamage.get();
    }

    private boolean getDamageBreak(BlockPos pos){
        assert mc.player != null;
        return breakMode.get() == Mode.Suicide || DamageCalcUtils.anchorDamage(mc.player, new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5)) <= maxDamage.get();
    }

    private boolean isValidPlace(BlockPos pos) {
        return mc.world.getBlockState(pos).isAir() && Math.sqrt(mc.player.getBlockPos().getSquaredDistance(pos)) <= placeRange.get() && getDamagePlace(pos);
    }

    private boolean isValidBreak(BlockPos pos) {
        return mc.world.getBlockState(pos).getBlock() == Blocks.RESPAWN_ANCHOR && Math.sqrt(mc.player.getBlockPos().getSquaredDistance(pos)) <= breakRange.get() && getDamageBreak(pos);
    }

    private PlayerEntity findTarget(){
        Optional<PlayerEntity> livingEntity = Streams.stream(mc.world.getEntities())
                .filter(entity -> entity instanceof PlayerEntity)
                .filter(Entity::isAlive)
                .filter(entity -> entity != mc.player)
                .filter(entity -> FriendManager.INSTANCE.attack((PlayerEntity) entity))
                .filter(entity -> entity.distanceTo(mc.player) <= targetRange.get() * 2)
                .min(Comparator.comparingDouble(o -> o.distanceTo(mc.player)))
                .map(entity -> (PlayerEntity) entity);
        return livingEntity.orElse(null);
    }

    private void breakAnchor(BlockPos pos, int glowSlot, int nonGlowSlot) {
        if (pos == null || mc.world.getBlockState(pos).getBlock() != Blocks.RESPAWN_ANCHOR) return;

        if (glowSlot != -1 && nonGlowSlot != -1) {
            int preSlot = mc.player.inventory.selectedSlot;
            mc.player.inventory.selectedSlot = glowSlot;
            mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), Direction.UP, pos, true));
            mc.player.inventory.selectedSlot = nonGlowSlot;
            mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), Direction.UP, pos, true));
            mc.player.inventory.selectedSlot = preSlot;
        }
    }

    private float getTotalHealth(PlayerEntity target) {
        return target.getHealth() + target.getAbsorptionAmount();
    }
}
