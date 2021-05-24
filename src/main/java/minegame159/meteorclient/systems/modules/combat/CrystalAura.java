/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.combat;

import com.google.common.collect.Streams;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import minegame159.meteorclient.events.render.Render2DEvent;
import minegame159.meteorclient.events.render.RenderEvent;
import minegame159.meteorclient.events.world.PlaySoundEvent;
import minegame159.meteorclient.events.world.TickEvent;
import minegame159.meteorclient.mixininterface.IClientPlayerInteractionManager;
import minegame159.meteorclient.mixininterface.IVec3d;
import minegame159.meteorclient.rendering.Renderer;
import minegame159.meteorclient.rendering.ShapeMode;
import minegame159.meteorclient.rendering.text.TextRenderer;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;
import minegame159.meteorclient.utils.entity.EntityUtils;
import minegame159.meteorclient.utils.entity.SortPriority;
import minegame159.meteorclient.utils.entity.TargetUtils;
import minegame159.meteorclient.utils.misc.Keybind;
import minegame159.meteorclient.utils.misc.Pool;
import minegame159.meteorclient.utils.misc.Vec3;
import minegame159.meteorclient.utils.player.DamageCalcUtils;
import minegame159.meteorclient.utils.player.InvUtils;
import minegame159.meteorclient.utils.player.PlayerUtils;
import minegame159.meteorclient.utils.player.Rotations;
import minegame159.meteorclient.utils.render.NametagUtils;
import minegame159.meteorclient.utils.render.color.SettingColor;
import minegame159.meteorclient.utils.world.BlockUtils;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;

import java.util.*;

public class CrystalAura extends Module {
    public enum RotationMode {
        Placing,
        Breaking,
        Both,
        None
    }

    public enum CancelCrystalMode {
        Sound,
        Hit,
	    None
    }

    private final SettingGroup sgTarget = settings.createGroup("Target");
    private final SettingGroup sgPlace = settings.createGroup("Place");
    private final SettingGroup sgBreak = settings.createGroup("Break");
    private final SettingGroup sgPause = settings.createGroup("Pause");
    private final SettingGroup sgMisc = settings.createGroup("Misc");
    private final SettingGroup sgRender = settings.createGroup("Render");

    // Targeting

    private final Setting<Double> targetRange = sgTarget.add(new DoubleSetting.Builder()
            .name("target-range")
            .description("The maximum range the entity can be to be targeted.")
            .defaultValue(7)
            .min(0)
            .sliderMax(10)
            .build()
    );

    private final Setting<SortPriority> targetPriority = sgTarget.add(new EnumSetting.Builder<SortPriority>()
            .name("target-priority")
            .description("How to select the player to target.")
            .defaultValue(SortPriority.LowestHealth)
            .build()
    );

    private final Setting<Double> minDamage = sgTarget.add(new DoubleSetting.Builder()
            .name("min-damage")
            .description("The minimum damage to deal.")
            .defaultValue(8)
            .build()
    );

    // Place

    private final Setting<Integer> placeDelay = sgPlace.add(new IntSetting.Builder()
            .name("place-delay")
            .description("The amount of delay in ticks before placing.")
            .defaultValue(2)
            .min(0)
            .sliderMax(10)
            .build()
    );

    private final Setting<Integer> placeRange = sgPlace.add(new IntSetting.Builder()
            .name("place-range")
            .description("The radius in which crystals can be placed in.")
            .defaultValue(5)
            .min(0)
            .sliderMax(7)
            .build()
    );

    private final Setting<Integer> placeWallsRange = sgPlace.add(new IntSetting.Builder()
            .name("place-walls-range")
            .description("The radius in which crystals can be placed through walls.")
            .defaultValue(3)
            .min(0)
            .sliderMax(7)
            .build()
    );

    private final Setting<Boolean> oldPlace = sgPlace.add(new BoolSetting.Builder()
            .name("1.12")
            .description("Won't place in one block holes for 1.12 crystal placements.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Keybind> surroundBreak = sgPlace.add(new KeybindSetting.Builder()
            .name("surround-break")
            .description("Forces you to place crystals next to the targets surround when the bind is held.")
            .defaultValue(Keybind.fromKey(-1))
            .build()
    );

    private final Setting<Boolean> facePlace = sgPlace.add(new BoolSetting.Builder()
            .name("face-place")
            .description("Will face-place when target is below a certain health or armor durability threshold.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> facePlaceHealth = sgPlace.add(new DoubleSetting.Builder()
            .name("face-place-health")
            .description("The health required to face place.")
            .defaultValue(8)
            .min(1).max(36)
            .sliderMin(1).sliderMax(36)
            .visible(facePlace::get)
            .build()
    );

    private final Setting<Integer> facePlaceDurability = sgPlace.add(new IntSetting.Builder()
            .name("face-place-durability")
            .description("The durability threshold to face place.")
            .defaultValue(2)
            .min(1).max(100)
            .sliderMin(0).sliderMax(100)
            .visible(facePlace::get)
            .build()
    );

    private final Setting<Keybind> forceFacePlace = sgPlace.add(new KeybindSetting.Builder()
            .name("force-face-place")
            .description("Forces you to face place when the key is held.")
            .defaultValue(Keybind.fromKey(-1))
            .visible(facePlace::get)
            .build()
    );

    // Break

    private final Setting<Integer> breakDelay = sgBreak.add(new IntSetting.Builder()
            .name("break-delay")
            .description("The amount of delay in ticks before breaking.")
            .defaultValue(1)
            .min(0)
            .sliderMax(10)
            .build()
    );

    private final Setting<Integer> breakRange = sgBreak.add(new IntSetting.Builder()
            .name("break-range")
            .description("The maximum range that crystals can be to be broken.")
            .defaultValue(5)
            .min(0)
            .sliderMax(7)
            .build()
    );

    private final Setting<Integer> breakWallsRange = sgBreak.add(new IntSetting.Builder()
            .name("break-walls-range")
            .description("The radius in which crystals can be broken through walls.")
            .defaultValue(3)
            .min(0)
            .sliderMax(7)
            .build()
    );

    private final Setting<CancelCrystalMode> cancelCrystalMode = sgBreak.add(new EnumSetting.Builder<CancelCrystalMode>()
            .name("cancel-mode")
            .description("Mode to use for the crystals to be removed from the world.")
            .defaultValue(CancelCrystalMode.Sound)
            .build()
    );

    // Pause

    private final Setting<Double> pauseAtHealth = sgPause.add(new DoubleSetting.Builder()
            .name("pause-health")
            .description("Pauses when you go below a certain health.")
            .defaultValue(5)
            .min(0)
            .build()
    );

    private final Setting<Boolean> pauseOnEat = sgPause.add(new BoolSetting.Builder()
            .name("pause-on-eat")
            .description("Pauses Crystal Aura while eating.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> pauseOnDrink = sgPause.add(new BoolSetting.Builder()
            .name("pause-on-drink")
            .description("Pauses Crystal Aura while drinking a potion.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> pauseOnMine = sgPause.add(new BoolSetting.Builder()
            .name("pause-on-mine")
            .description("Pauses Crystal Aura while mining blocks.")
            .defaultValue(false)
            .build()
    );

    // Misc

    private final Setting<Boolean> autoSwitch = sgMisc.add(new BoolSetting.Builder()
            .name("auto-switch")
            .description("Switches to crystals automatically.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> antiWeakness = sgMisc.add(new BoolSetting.Builder()
            .name("anti-weakness")
            .description("Switches to tools to break crystals instead of your fist.")
            .defaultValue(true)
            .build()
    );

    private final Setting<RotationMode> rotationMode = sgMisc.add(new EnumSetting.Builder<RotationMode>()
            .name("rotation-mode")
            .description("The method of rotating when using Crystal Aura.")
            .defaultValue(RotationMode.Placing)
            .build()
    );

    private final Setting<Integer> verticalRange = sgMisc.add(new IntSetting.Builder()
            .name("vertical-range")
            .description("The maximum vertical range for placing/breaking end crystals.")
            .min(0)
            .defaultValue(3)
            .max(7)
            .build()
    );

    private final Setting<Boolean> antiSuicide = sgMisc.add(new BoolSetting.Builder()
            .name("anti-suicide")
            .description("Attempts to prevent you from killing yourself.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> maxSelfDamage = sgMisc.add(new DoubleSetting.Builder()
            .name("max-self-damage")
            .description("The maximum self-damage allowed.")
            .defaultValue(8)
            .build()
    );

    // Render

    private final Setting<Boolean> swing = sgRender.add(new BoolSetting.Builder()
            .name("swing")
            .description("Renders your hand swinging client-side.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
            .name("render")
            .description("Renders the block under where it is placing a crystal.")
            .defaultValue(true)
            .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Sides)
            .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side-color")
            .description("The side color.")
            .defaultValue(new SettingColor(255, 255, 255, 75))
            .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The line color.")
            .defaultValue(new SettingColor(255, 255, 255, 175))
            .build()
    );

    private final Setting<Boolean> renderDamage = sgRender.add(new BoolSetting.Builder()
            .name("damage-text")
            .description("Renders text displaying the amount of damage the crystal will do.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> damageScale = sgRender.add(new DoubleSetting.Builder()
            .name("damage-text-scale")
            .description("The scale of the damage text.")
            .defaultValue(1.4)
            .min(0)
            .sliderMax(5)
            .visible(renderDamage::get)
            .build()
    );

    private final Setting<SettingColor> damageColor = sgRender.add(new ColorSetting.Builder()
            .name("damage-color")
            .description("The color of the damage text.")
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .visible(renderDamage::get)
            .build()
    );

    private final Setting<Integer> renderTimer = sgRender.add(new IntSetting.Builder()
            .name("timer")
            .description("The amount of time between changing the block render.")
            .defaultValue(0)
            .min(0)
            .sliderMax(10)
            .build()
    );

    public CrystalAura() {
        super(Categories.Combat, "crystal-aura", "Automatically places and breaks crystals to damage other players.");
    }

    private int placeDelayLeft = placeDelay.get();
    private int breakDelayLeft = breakDelay.get();

    private PlayerEntity playerTarget;
    private BlockPos blockTarget;

    private final Map<BlockPos, Double> crystalMap = new HashMap<>();
    private final List<Integer> removalQueue = new ArrayList<>();

    private static final Vec3d crystalPos = new Vec3d(0, 0, 0);
    private static final Vec3d hitPos = new Vec3d(0, 0, 0);

    private final Pool<RenderBlock> renderBlockPool = new Pool<>(RenderBlock::new);
    private final List<RenderBlock> renderBlocks = new ArrayList<>();
    private static final Vec3 renderPos = new Vec3();

    private boolean broken = false;

    @Override
    public void onActivate() {
        placeDelayLeft = 0;
        breakDelayLeft = 0;
    }

    @Override
    public void onDeactivate() {
        for (RenderBlock renderBlock : renderBlocks) {
            renderBlockPool.free(renderBlock);
        }
        renderBlocks.clear();
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onTick(TickEvent.Pre event) {
        if (cancelCrystalMode.get() == CancelCrystalMode.Hit) {
            removalQueue.forEach(id -> mc.world.removeEntity(id));
            removalQueue.clear();
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onPlaySound(PlaySoundEvent event) {
        if (event.sound.getCategory().getName().equals(SoundCategory.BLOCKS.getName()) && event.sound.getId().getPath().equals("entity.generic.explode") && cancelCrystalMode.get() == CancelCrystalMode.Sound) {
            removalQueue.forEach(id -> mc.world.removeEntity(id));
            removalQueue.clear();
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onTick(TickEvent.Post event) {
        // Clear render
        for (Iterator<RenderBlock> it = renderBlocks.iterator(); it.hasNext();) {
            RenderBlock renderBlock = it.next();

            if (renderBlock.shouldRemove()) {
                it.remove();
                renderBlockPool.free(renderBlock);
            }
        }

        // Find target
        if (TargetUtils.isBadTarget(playerTarget, targetRange.get())) playerTarget = TargetUtils.getPlayerTarget(targetRange.get(), targetPriority.get());
        if (TargetUtils.isBadTarget(playerTarget, targetRange.get())) return;

        // Pause
        if (PlayerUtils.shouldPause(pauseOnMine.get(), pauseOnEat.get(), pauseOnDrink.get())) return;
        if (PlayerUtils.getTotalHealth() <= pauseAtHealth.get()) return;

        // Break
        breakDelayLeft--;
        if (breakDelayLeft <= 0) {
            breakBest();
            if (broken) return;
	  }

        // Check for crystals
        if (!autoSwitch.get() && InvUtils.getHand(itemStack -> itemStack.getItem() == Items.END_CRYSTAL) == null) return;

        // Get place positions
        getAllValid();
        if (crystalMap.isEmpty()) return;

        // Select best pos
        findBestPos();
        if (blockTarget == null) return;

        // Run place
        placeDelayLeft--;
        if (placeDelayLeft <= 0) placeBest();
    }

    // Breaking
    private void breakBest() {
        broken = false;
        Streams.stream(mc.world.getEntities())
                .filter(this::validBreak)
                .max(Comparator.comparingDouble(o -> DamageCalcUtils.crystalDamage(playerTarget, getCrystalPos(o.getBlockPos()))))
                .ifPresent(entity -> hitCrystal((EndCrystalEntity) entity));
    }

    private void hitCrystal(EndCrystalEntity entity) {
        int preSlot = mc.player.inventory.selectedSlot, slot;

        if (antiWeakness.get() && mc.player.getActiveStatusEffects().containsKey(StatusEffects.WEAKNESS)) {
            slot = InvUtils.findItemInHotbar(itemStack -> itemStack.getItem() instanceof SwordItem || itemStack.getItem() instanceof AxeItem);
            if (slot == -1) return;
        } else {
            slot = mc.player.inventory.selectedSlot;
        }

        mc.player.inventory.selectedSlot = slot;

        if (rotationMode.get() == RotationMode.Breaking || rotationMode.get() == RotationMode.Both) {
            float[] rotation = PlayerUtils.calculateAngle(entity.getPos());
            Rotations.rotate(rotation[0], rotation[1], 30, () -> attackCrystal(entity));
        } else {
            attackCrystal(entity);
        }

        mc.player.inventory.selectedSlot = preSlot;

        removalQueue.add(entity.getEntityId());
        broken = true;
        breakDelayLeft = breakDelay.get();
    }

    private void attackCrystal(EndCrystalEntity entity) {
        mc.interactionManager.attackEntity(mc.player, entity);
        if (swing.get()) mc.player.swingHand(Hand.MAIN_HAND);
        else mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
    }

    private boolean validBreak(Entity entity) {
        if (!(entity instanceof EndCrystalEntity)) return false;
        if (!entity.isAlive()) return false;

        if (PlayerUtils.canSeeEntity(entity) ) {
            if (PlayerUtils.distanceTo(entity) >= breakRange.get()) return false;
        } else {
            if (PlayerUtils.distanceTo(entity) >= breakWallsRange.get()) return false;
        }

        if (DamageCalcUtils.crystalDamage(mc.player, getCrystalPos(entity.getBlockPos())) >= maxSelfDamage.get()) return false;
        if (antiSuicide.get() && PlayerUtils.getTotalHealth() - DamageCalcUtils.crystalDamage(mc.player, getCrystalPos(entity.getBlockPos())) <= 0) return false;
        return shouldFacePlace() || surroundBreak.get().isPressed() || !(DamageCalcUtils.crystalDamage(playerTarget, getCrystalPos(entity.getBlockPos())) < minDamage.get());
    }

    // Placing
    private void placeBest() {
        Hand hand = InvUtils.getHand(Items.END_CRYSTAL);
        int slot;

        if (hand == null) {
            slot = InvUtils.findItemInHotbar(Items.END_CRYSTAL);
            if (!autoSwitch.get() || slot == -1) return;

            mc.player.inventory.selectedSlot = slot;
            ((IClientPlayerInteractionManager) mc.interactionManager).syncSelectedSlot2();

            hand = InvUtils.getHand(Items.END_CRYSTAL);
            if (hand == null) return;
        }

        Direction direction = rayTrace(blockTarget, true);
        Direction opposite = direction.getOpposite();
        BlockPos neighbor = blockTarget.offset(direction);

        ((IVec3d) hitPos).set(neighbor.getX() + 0.5 + opposite.getVector().getX() * 0.5, neighbor.getY() + 0.5 + opposite.getVector().getY() * 0.5, neighbor.getZ() + 0.5 + opposite.getVector().getZ() * 0.5);

        Hand finalHand = hand;
        if (rotationMode.get() == RotationMode.Placing || rotationMode.get() == RotationMode.Both) {
            float[] rotation = PlayerUtils.calculateAngle(hitPos);
            Rotations.rotate(rotation[0], rotation[1], 30, () -> place(direction, finalHand));
        } else {
            place(direction, finalHand);
        }

        if (render.get()) {
            RenderBlock renderBlock = renderBlockPool.get();
            renderBlock.reset(blockTarget);
            renderBlock.damage = DamageCalcUtils.crystalDamage(playerTarget, getCrystalPos(blockTarget.up()));
            renderBlocks.add(renderBlock);
        }
    }

    private void place(Direction direction, Hand hand) {
        mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(hand, new BlockHitResult(hitPos, direction, blockTarget, false)));
        if (swing.get()) mc.player.swingHand(hand);
        else mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(hand));
    }

    private boolean shouldFacePlace() {
        if (!facePlace.get()) return false;

        if (EntityUtils.getTotalHealth(playerTarget) <= facePlaceHealth.get()) return true;

        for (ItemStack itemStack : playerTarget.getArmorItems()) {
            if (itemStack.isEmpty() || !itemStack.isDamageable()) continue;
            if ((((itemStack.getMaxDamage() - itemStack.getDamage()) / itemStack.getMaxDamage()) * 100) <= facePlaceDurability.get()) return true;
        }

        return forceFacePlace.get().isPressed();
    }

    private void findBestPos() {
        double bestDamage = 0;
        blockTarget = null;

        for (Map.Entry<BlockPos, Double> blockPosDoubleEntry : crystalMap.entrySet()) {
            if (blockPosDoubleEntry.getValue() > bestDamage) {
                bestDamage = blockPosDoubleEntry.getValue();
                if (blockPosDoubleEntry.getValue() >= minDamage.get()) blockTarget = blockPosDoubleEntry.getKey();
            }
        }
    }

    private void getAllValid() {
        crystalMap.clear();

        for (BlockPos blockPos : BlockUtils.getSphere(mc.player.getBlockPos(), placeRange.get(), verticalRange.get())) {
            if (!validPlace(blockPos)) continue;
            crystalMap.put(blockPos, DamageCalcUtils.crystalDamage(playerTarget, getCrystalPos(blockPos.up())));
        }
    }

    private boolean validPlace(BlockPos pos) {
        if (crystalMap.keySet().contains(pos)) return false;

        // Base check
        Block block = mc.world.getBlockState(pos).getBlock();
        if (block != Blocks.OBSIDIAN && block != Blocks.BEDROCK) return false;

        // Raytracing
        boolean canSee = rayTrace(pos, false) != null;
        if (canSee) {
            if (PlayerUtils.distanceTo(pos) >= placeRange.get()) return false;
        } else {
            if (PlayerUtils.distanceTo(pos) >= placeWallsRange.get()) return false;
        }

        // Space check
        BlockPos crystalPos = pos.up();
        if (notEmpty(crystalPos)) return false;
        if (oldPlace.get() && notEmpty(crystalPos.up())) return false;

        // Damage check
        if (DamageCalcUtils.crystalDamage(mc.player, getCrystalPos(crystalPos)) >= maxSelfDamage.get()) return false;
        if (antiSuicide.get() && PlayerUtils.getTotalHealth() - DamageCalcUtils.crystalDamage(mc.player, getCrystalPos(crystalPos)) <= 0) return false;

        if (shouldFacePlace()) {
            BlockPos targetHead = playerTarget.getBlockPos();

            // Checking faceplace blocks
            for (Direction direction : Direction.values()) {
                if (direction == Direction.DOWN || direction == Direction.UP) continue;

                // If one of the positions matches the current pos, ignore minDamage
                if (pos.equals(targetHead.offset(direction))) return true;
            }
        }
        else if (surroundBreak.get().isPressed()) {
            BlockPos targetSurround = EntityUtils.getCityBlock(playerTarget);

            if (targetSurround != null) {
                // Checking arround targets city block
                for (Direction direction : Direction.values()) {
                    if (direction == Direction.DOWN || direction == Direction.UP) continue;

                    // If one of the positions matches the current pos, ignore minDamage
                    if (pos.equals(targetSurround.down().offset(direction))) return true;
                }
            }
        }

        return !(DamageCalcUtils.crystalDamage(playerTarget, getCrystalPos(crystalPos)) < minDamage.get());
    }

    // Getting damage from the center of the blast
    private Vec3d getCrystalPos(BlockPos blockPos) {
        ((IVec3d) crystalPos).set(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
        return crystalPos;
    }

    private boolean notEmpty(BlockPos pos) {
        return !mc.world.getBlockState(pos).isAir() || !mc.world.getOtherEntities(null, new Box(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0D, pos.getY() + 2.0D, pos.getZ() + 1.0D)).isEmpty();
    }

    private Direction rayTrace(BlockPos pos, boolean forceReturn) {
        Vec3d eyesPos = new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());

        for (Direction direction : Direction.values()) {
            RaycastContext raycastContext = new RaycastContext(eyesPos, new Vec3d(pos.getX() + 0.5 + direction.getVector().getX() * 0.5,
                    pos.getY() + 0.5 + direction.getVector().getY() * 0.5,
                    pos.getZ() + 0.5 + direction.getVector().getZ() * 0.5), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
            BlockHitResult result = mc.world.raycast(raycastContext);
            if (result != null && result.getType() == HitResult.Type.BLOCK && result.getBlockPos().equals(pos)) {
                return direction;
            }
        }
        if (forceReturn) { // When we're placing, we have to return a direction so we have a side to place against
            if ((double) pos.getY() > eyesPos.y) {
                return Direction.DOWN; // The player can never see the top of a block if they are under it
            }
            return Direction.UP;
        }
        return null;
    }

    @EventHandler
    private void onRender(RenderEvent event) {
        if (!render.get()) return;

        for (RenderBlock renderBlock : renderBlocks) {
            renderBlock.render3D();
        }
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (!render.get()) return;

        for (RenderBlock renderBlock : renderBlocks) {
            renderBlock.render2D();
        }
    }

    private class RenderBlock {
        private int x, y, z;
        private int timer;
        private double damage;

        public void reset(BlockPos pos) {
            x = MathHelper.floor(pos.getX());
            y = MathHelper.floor(pos.getY());
            z = MathHelper.floor(pos.getZ());
            timer = renderTimer.get();
        }

        public boolean shouldRemove() {
            if (timer <= 0) return true;
            timer--;
            return false;
        }

        public void render3D() {
            Renderer.boxWithLines(Renderer.NORMAL, Renderer.LINES, x, y, z, 1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }

        public void render2D() {
            if (renderDamage.get()) {
                renderPos.set(x + 0.5, y + 0.5, z + 0.5);

                if (NametagUtils.to2D(renderPos, damageScale.get())) {
                    NametagUtils.begin(renderPos);
                    TextRenderer.get().begin(1, false, true);

                    String damageText = String.valueOf(Math.round(damage * 100.0) / 100.0);

                    double w = TextRenderer.get().getWidth(damageText) / 2;

                    TextRenderer.get().render(damageText, -w, 0, damageColor.get());

                    TextRenderer.get().end();
                    NametagUtils.end();
                }
            }
        }
    }

    @Override
    public String getInfoString() {
        if (playerTarget != null) return playerTarget.getEntityName();
        return null;
    }
}
